package optisense.ble.server

import cats.effect.IO
import cats.syntax.all._
import fs2.Stream
import net.sigusr.mqtt.api.Message
import optisense.ble.inode.INodeParser
import optisense.ble.server.ServerApp.Config
import scodec.bits.BitVector

object MessageProccessing {
  def processMessages: Stream[IO, Message] => Stream[IO, FlatSensorData] =
    _.evalMap(msg => IO.fromEither(Converter(msg)))
      .flatMap(Stream.emits)
      .flatMap { json =>
        json.as[TransmitterData.RawDataItem] match {
          case Left(_) => Stream.eval(putStrLn[IO]("Not an data item, skipping...")) *> Stream.empty
          case Right(td) =>
            Stream.eval(putStrLn[IO](s"Data item detected: $td")) *>
              Stream.eval(IO(BitVector.fromValidHex(td.rawData)))
        }
      }
      .flatMap(rawData => Stream.evalSeq(IO.fromTry(BleFrame.framesCodec.decodeValue(rawData).toTry)))
      .evalMapFilter {
        case BleFrame.Flags(bytes) =>
          putStrLn[IO]("Flags frame, skipping...").as(none)
        case BleFrame.ManufactureData(bytes) =>
          putStrLn[IO]("ManufactureData detected. Decoding...") *> IO
            .fromTry(INodeParser.codec.decodeValue(bytes).toTry)
            .map(_.some)
        case BleFrame.Unknown(bytes) =>
          putStrLn[IO]("Unknown BLE frame, skipping...").as(none)
      }
      .map(FlatSensorData.from)
}
