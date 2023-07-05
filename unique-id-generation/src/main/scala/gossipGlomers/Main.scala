package gossipGlomers

import zio.*
import zio.json.*
import com.bilalfazlani.zioMaelstrom.*
import com.bilalfazlani.zioMaelstrom.protocol.*

case class Generate(msg_id: MessageId) extends NeedsReply derives JsonCodec

case class GenerateOk(in_reply_to: MessageId, id: String, `type`: String = "generate_ok") extends Sendable, Reply
    derives JsonCodec

object Main extends ZIOAppDefault {

  def handler = receive[Generate] { case Generate(msg_id) =>
    ZIO.serviceWithZIO[Ref[Long]](_.updateAndGet(_ + 1).flatMap { newId =>
      reply(GenerateOk(msg_id, s"$me-$newId"))
    })
  }

  def run = handler.provideSome[Scope](
    MaelstromRuntime.live(Settings(logLevel = NodeLogLevel.Debug)),
    ZLayer.fromZIO(Ref.make(0L))
  )
}
