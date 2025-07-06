package gossipGlomers

import com.bilalfazlani.zioMaelstrom.{*, given}
import com.bilalfazlani.zioMaelstrom.InputStream.InlineMessage
import com.bilalfazlani.zioMaelstrom.models.{Body, MsgName}
import zio.json.*
import zio.{Ref, ZIO, ZLayer}

object Main extends MaelstromNode {
  val program = receive[InputMessage] {
    case Send(key, msg) =>
      for
        state   <- ZIO.service[State]
        updated <- state.updateAndGet(_.addMsg(key, msg))
        _       <- reply(SendOk(updated.keyDataFor(key).get.offset))
      yield ()
    case Poll(offsets) =>
      for
        state      <- ZIO.service[State]
        pollResult <- state.get.map(_.poll(offsets))
        _          <- reply(PollOk(pollResult))
      yield ()
    case CommitOffsets(offsets) =>
      for
        state <- ZIO.service[State]
        _     <- state.update(_.commit(offsets))
        _     <- reply(CommitOffsetsOk())
      yield ()
    case ListCommittedOffsets(keys) =>
      for
        state            <- ZIO.service[State]
        committedOffsets <- state.get.map(_.getCommittedOffsets(keys))
        _                <- reply(ListCommittedOffsetsOk(committedOffsets))
      yield ()
  }.provideSome[MaelstromRuntime](ZLayer(Ref.make(AllData.empty)))
}

type State = Ref[AllData]
