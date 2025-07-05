package gossipGlomers

import zio.json.*
import com.bilalfazlani.zioMaelstrom.{*, given}

// In Messages
sealed trait InputMessage derives JsonDecoder
case class Add(delta: Long) extends InputMessage
case class Read()           extends InputMessage
case class Get()            extends InputMessage derives JsonEncoder

// Responses Messages
case class AddOk() derives JsonCodec
case class ReadOk(value: Long) derives JsonCodec
case class GetOk(value: Long) derives JsonCodec
