package gossipGlomers

import zio.json.*
import com.bilalfazlani.zioMaelstrom.{*, given}

// In Messages
sealed trait InputMessage derives JsonCodec

case class Broadcast(message: Int)                      extends InputMessage
case class Read()                                       extends InputMessage
case class Topology(topology: Map[NodeId, Set[NodeId]]) extends InputMessage

// Bi-directional Messages
case class Gossip(messages: Set[Int]) extends InputMessage derives JsonCodec

// Out Messages

case class BroadcastOk() derives JsonCodec
case class ReadOk(messages: Set[Int]) derives JsonCodec
case class TopologyOk() derives JsonCodec
