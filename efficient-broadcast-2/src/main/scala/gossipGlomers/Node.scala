package gossipGlomers

import zio.*
import com.bilalfazlani.zioMaelstrom.*

trait Node:
  def start: ZIO[Ref.Synchronized[State] & MaelstromRuntime & Scope, Nothing, Unit]

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
      leader = (others + myId).toSeq.min
      leaderInfo = if leader == myId then NodeRole.Leader(others) else NodeRole.Follower(leader)
      node <- leaderInfo match
        case NodeRole.Leader(_) =>
          logInfo(s"I am the leader ($myId)!") as Leader(others)
        case NodeRole.Follower(nodeId) =>
          logInfo(s"leader is $nodeId") as Follower(nodeId)
    yield node

  val live = ZLayer.fromZIO(makeNode)

  val start = ZIO.serviceWithZIO[Node](_.start)
