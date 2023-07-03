package gossipGlomers

import zio.*
import zio.json.*
import com.bilalfazlani.zioMaelstrom.*
import com.bilalfazlani.zioMaelstrom.protocol.*

case class Leader(followers: Set[NodeId]) extends Node {
  def start = handleMessages zipPar syncMessages

  def handleMessages = receive[LeaderMessage] {
    case Broadcast(number, msg_id) =>
      ZIO.serviceWithZIO[Ref[State]](_.update(_.addNewNumber(number))) *>
        reply(BroadcastOk(msg_id))

    case Report(numbers, msg_id, _) =>
      ZIO.serviceWithZIO[Ref[State]](_.update(_.addNewNumbers(numbers))) *>
        reply(ReportOk(msg_id))

    case Read(msg_id) =>
      ZIO.serviceWithZIO[Ref[State]](
        _.get.flatMap(state => reply(ReadOk(msg_id, state.oldNumbers ++ state.newNumbers)))
      )

    case Topology(_, msg_id) => reply(TopologyOk(msg_id))
  }

  def syncMessages =
    (for
      ref <- ZIO.service[Ref.Synchronized[State]]
      _ <- ref.updateSomeZIO {
        case s if s.newNumbers.nonEmpty =>
          for
            nextMessageId <- nextMsgId
            _ <- ZIO.foreachPar(followers)(_.ask[UpdateOk](Update(s.newNumbers, nextMessageId), 500.millis))
            _ <- logInfo(s"updated all followers with ${s.newNumbers}")
          yield s.move
      } catchAll (e => logWarn(s"updating failed: ${e}"))
    yield ()).repeat(Schedule.fixed(700.millis)).unit
}
