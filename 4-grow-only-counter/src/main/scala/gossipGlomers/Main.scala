package gossipGlomers

import zio.*
import zio.json.*
import com.bilalfazlani.zioMaelstrom.*
import com.bilalfazlani.zioMaelstrom.services.SeqKv
import zio.ZIO.{logError, logWarning}

object Main extends MaelstromNode {

  override val configure: NodeConfig = NodeConfig.withLogLevelDebug

  private type Cache = Ref[Map[NodeId, Long]]

  private def updateCache(nodeId: NodeId, value: Long) =
    for
      cache <- ZIO.service[Cache]
      _     <- cache.update(currentCache => currentCache.updated(nodeId, value))
    yield ()

  private def getCache(nodeId: NodeId) =
    ZIO
      .serviceWithZIO[Cache](_.get.map(_.getOrElse(nodeId, 0L)))
      .tap(value => ZIO.logDebug(s"fetched $nodeId from cache with value $value"))

  private def readDatabase = for
    me <- MaelstromRuntime.me
    count <- SeqKv
      .read[Long](me, 30.millis)
      .catchSome { case Error(ErrorCode.KeyDoesNotExist, _) =>
        ZIO.succeed(0L)
      }
      .tapBoth(e => logError(s"failed to read key $me from seq-kv. error: $e"), updateCache(me, _))
      .orElse(getCache(me))
  yield count

  private def fetchFromNode(remote: NodeId): ZIO[Cache & MessageSender, Nothing, Long] =
    remote
      .ask[GetOk](Get(), 30.millis)
      .map(_.value)
      .tapBoth(
        e => logWarning(s"failed to read from $remote. error: $e"),
        value => updateCache(remote, value)
      )
      .orElse(getCache(remote))

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
      MaelstromRuntime.me.flatMap(me =>
        SeqKv
          .update[NodeId, Long](me, 300.millis) {
            case Some(currentValue) => currentValue + delta
            case None               => delta
          }
          .tapError(e => logWarning(s"an attempt to add key $me failed with error: $e"))
          .retryN(5)
          .tapError(e =>
            logError(s"failed to add key $me all attempts exhausted. error: $e") *> reply(makeErrorReply(e))
          )
          .zipRight(reply(AddOk()))
          .ignore
      )

    case Get() => readDatabase.flatMap(x => reply(GetOk(x)))

    case Read() =>
      val total = for
        others           <- MaelstromRuntime.others
        othersTotalFiber <- ZIO.mergeAllPar(others.map(fetchFromNode))(0L)(_ + _).forkScoped
        myValue          <- readDatabase
        othersTotal      <- othersTotalFiber.join
      yield myValue + othersTotal
      total.flatMap(count => reply(ReadOk(count)))
  }.provideSome[MaelstromRuntime](ZLayer(Ref.make(Map.empty[NodeId, Long])))
}
