package gossipGlomers

import zio.*
import zio.json.*
import com.bilalfazlani.zioMaelstrom.*

object Main extends MaelstromNode {

  case class State(
      numbers: Set[Int] = Set.empty
  ) {
    def addNumber(n: Int): State = copy(numbers = numbers + n)
    def addNumbers(ns: Set[Int]): State = copy(numbers = numbers ++ ns)
  }

  object State:
    def make = ZLayer.fromZIO(Ref.make(State()))
    def getNumbers = ZIO.serviceWithZIO[Ref[State]](_.get.map(_.numbers))
    def addNumber(n: Int) = ZIO.serviceWithZIO[Ref[State]](_.update(_.addNumber(n)))
    def addNumbers(ns: Set[Int]) = ZIO.serviceWithZIO[Ref[State]](_.update(_.addNumbers(ns)))

  val handler = receive[InputMessage] {
    case Broadcast(number) => State.addNumber(number) *> reply(BroadcastOk())

    case Read() => State.getNumbers.flatMap(nums => reply(ReadOk(nums)))

    case Topology(topology) => reply(TopologyOk()) *> startGossip(topology(me))

    case Gossip(messages) => State.addNumbers(messages)
  }

  def startGossip(neighbours: Set[NodeId]) =
    (for {
      nums <- State.getNumbers
      _ <- ZIO.foreachPar(neighbours)(_ send Gossip(nums))
    } yield ()).repeat(Schedule.fixed(300.millis)).forkScoped.unit

  def program = handler.provideSome[MaelstromRuntime & Scope](State.make)
}
