package gossipGlomers

import zio.json.*
import com.bilalfazlani.zioMaelstrom.*

// In Messages

@jsonDiscriminator("type")
sealed trait FollowerMessage derives JsonCodec

@jsonDiscriminator("type")
sealed trait LeaderMessage derives JsonCodec

@jsonHint("broadcast")
case class Broadcast(message: Int) extends FollowerMessage, LeaderMessage

@jsonHint("read")
case class Read() extends FollowerMessage, LeaderMessage

@jsonHint("topology")
case class Topology(topology: Map[NodeId, Set[NodeId]]) extends FollowerMessage, LeaderMessage

// In/Out Messages
@jsonHint("gossip")
case class Gossip(numbers: Set[Int]) extends LeaderMessage derives JsonCodec

object Gossip {
  def apply(number: Int): Gossip = Gossip(Set(number))
}

@jsonHint("update")
case class Update(numbers: Set[Int]) extends FollowerMessage derives JsonCodec

// Out Messages

case class BroadcastOk() derives JsonCodec

case class ReadOk(messages: Set[Int]) derives JsonCodec

case class TopologyOk() derives JsonCodec

case class GossipOk() derives JsonCodec

case class UpdateOk() derives JsonCodec
