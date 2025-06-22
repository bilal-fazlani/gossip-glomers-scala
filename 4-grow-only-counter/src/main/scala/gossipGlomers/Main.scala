package gossipGlomers

import zio.*
import zio.json.*
import com.bilalfazlani.zioMaelstrom.*
import com.bilalfazlani.zioMaelstrom.services.SeqKv
import zio.ZIO.{logError, logWarning}

object Main extends MaelstromNode {

  override val configure: NodeConfig = NodeConfig.withLogLevelDebug

  private def readDatabase = {
    for
      me <- MaelstromRuntime.me
      count <- SeqKv
        .read[Long](me, 250.millis)
        .catchSome { case Error(ErrorCode.KeyDoesNotExist, _) =>
          ZIO.succeed(0L)
        }
        .tapError(e => logError(s"failed to read key $me from seq-kv. error: $e"))
    yield count
  }

  private def fetchFromNode(remote: NodeId): ZIO[MessageSender, AskError, Long] =
    remote
      .ask[GetOk](Get(), 300.millis)
      .map(_.value)
      .tapError(e => logWarning(s"an attempt to read from $remote failed with error: $e"))
      .retryN(5)
      .tapError(e => logError(s"failed to read from $remote. error: $e"))

  private def makeErrorReply(askError: AskError) = askError match
    case Timeout(downstreamRequestId: MessageId, downstreamAddress: NodeId, _) =>
      Error(
        ErrorCode.Timeout,
        s"timed out while waiting for a response from $downstreamAddress for messageId $downstreamRequestId"
      )
    case DecodingFailure(_, _) => Error(ErrorCode.NotSupported, "functionality seems to be broken")
    case _                     => Error(ErrorCode.NotSupported, "functionality seems to be broken")

  // noinspection TypeAnnotation
  val program = receive[InputMessage] {
    case Add(delta) =>
      SeqKv
        .update[NodeId, Long](me, 300.millis) {
          case Some(currentValue) => currentValue + delta
          case None               => delta
        }
        .tapError(e => logWarning(s"an attempt to add key $me failed with error: $e"))
        .retryN(5)
        .tapError(e => logError(s"failed to add key $me all attempts exhausted. error: $e") *> reply(makeErrorReply(e)))
        .zipRight(reply(AddOk()))
        .ignore

    case Get() =>
      readDatabase
        .foldZIO(
          error => logError(s"failed to read key $me from database. error: $error") *> reply(makeErrorReply(error)),
          count => reply(GetOk(count))
        )

    case Read() =>
      val total = for
        othersTotalFiber <- ZIO.mergeAllPar(others.map(fetchFromNode))(0L)(_ + _).forkScoped
        myValue <- readDatabase.retryN(3)
        othersTotal <- othersTotalFiber.join
      yield myValue + othersTotal
      total.foldZIO(
        error => reply(makeErrorReply(error)),
        count => reply(ReadOk(count))
      )
  }
}
