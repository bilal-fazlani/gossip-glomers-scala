package gossipGlomers

import zio.json.*
import com.bilalfazlani.zioMaelstrom.given

given JsonEncoder[Map[Key, Offset]] =
  JsonEncoder[Map[String, Int]].contramap[Map[Key, Offset]](_.map((k, v) => (k.toString, v.number)))

given JsonDecoder[Map[Key, Offset]] =
  JsonDecoder[Map[String, Int]].map[Map[Key, Offset]](_.map((k, v) => (Key(k), Offset(v))))

sealed trait InputMessage derives JsonCodec
case class Send(key: Key, msg: Value)               extends InputMessage
case class Poll(offsets: Map[Key, Offset])          extends InputMessage
case class CommitOffsets(offsets: Map[Key, Offset]) extends InputMessage
case class ListCommittedOffsets(keys: Set[Key])     extends InputMessage

case class SendOk(offset: Offset) derives JsonEncoder
case class PollOk(msgs: Map[Key, List[OffsetValue]]) derives JsonEncoder
case class CommitOffsetsOk() derives JsonEncoder
case class ListCommittedOffsetsOk(offsets: Map[Key, Offset]) derives JsonEncoder
