package gossipGlomers

import zio.*
import zio.json.*
import com.bilalfazlani.zioMaelstrom.*
import com.bilalfazlani.zioMaelstrom.protocol.*

object EfficientBroadcast1 extends ZIOAppDefault {

  enum NodeRole:
    case Leader(followers: Set[NodeId])
    case Follower(of: NodeId)

  case class State(
      role: NodeRole,
      numbers: Set[Int] = Set.empty,
      currentMessageId: MessageId = MessageId(0)
  ) {
    def addNumbers(ns: Set[Int]): State = copy(numbers = numbers ++ ns)
    def addNumber(n: Int): State = copy(numbers = numbers + n)
    def incMessageId = copy(currentMessageId = currentMessageId.inc)
  }

  private def nextMsgId = ZIO.serviceWithZIO[Ref[State]](_.updateAndGet(_.incMessageId).map(_.currentMessageId))

  def gossipToLeader(leader: NodeId, number: Int) =
    val retryCount = 10
    (
      for
        nextMessageId <- nextMsgId
        state <- ZIO.serviceWithZIO[Ref[State]](_.get)
        _ <- leader
          .ask[GossipOk](Gossip(number, nextMessageId), 2.seconds)
          .retry(Schedule.recurs(retryCount))
          .tapError(e => logError(s"gossip ask failed after $retryCount retries: ${e}"))
          .catchAll(_ => ZIO.unit)
      yield ()
    ).unit

  def updateToFollowers(followers: Set[NodeId], numbers: Set[Int]) =
    val retryCount = 10
    (
      for
        nextMessageId <- nextMsgId
        state <- ZIO.serviceWithZIO[Ref[State]](_.get)
        _ <- ZIO.foreachPar(followers) { follower =>
          follower
            .ask[UpdateOk](Update(numbers, nextMessageId), 2.seconds)
            .retry(Schedule.recurs(retryCount))
            .tapError(e => logError(s"update ask failed after $retryCount retries: ${e}"))
            .catchAll(_ => ZIO.unit)
        }
      yield ()
    ).unit

  def followerHandler(leader: NodeId) = receive[FollowerMessage] {
    case Broadcast(number, msg_id) =>
      ZIO.serviceWithZIO[Ref[State]](_.update(_.addNumber(number))) *>
        (reply(BroadcastOk(msg_id)) zipPar gossipToLeader(leader, number))

    case Update(numbers, msgId, _) =>
      ZIO.serviceWithZIO[Ref[State]](_.update(_.addNumbers(numbers))) *> reply(UpdateOk(msgId))

    case Read(msg_id) =>
      ZIO.serviceWithZIO[Ref[State]](_.get.flatMap(state => reply(ReadOk(msg_id, state.numbers))))

    case Topology(_, msg_id) => reply(TopologyOk(msg_id))
  }

  def leaderHandler(followers: Set[NodeId]) = receive[LeaderMessage] {
    case Broadcast(number, msg_id) =>
      ZIO.serviceWithZIO[Ref[State]](_.update(_.addNumber(number))) *>
        reply(BroadcastOk(msg_id)) zipPar updateToFollowers(followers, Set(number))

    case Gossip(numbers, msg_id, _) =>
      ZIO.serviceWithZIO[Ref[State]](_.update(_.addNumbers(numbers))) *>
        (reply(GossipOk(msg_id)) zipPar updateToFollowers(followers - src, numbers))

    case Read(msg_id) =>
      ZIO.serviceWithZIO[Ref[State]](_.get.flatMap(state => reply(ReadOk(msg_id, state.numbers))))

    case Topology(_, msg_id) => reply(TopologyOk(msg_id))
  }

  def handler = for
    role <- ZIO.serviceWithZIO[Ref[State]](_.get.map(_.role))
    _ <- role match
      case NodeRole.Leader(followers) => leaderHandler(followers)
      case NodeRole.Follower(leader)  => followerHandler(leader)
  yield ()

  val decideRole =
    given Ordering[NodeId] = Ordering.by[NodeId, String](_.toString)
    for
      _ <- logDebug("deciding first alphabetical node as leader")
      others <- getOtherNodeIds
      myId <- getMyNodeId
      leader = (others + myId).toSeq.sorted.head
      leaderInfo = if leader == myId then NodeRole.Leader(others) else NodeRole.Follower(leader)
      _ <- leaderInfo match
        case NodeRole.Leader(_)        => logInfo(s"I am the leader ($myId)!")
        case NodeRole.Follower(nodeId) => logInfo(s"leader is $nodeId")
    yield leaderInfo

  def run = handler.provideSome[Scope](
    MaelstromRuntime.live(Settings(logLevel = NodeLogLevel.Info)),
    ZLayer.fromZIO(decideRole).flatMap(role => ZLayer.fromZIO(Ref.make(State(role.get))))
  )
}
