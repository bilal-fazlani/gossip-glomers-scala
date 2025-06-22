package gossipGlomers

import zio.*
import zio.json.*
import com.bilalfazlani.zioMaelstrom.*

object Main extends MaelstromNode {

  case class State(
      numbers: Set[Int] = Set.empty
  ) {
    def addNumber(n: Int): State = copy(numbers = numbers + n)
  }

  object State:
    def make = ZLayer.fromZIO(Ref.make(State()))
    def getNumbers = ZIO.serviceWithZIO[Ref[State]](_.get.map(_.numbers))
    def addNumber(n: Int) = ZIO.serviceWithZIO[Ref[State]](_.update(_.addNumber(n)))

  val handler = receive[InputMessage] {
    case Broadcast(number) =>
      State.addNumber(number) *> reply(BroadcastOk())

    case Read() =>
      State.getNumbers.flatMap(nums => reply(ReadOk(nums)))

    case Topology(_) => reply(TopologyOk())
  }

  def program = handler.provideSome[MaelstromRuntime](State.make)
}
