package gossipGlomers

import zio.json.*
import com.bilalfazlani.zioMaelstrom.*

// In Messages

@jsonDiscriminator("type")
sealed trait InputMessage derives JsonCodec

@jsonHint("broadcast")
case class Broadcast(message: Int, msg_id: MessageId) extends InputMessage, NeedsReply

@jsonHint("read")
case class Read(msg_id: MessageId) extends InputMessage, NeedsReply

@jsonHint("topology")
case class Topology(topology: Map[NodeId, Set[NodeId]], msg_id: MessageId) extends InputMessage, NeedsReply

// Bi-directional Messages

@jsonHint("gossip")
case class Gossip(messages: Set[Int], `type`: String = "gossip") extends InputMessage, Sendable derives JsonCodec

// Out Messages

case class BroadcastOk(in_reply_to: MessageId, `type`: String = "broadcast_ok") extends Reply, Sendable
    derives JsonCodec

case class ReadOk(in_reply_to: MessageId, messages: Set[Int], `type`: String = "read_ok") extends Reply, Sendable
    derives JsonCodec

case class TopologyOk(in_reply_to: MessageId, `type`: String = "topology_ok") extends Reply, Sendable derives JsonCodec
