package gossipGlomers

import zio.json.*
import com.bilalfazlani.zioMaelstrom.protocol.*

// In Messages

@jsonDiscriminator("type")
sealed trait FollowerMessage derives JsonCodec

@jsonDiscriminator("type")
sealed trait LeaderMessage derives JsonCodec

@jsonHint("broadcast")
case class Broadcast(message: Int, msg_id: MessageId) extends FollowerMessage, LeaderMessage, NeedsReply

@jsonHint("read")
case class Read(msg_id: MessageId) extends FollowerMessage, LeaderMessage, NeedsReply

@jsonHint("topology")
case class Topology(topology: Map[NodeId, Set[NodeId]], msg_id: MessageId)
    extends FollowerMessage,
      LeaderMessage,
      NeedsReply

// In/Out Messages

@jsonHint("gossip")
case class Gossip(numbers: Set[Int], msg_id: MessageId, `type`: String = "gossip")
    extends LeaderMessage,
      Sendable,
      NeedsReply derives JsonCodec
object Gossip {
  def apply(number: Int, msg_id: MessageId): Gossip = Gossip(Set(number), msg_id)
}

@jsonHint("update")
case class Update(numbers: Set[Int], msg_id: MessageId, `type`: String = "update")
    extends FollowerMessage,
      Sendable,
      NeedsReply derives JsonCodec

// Out Messages

case class BroadcastOk(in_reply_to: MessageId, `type`: String = "broadcast_ok") extends Reply, Sendable
    derives JsonCodec

case class ReadOk(in_reply_to: MessageId, messages: Set[Int], `type`: String = "read_ok") extends Reply, Sendable
    derives JsonCodec

case class TopologyOk(in_reply_to: MessageId, `type`: String = "topology_ok") extends Reply, Sendable derives JsonCodec

case class GossipOk(in_reply_to: MessageId, `type`: String = "gossip_ok") extends Reply, Sendable derives JsonCodec

case class UpdateOk(in_reply_to: MessageId, `type`: String = "update_ok") extends Reply, Sendable derives JsonCodec
