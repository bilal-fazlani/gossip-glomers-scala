package gossipGlomers

import zio.*
import zio.json.*
import com.bilalfazlani.zioMaelstrom.*
import com.bilalfazlani.zioMaelstrom.protocol.*

case class Leader(followers: Set[NodeId]) extends Node {
  def start = handleMessages zipPar syncMessages

  def handleMessages = receive[LeaderMessage] {
    case Broadcast(number, msg_id) =>
      State.addNewNumber(number) *> reply(BroadcastOk(msg_id))

    case Report(numbers, msg_id, _) =>
      State.addNewNumbers(numbers) *> reply(ReportOk(msg_id))

    case Read(msg_id) =>
      State.get.flatMap(state => reply(ReadOk(msg_id, state.oldNumbers ++ state.newNumbers)))

    case Topology(_, msg_id) => reply(TopologyOk(msg_id))
  }

  def syncMessages =
    State
      .move { state =>
        val newState = state.incMessageId
        val nextMessageId = newState.currentMessageId
        (
          ZIO.foreachPar(followers)(_.ask[UpdateOk](Update(newState.newNumbers, nextMessageId), 150.millis))
            *> logInfo(s"updated all followers with ${newState.newNumbers}")
        ).catchAll(e => logWarn(s"reporting failed: ${e}"))
      }
      .repeat(Schedule.fixed(150.millis).jittered)
      .unit

}
