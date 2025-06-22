package gossipGlomers

import zio.*
import zio.json.*
import com.bilalfazlani.zioMaelstrom.*

case class Generate() derives JsonCodec

case class GenerateOk(id: String) derives JsonCodec

object Main extends MaelstromNode {

  val program =
    receive[Generate](_ =>
      for
        id <- ZIO.serviceWithZIO[Ref[Long]](_.getAndIncrement)
        _ <- reply(GenerateOk(s"$me-$id"))
      yield ()
    ).provideSome[MaelstromRuntime](ZLayer.fromZIO(Ref.make(0L)))
}
