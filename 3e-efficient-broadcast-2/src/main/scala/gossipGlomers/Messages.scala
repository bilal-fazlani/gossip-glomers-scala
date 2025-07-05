package gossipGlomers

import zio.json.*
import com.bilalfazlani.zioMaelstrom.{*, given}

// In Messages
sealed trait FollowerMessage derives JsonCodec
sealed trait LeaderMessage derives JsonCodec
case class Broadcast(message: Int)                      extends FollowerMessage, LeaderMessage
case class Read()                                       extends FollowerMessage, LeaderMessage
case class Topology(topology: Map[NodeId, Set[NodeId]]) extends FollowerMessage, LeaderMessage

// In/Out Messages
case class Report(numbers: Set[Int]) extends LeaderMessage derives JsonCodec
case class Update(numbers: Set[Int]) extends FollowerMessage derives JsonCodec

// Out Messages
case class BroadcastOk() derives JsonCodec
case class ReadOk(messages: Set[Int]) derives JsonCodec
case class TopologyOk() derives JsonCodec
case class ReportOk() derives JsonCodec
case class UpdateOk() derives JsonCodec
