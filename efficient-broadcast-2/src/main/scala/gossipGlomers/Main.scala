package gossipGlomers

import zio.*
import com.bilalfazlani.zioMaelstrom.*

object Main extends ZIOAppDefault {
  def run = Node.start
    .provideSome[Scope](
      MaelstromRuntime.live(Settings(logLevel = NodeLogLevel.Debug)),
      Node.live,
      ZLayer.fromZIO(Ref.Synchronized.make(State()))
    )
}
