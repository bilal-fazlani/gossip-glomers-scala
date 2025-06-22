package gossipGlomers

import zio.*
import zio.json.*
import com.bilalfazlani.zioMaelstrom.*
import zio.ZIO.{logInfo, logWarning}
case class Leader(followers: Set[NodeId]) extends Node {
  def start = handleMessages zipPar syncMessages

  def handleMessages = receive[LeaderMessage] {
    case Broadcast(number) =>
      State.addNewNumber(number) *> reply(BroadcastOk())

    case Report(numbers) =>
      State.addNewNumbers(numbers) *> reply(ReportOk())

    case Read() =>
      State.get.flatMap(state => reply(ReadOk(state.oldNumbers ++ state.newNumbers)))

    case Topology(_) => reply(TopologyOk())
  }

  def syncMessages =
    State
      .move { state =>
        for
          _ <- ZIO.foreachPar(followers)(_.ask[UpdateOk](Update(state.newNumbers), 150.millis))
          _ <- logInfo(s"updated all followers with ${state.newNumbers}")
        yield ()
      }
      .catchAll(e => logWarning(s"reporting failed: ${e}"))
      .repeat(Schedule.fixed(150.millis).jittered)
      .unit

}
