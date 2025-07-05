package gossipGlomers

import zio.*
import zio.json.*
import com.bilalfazlani.zioMaelstrom.*
import zio.ZIO.{logDebug, logInfo, logError}

object Main extends MaelstromNode {

  enum NodeRole:
    case Leader(followers: Set[NodeId])
    case Follower(of: NodeId)

  case class State(
      role: NodeRole,
      numbers: Set[Int] = Set.empty
  ) {
    def addNumbers(ns: Set[Int]): State = copy(numbers = numbers ++ ns)
    def addNumber(n: Int): State = copy(numbers = numbers + n)
  }

  private def gossipToLeader(leader: NodeId, number: Int) =
    val retryCount = 10
    leader
      .ask[GossipOk](Gossip(number), 2.seconds)
      .retry(Schedule.recurs(retryCount))
      .tapError(e => logError(s"gossip ask failed after $retryCount retries: ${e}"))
      .ignore

  private def updateToFollowers(followers: Set[NodeId], numbers: Set[Int]) =
    val retryCount = 10
    ZIO.foreachParDiscard(followers) {
      follower =>
        follower
          .ask[UpdateOk](Update(numbers), 2.seconds)
          .retry(Schedule.recurs(retryCount))
          .tapError(e => logError(s"update ask failed after $retryCount retries: ${e}"))
          .ignore
    }

  private def followerHandler(leader: NodeId) = receive[FollowerMessage] {
    case Broadcast(number) =>
      ZIO.serviceWithZIO[Ref[State]](_.update(_.addNumber(number))) *>
        (reply(BroadcastOk()) zipPar gossipToLeader(leader, number))

    case Update(numbers) =>
      ZIO.serviceWithZIO[Ref[State]](_.update(_.addNumbers(numbers))) *> reply(UpdateOk())

    case Read() =>
      ZIO.serviceWithZIO[Ref[State]](_.get.flatMap(state => reply(ReadOk(state.numbers))))

    case Topology(_) => reply(TopologyOk())
  }

  private def leaderHandler(followers: Set[NodeId]) = receive[LeaderMessage] {
    case Broadcast(number) =>
      ZIO.serviceWithZIO[Ref[State]](_.update(_.addNumber(number))) *>
        reply(BroadcastOk()) zipPar updateToFollowers(followers, Set(number))

    case Gossip(numbers) =>
      for
        _ <- ZIO.serviceWithZIO[Ref[State]](_.update(_.addNumbers(numbers)))
        src <- MaelstromRuntime.src
        _ <- reply(GossipOk()) zipPar updateToFollowers(followers - src, numbers)
      yield ()

    case Read() =>
      ZIO.serviceWithZIO[Ref[State]](_.get.flatMap(state => reply(ReadOk(state.numbers))))

    case Topology(_) => reply(TopologyOk())
  }

  private val handler = for
    role <- ZIO.serviceWithZIO[Ref[State]](_.get.map(_.role))
    _ <- role match
      case NodeRole.Leader(followers) => leaderHandler(followers)
      case NodeRole.Follower(leader)  => followerHandler(leader)
  yield ()

  private val decideRole =
    given Ordering[NodeId] = Ordering.by[NodeId, String](_.toString)
    for
      _ <- logDebug("deciding first alphabetical node as leader")
      others <- MaelstromRuntime.others
      myId <- MaelstromRuntime.me
      leader = (others + myId).toSeq.min
      selfRole = if leader == myId then NodeRole.Leader(others) else NodeRole.Follower(leader)
      _ <- selfRole match
        case NodeRole.Leader(_)        => logInfo(s"I am the leader ($myId)!")
        case NodeRole.Follower(nodeId) => logInfo(s"leader is $nodeId")
    yield selfRole

  override val program =
    handler.provideSome[MaelstromRuntime](ZLayer.fromZIO(decideRole).flatMap(role => ZLayer.fromZIO(Ref.make(State(role.get)))))

  override val configure: NodeConfig = NodeConfig.withLogLevelDebug.withColoredLog
}
