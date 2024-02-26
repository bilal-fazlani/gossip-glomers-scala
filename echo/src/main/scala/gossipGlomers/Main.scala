package gossipGlomers

import zio.*
import zio.json.*
import com.bilalfazlani.zioMaelstrom.*

case class Echo(echo: String, msg_id: MessageId) extends NeedsReply derives JsonCodec

case class EchoOk(in_reply_to: MessageId, echo: String, `type`: String = "echo_ok") extends Sendable, Reply
    derives JsonCodec

object Main extends MaelstromNode {

  override val configure: NodeConfig = NodeConfig().withLogLevelDebug

  val program = receive[Echo](msg => reply(EchoOk(msg.msg_id, msg.echo)))
}
