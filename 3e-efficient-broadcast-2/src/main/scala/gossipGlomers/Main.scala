package gossipGlomers

import zio.*
import com.bilalfazlani.zioMaelstrom.*
import zio.Ref.Synchronized
import com.bilalfazlani.zioMaelstrom.Initialisation
import com.bilalfazlani.zioMaelstrom.Settings

object Main extends MaelstromNode {

  override val configure: NodeConfig = NodeConfig.withLogLevelDebug.withColoredLog

  def program = Node.start
    .provideSome[MaelstromRuntime & Scope](
      Node.live,
      ZLayer.fromZIO(Ref.Synchronized.make(State()))
    )
}
