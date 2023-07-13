package gossipGlomers

import zio.json.*
import com.bilalfazlani.zioMaelstrom.*

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

@jsonHint("report")
case class Report(numbers: Set[Int], msg_id: MessageId, `type`: String = "report")
    extends LeaderMessage,
      Sendable,
      NeedsReply derives JsonCodec

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

case class ReportOk(in_reply_to: MessageId, `type`: String = "report_ok") extends Reply, Sendable derives JsonCodec

case class UpdateOk(in_reply_to: MessageId, `type`: String = "update_ok") extends Reply, Sendable derives JsonCodec
