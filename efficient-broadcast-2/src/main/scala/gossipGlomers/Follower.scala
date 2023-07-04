package gossipGlomers

import zio.*
import zio.json.*
import com.bilalfazlani.zioMaelstrom.*
import com.bilalfazlani.zioMaelstrom.protocol.*

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
      val newState = state.incMessageId
      val nextMessageId = newState.currentMessageId
      (
        leader.ask[UpdateOk](Update(newState.newNumbers, nextMessageId), 500.millis)
          *> logInfo(s"updated leader with ${newState.newNumbers}")
      ).catchAll(e => logWarn(s"reporting failed: ${e}"))
    }
    .repeat(Schedule.fixed(700.millis))
    .unit
}
