package gossipGlomers

import zio.*
import zio.json.*
import com.bilalfazlani.zioMaelstrom.*

object Main extends ZIOAppDefault {

  private def readSelf(me: NodeId) =
    SeqKv
      .read[Long](me, 300.millis)
      .catchSome { case ErrorMessage(_, ErrorCode.KeyDoesNotExist, _, _) =>
        ZIO.succeed(0L)
      }
      .tapError(e => logError(s"failed to read key $me from seq-kv. error: $e"))

  private def readOther(remote: NodeId) =
    for
      msgId <- MessageId.next
      value <- remote
        .ask[GetOk](Get(msgId), 500.millis)
        .map(_.value)
        .tapError(e => logWarn(s"an attempt to read from $remote failed with error: $e"))
        .retryN(2)
        .tapError(e => logError(s"failed to read from $remote. error: $e"))
    yield value

  private def add(delta: Long, source: NodeId, me: NodeId, msg_id: MessageId): ZIO[MaelstromRuntime, AskError, Unit] =
    for
      value <- readSelf(me)
      _ <- SeqKv
        .cas(me, value, value + delta, true, 300.millis)
        .catchSome { case ErrorMessage(_, ErrorCode.PreconditionFailed, _, _) =>
          logDebug(s"an attempt to add key $me failed due to precondition failure. will attempt again")
            *> add(delta, source, me, msg_id)
        }
        .tapError(e => logWarn(s"an attempt to add key $me failed with error: $e"))
      _ <- source send AddOk(msg_id)
    yield ()

  private def replyError(askError: AskError, msg_id: MessageId, remote: NodeId) =
    askError match
      case t: Timeout      => remote send ErrorMessage(msg_id, ErrorCode.Timeout, "downstream request timed out")
      case e: ErrorMessage => remote send e.copy(in_reply_to = msg_id)
      case d: DecodingFailure =>
        remote send ErrorMessage(msg_id, ErrorCode.NotSupported, "functionality seems to be broken")

  val handler = receive[InputMessage] {

    case Add(delta, msg_id) =>
      add(delta, src, me, msg_id)
        .retryN(2)
        .tapError(e => logError(s"could not add key $me all attempts exhausted. error: $e"))
        .catchAll(replyError(_, msg_id, src))

    case Get(msg_id, _) =>
      for
        response <- readSelf(me)
          .retryN(2)
          .tapError(e => logError(s"could not read key $me all attempts exhausted. error: $e"))
          .map(Some(_))
          .catchAll(replyError(_, msg_id, src) as None)
        _ <- reply(GetOk(msg_id, response.get))
      yield ()

    case Read(msg_id) =>
      for
        myValueFiber <- readSelf(me)
          .retryN(2)
          .map(Some(_))
          .forkScoped
        othersTotal <-
          ZIO
            .mergeAllPar(others.map(readOther))(0L)(_ + _)
            .map(Some(_))
            .catchAll(replyError(_, msg_id, src) as None)
        myValue <- myValueFiber.join.catchAll(replyError(_, msg_id, src) as None)
        total = for
          m <- myValue
          o <- othersTotal
        yield m + o
        _ <- total.fold(ZIO.unit)(x => reply(ReadOk(msg_id, x)))
      yield ()
  }

  def run = handler.provideSome[Scope](
    MaelstromRuntime.live(Settings(logLevel = NodeLogLevel.Debug))
  )
}
