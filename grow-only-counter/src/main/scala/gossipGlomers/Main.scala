package gossipGlomers

import zio.*
import zio.json.*
import com.bilalfazlani.zioMaelstrom.*
import com.bilalfazlani.zioMaelstrom.services.SeqKv
import zio.ZIO.{logError, logWarning}

object Main extends MaelstromNode {

  private def readSelf(me: NodeId) =
    SeqKv
      .read[Long](me, 300.millis)
      .catchSome { case ErrorMessage(_, ErrorCode.KeyDoesNotExist, _, _) =>
        ZIO.succeed(0L)
      }
      .tapError(e => logError(s"failed to read key $me from seq-kv. error: $e"))

  private def readOther(remote: NodeId) =
    remote
      .ask[GetOk](Get(), 500.millis)
      .map(_.value)
      .tapError(e => logWarning(s"an attempt to read from $remote failed with error: $e"))
      .retryN(2)
      .tapError(e => logError(s"failed to read from $remote. error: $e"))

  private def add(delta: Long, source: NodeId, me: NodeId): ZIO[MaelstromRuntime, AskError, Unit] =
    for
      _ <- SeqKv
        .update[NodeId, Long](me, 300.millis) {
          case Some(currentValue) => currentValue + delta
          case None               => delta
        }
        .tapError(e => logWarning(s"an attempt to add key $me failed with error: $e"))
      _ <- source send AddOk()
    yield ()

  private def replyError(askError: AskError, msg_id: Option[MessageId], remote: NodeId) =
    (askError, msg_id) match
      case (t: Timeout, Some(msg_id)) =>
        remote send ErrorMessage(msg_id, ErrorCode.Timeout, "downstream request timed out")
      case (e: ErrorMessage, Some(msg_id)) => remote send e.copy(in_reply_to = msg_id)
      case (d: DecodingFailure, Some(msg_id)) =>
        remote send ErrorMessage(msg_id, ErrorCode.NotSupported, "functionality seems to be broken")
      case _ => ZIO.unit

  val program = receive[InputMessage] {
    case Add(delta) =>
      add(delta, src, me)
        .retryN(2)
        .tapError(e => logError(s"could not add key $me all attempts exhausted. error: $e"))
        .catchAll(replyError(_, summon[Option[MessageId]], src))

    case Get() =>
      for
        response <- readSelf(me)
          .retryN(2)
          .tapError(e => logError(s"could not read key $me all attempts exhausted. error: $e"))
          .map(Some(_))
          .catchAll(replyError(_, summon[Option[MessageId]], src) as None)
        _ <- reply(GetOk(response.get)) // todo: this will break if response is None
      yield ()

    case Read() =>
      for
        myValueFiber <- readSelf(me)
          .retryN(2)
          .map(Some(_))
          .forkScoped
        othersTotal <-
          ZIO
            .mergeAllPar(others.map(readOther))(0L)(_ + _)
            .map(Some(_))
            .catchAll(replyError(_, summon[Option[MessageId]], src) as None)
        myValue <- myValueFiber.join.catchAll(replyError(_, summon[Option[MessageId]], src) as None)
        total = for
          m <- myValue
          o <- othersTotal
        yield m + o
        _ <- total.fold(ZIO.unit)(x => reply(ReadOk(x)))
      yield ()
  }
}
