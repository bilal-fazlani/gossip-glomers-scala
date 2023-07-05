package gossipGlomers

import zio.*
import zio.json.*
import com.bilalfazlani.zioMaelstrom.*
import com.bilalfazlani.zioMaelstrom.protocol.*

case class Echo(echo: String, msg_id: MessageId) extends NeedsReply derives JsonCodec

case class EchoOk(in_reply_to: MessageId, echo: String, `type`: String = "echo_ok") extends Sendable, Reply
    derives JsonCodec

object Main extends ZIOAppDefault {

  def handler = receive[Echo] { case Echo(echo, msg_id) =>
    reply(EchoOk(msg_id, echo))
  }

  def run = handler.provideSome[Scope](
    MaelstromRuntime.live(Settings(logLevel = NodeLogLevel.Debug))
  )
}
