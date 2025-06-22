package gossipGlomers

import zio.*
import zio.json.*
import com.bilalfazlani.zioMaelstrom.*

case class Echo(echo: String) derives JsonCodec

case class EchoOk(echo: String) derives JsonCodec

object Main extends MaelstromNode:
  val program = receive[Echo](msg => reply(EchoOk(msg.echo)))
