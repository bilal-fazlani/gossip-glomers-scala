package gossipGlomers

import zio.*
import com.bilalfazlani.zioMaelstrom.*

object Main extends MaelstromNode {
  
  override val configure: NodeConfig = NodeConfig().withLogLevelDebug

  val program = {
    for
      myId <- getMyNodeId
      other <- getOtherNodeIds
      _ <- Node.start(myId, other)
    yield ()
  }.provideRemaining(State.empty)
}
