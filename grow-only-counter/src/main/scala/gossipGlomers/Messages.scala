package gossipGlomers

import zio.json.*
import com.bilalfazlani.zioMaelstrom.*

// In Messages

@jsonDiscriminator("type")
sealed trait InputMessage derives JsonCodec

@jsonHint("add")
case class Add(delta: Long) extends InputMessage

@jsonHint("read")
case class Read() extends InputMessage

// In/Out Messages
@jsonHint("get")
case class Get() extends InputMessage derives JsonCodec

// Out Messages

case class AddOk() derives JsonCodec

case class ReadOk(value: Long) derives JsonCodec

case class GetOk(value: Long) derives JsonCodec
