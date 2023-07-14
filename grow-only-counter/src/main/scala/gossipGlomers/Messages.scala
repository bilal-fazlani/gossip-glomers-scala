package gossipGlomers

import zio.json.*
import com.bilalfazlani.zioMaelstrom.*

// In Messages

@jsonDiscriminator("type")
sealed trait InputMessage derives JsonCodec

@jsonHint("add")
case class Add(delta: Long, msg_id: MessageId) extends InputMessage, NeedsReply

@jsonHint("read")
case class Read(msg_id: MessageId) extends InputMessage, NeedsReply

// In/Out Messages
@jsonHint("get")
case class Get(msg_id: MessageId, `type`: String = "get") extends InputMessage, NeedsReply, Sendable derives JsonCodec

// Out Messages

case class AddOk(in_reply_to: MessageId, `type`: String = "add_ok") extends Reply, Sendable derives JsonCodec

case class ReadOk(in_reply_to: MessageId, value: Long, `type`: String = "read_ok") extends Reply, Sendable
    derives JsonCodec

case class GetOk(in_reply_to: MessageId, value: Long, `type`: String = "get_ok") extends Reply, Sendable
    derives JsonCodec
