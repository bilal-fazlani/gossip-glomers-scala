package gossipGlomers

import com.bilalfazlani.zioMaelstrom.protocol.MessageId

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
