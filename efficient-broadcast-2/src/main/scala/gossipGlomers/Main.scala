package gossipGlomers

import zio.*
import zio.json.*
import com.bilalfazlani.zioMaelstrom.*
import com.bilalfazlani.zioMaelstrom.protocol.*

case class State(
    oldNumbers: Set[Int] = Set.empty,
    newNumbers: Set[Int] = Set.empty,
    currentMessageId: MessageId = MessageId(0)
) {
  def addOldNumbers(ns: Set[Int]): State = copy(oldNumbers = oldNumbers ++ ns)
  def addBroadcast(n: Int): State = copy(newNumbers = newNumbers + n)
  def incMessageId = copy(currentMessageId = currentMessageId.inc)
}

def nextMsgId = ZIO.serviceWithZIO[Ref[State]](_.updateAndGet(_.incMessageId).map(_.currentMessageId))

object Main extends ZIOAppDefault {
  def run = Node.start
    .provideSome[Scope](
      MaelstromRuntime.live(Settings(logLevel = NodeLogLevel.Info)),
      Node.live,
      ZLayer.fromZIO(Ref.make(State()))
    )
}

sealed trait Node:
  def start: ZIO[Ref[State] & MaelstromRuntime & Scope, Nothing, Unit]

object Node:

  private val makeNode =
    enum NodeRole:
      case Leader(followers: Set[NodeId])
      case Follower(leader: NodeId)

    given Ordering[NodeId] = Ordering.by[NodeId, String](_.toString)
    for
      _ <- logDebug("deciding first alphabetical node as leader")
      others <- getOtherNodeIds
      myId <- getMyNodeId
      leader = (others + myId).toSeq.sorted.head
      leaderInfo = if leader == myId then NodeRole.Leader(others) else NodeRole.Follower(leader)
      node <- leaderInfo match
        case NodeRole.Leader(_) =>
          logInfo(s"I am the leader ($myId)!") as Leader(others)
        case NodeRole.Follower(nodeId) =>
          logInfo(s"leader is $nodeId") as Follower(nodeId)
    yield node

  val live = ZLayer.fromZIO(makeNode)

  val start = ZIO.serviceWithZIO[Node](_.start)

case class Follower(leader: NodeId) extends Node {

  def start = handler

  def gossipToLeader(number: Int) =
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

  def handler = receive[FollowerMessage] {
    case Broadcast(number, msg_id) =>
      ZIO.serviceWithZIO[Ref[State]](_.update(_.addBroadcast(number))) *>
        (reply(BroadcastOk(msg_id)) zipPar gossipToLeader(number))

    case Update(numbers, msgId, _) =>
      ZIO.serviceWithZIO[Ref[State]](_.update(_.addOldNumbers(numbers))) *> reply(UpdateOk(msgId))

    case Read(msg_id) =>
      ZIO.serviceWithZIO[Ref[State]](
        _.get.flatMap(state => reply(ReadOk(msg_id, state.oldNumbers ++ state.newNumbers)))
      )

    case Topology(_, msg_id) => reply(TopologyOk(msg_id))
  }
}

case class Leader(followers: Set[NodeId]) extends Node {

  def start = handler

  def handler = receive[LeaderMessage] {
    case Broadcast(number, msg_id) =>
      ZIO.serviceWithZIO[Ref[State]](_.update(_.addBroadcast(number))) *>
        reply(BroadcastOk(msg_id)) zipPar updateToFollowers(Set(number), Set.empty)

    case Gossip(numbers, msg_id, _) =>
      ZIO.serviceWithZIO[Ref[State]](_.update(_.addOldNumbers(numbers))) *>
        (reply(GossipOk(msg_id)) zipPar updateToFollowers(numbers, Set(src)))

    case Read(msg_id) =>
      ZIO.serviceWithZIO[Ref[State]](
        _.get.flatMap(state => reply(ReadOk(msg_id, state.oldNumbers ++ state.newNumbers)))
      )

    case Topology(_, msg_id) => reply(TopologyOk(msg_id))
  }

  def updateToFollowers(numbers: Set[Int], except: Set[NodeId]) =
    val retryCount = 10
    (
      for
        nextMessageId <- nextMsgId
        state <- ZIO.serviceWithZIO[Ref[State]](_.get)
        _ <- ZIO.foreachPar(followers -- except) { follower =>
          follower
            .ask[UpdateOk](Update(numbers, nextMessageId), 2.seconds)
            .retry(Schedule.recurs(retryCount))
            .tapError(e => logError(s"update ask failed after $retryCount retries: ${e}"))
            .catchAll(_ => ZIO.unit)
        }
      yield ()
    ).unit
}
