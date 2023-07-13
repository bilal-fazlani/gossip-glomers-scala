package gossipGlomers

import zio.*
import zio.json.*
import com.bilalfazlani.zioMaelstrom.*

case class Follower(leader: NodeId) extends Node {
  def start = handleMessages zipPar syncMessages

  def handleMessages = receive[FollowerMessage] {
    case Broadcast(number, msg_id) =>
      State.addNewNumber(number) *> reply(BroadcastOk(msg_id))

    case Update(numbers, msgId, _) =>
      State.addOldNumbers(numbers) *> reply(UpdateOk(msgId))

    case Read(msg_id) =>
      State.get.flatMap(state => reply(ReadOk(msg_id, state.oldNumbers ++ state.newNumbers)))

    case Topology(_, msg_id) => reply(TopologyOk(msg_id))
  }

  def syncMessages = State
    .move { state =>
      for
        nextMessageId <- MessageId.next
        _ <- leader.ask[ReportOk](Report(state.newNumbers, nextMessageId), 150.millis)
        _ <- logInfo(s"report ${state.newNumbers} to leader")
      yield ()
    }
    .catchAll(e => logWarn(s"reporting failed: ${e}"))
    .repeat(Schedule.fixed(150.millis).jittered)
    .unit
}
