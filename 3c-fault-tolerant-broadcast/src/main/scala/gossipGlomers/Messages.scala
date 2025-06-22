package gossipGlomers

import zio.json.*
import com.bilalfazlani.zioMaelstrom.*

// In Messages

@jsonDiscriminator("type")
sealed trait InputMessage derives JsonCodec

@jsonHint("broadcast")
case class Broadcast(message: Int) extends InputMessage

@jsonHint("read")
case class Read() extends InputMessage

@jsonHint("topology")
case class Topology(topology: Map[NodeId, Set[NodeId]]) extends InputMessage

// Bi-directional Messages

@jsonHint("gossip")
case class Gossip(messages: Set[Int]) extends InputMessage derives JsonCodec

// Out Messages

case class BroadcastOk() derives JsonCodec

case class ReadOk(messages: Set[Int]) derives JsonCodec

case class TopologyOk() derives JsonCodec
