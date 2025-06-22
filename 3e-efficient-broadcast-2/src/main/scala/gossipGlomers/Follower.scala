package gossipGlomers

import com.bilalfazlani.zioMaelstrom.*
import zio.*
import zio.ZIO.{logInfo, logWarning}
import zio.json.*

case class Follower(leader: NodeId) extends Node {
  def start = handleMessages zipPar syncMessages

  def handleMessages = receive[FollowerMessage] {
    case Broadcast(number) =>
      State.addNewNumber(number) *> reply(BroadcastOk())

    case Update(numbers) =>
      State.addOldNumbers(numbers) *> reply(UpdateOk())

    case Read() =>
      State.get.flatMap(state => reply(ReadOk(state.oldNumbers ++ state.newNumbers)))

    case Topology(_) => reply(TopologyOk())
  }

  def syncMessages = State
    .move { state =>
      for
        _ <- leader.ask[ReportOk](Report(state.newNumbers), 150.millis)
        _ <- logInfo(s"report ${state.newNumbers} to leader")
      yield ()
    }
    .catchAll(e => logWarning(s"reporting failed: ${e}"))
    .repeat(Schedule.fixed(150.millis).jittered)
    .unit
}
