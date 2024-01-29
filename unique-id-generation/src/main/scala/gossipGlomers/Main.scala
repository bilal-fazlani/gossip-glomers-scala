package gossipGlomers

import zio.*
import zio.json.*
import com.bilalfazlani.zioMaelstrom.*

case class Generate(msg_id: MessageId) extends NeedsReply derives JsonCodec

case class GenerateOk(in_reply_to: MessageId, id: String, `type`: String = "generate_ok") extends Sendable, Reply
    derives JsonCodec

object Main extends ZIOAppDefault {

  val newId = ZIO.serviceWithZIO[Ref[Long]](_.updateAndGet(_ + 1))

  val handler = receive[Generate](msg => newId.flatMap(id => reply(GenerateOk(msg.msg_id, s"$me-$id"))))

  val run = handler.provideSome[Scope](
    MaelstromRuntime.live(_.logLevel(NodeLogLevel.Debug)),
    ZLayer.fromZIO(Ref.make(0L))
  )
}
