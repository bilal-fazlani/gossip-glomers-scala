package gossipGlomers

import zio.json.JsonCodec
import zio.json.JsonFieldEncoder

case class AllData(data: Map[Key, KeyData]):
  def addMsg(key: Key, msg: Value): AllData = AllData(data.updatedWith(key) {
    case None    => Some(KeyData.Uncommitted(List(msg)))
    case Some(v) => Some(v.addMsg(msg))
  })

  def commit(offsets: Map[Key, Offset]): AllData = AllData(data.foldRight[Map[Key, KeyData]](Map.empty) {
    case ((key, keyData), acc) if offsets.contains(key) =>
      acc.updated(key, keyData.commit(offsets(key)))
    case ((key, keyData), acc) =>
      acc + ((key, keyData))
  })

  def poll(request: Map[Key, Offset]): Map[Key, List[OffsetValue]] =
    request.foldRight[Map[Key, List[OffsetValue]]](Map.empty) { case ((key, offset), acc) =>
      data.get(key).fold(acc)(keyData => acc + ((key, keyData.poll(offset))))
    }

  def keyDataFor(key: Key): Option[KeyData] = data.get(key)

  def getCommittedOffsets(keys: Set[Key]): Map[Key, Offset] = data.collect {
    case (key, KeyData.Committed(watermark, values)) if keys.contains(key) =>
      (key, watermark)
  }

object AllData:
  val empty: AllData = AllData(Map.empty)

enum KeyData(values: List[Value]):
  case Uncommitted(values: List[Value])                  extends KeyData(values)
  case Committed(watermark: Offset, values: List[Value]) extends KeyData(values)

  def addMsg(value: Value): KeyData = this match {
    case x: KeyData.Uncommitted => x.copy(values = values :+ value)
    case x: KeyData.Committed   => x.copy(values = values :+ value)
  }

  def commit(offset: Offset): KeyData = KeyData.Committed(offset, values)

  val offset: Offset = Offset(values.length - 1)

  def poll(offset: Offset): List[OffsetValue] =
    values.zipWithIndex
      .dropWhile(x => x._2 != offset.number)
      .map((value, offset) => OffsetValue(Offset(offset), value))

opaque type Key = String
object Key:
  def apply(str: String): Key = str
  given JsonCodec[Key]        = JsonCodec[String]
  given JsonFieldEncoder[Key] = JsonFieldEncoder[String]

opaque type Value = Int
object Value:
  def apply(number: Int): Value = number
  given JsonCodec[Value]        = JsonCodec[Int]

opaque type Offset = Int
object Offset:
  def apply(number: Int): Offset = number
  given JsonCodec[Offset]        = JsonCodec[Int]

extension (offset: Offset) def number: Int = offset

type OffsetValue = List[Int]
object OffsetValue:
  def apply(offset: Offset, value: Value): OffsetValue = List(offset, value)
