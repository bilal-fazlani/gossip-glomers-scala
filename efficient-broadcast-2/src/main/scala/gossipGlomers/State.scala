package gossipGlomers

import com.bilalfazlani.zioMaelstrom.protocol.MessageId
import zio.ZIO
import zio.Ref
import zio.Ref.Synchronized

case class State(
    oldNumbers: Set[Int] = Set.empty,
    newNumbers: Set[Int] = Set.empty,
    currentMessageId: MessageId = MessageId(0)
) {
  def addOldNumbers(ns: Set[Int]): State = copy(oldNumbers = oldNumbers ++ ns)
  def addNewNumber(n: Int): State = copy(newNumbers = newNumbers + n)
  def addNewNumbers(n: Set[Int]): State = copy(newNumbers = newNumbers ++ n)
  def incMessageId = copy(currentMessageId = currentMessageId.inc)
  def move = copy(oldNumbers = oldNumbers ++ newNumbers, newNumbers = Set.empty)
}

object State {
  def addOldNumbers(ns: Set[Int]) = ZIO.serviceWithZIO[Ref[State]](_.update(_.addOldNumbers(ns)))
  def addNewNumber(n: Int) = ZIO.serviceWithZIO[Ref[State]](_.update(_.addNewNumber(n)))
  def addNewNumbers(ns: Set[Int]) = ZIO.serviceWithZIO[Ref[State]](_.update(_.addNewNumbers(ns)))
  def get = ZIO.serviceWithZIO[Ref[State]](_.get)
  def incMessageId = ZIO.serviceWithZIO[Ref[State]](_.modify { s =>
    val newState = s.incMessageId
    (newState.currentMessageId, newState)
  })
  def move[R, E](effect: State => ZIO[R, E, ?]): ZIO[R & Synchronized[State], E, Unit] =
    ZIO.serviceWithZIO[Ref.Synchronized[State]](_.updateSomeZIO {
      case state if state.newNumbers.nonEmpty => effect(state) *> ZIO.succeed(state.move)
    })
}
