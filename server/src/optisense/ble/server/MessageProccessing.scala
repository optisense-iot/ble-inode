package optisense.ble.server

import cats.effect.IO
import cats.syntax.all._
import fs2.Stream
import net.sigusr.mqtt.api.Message
import optisense.ble.inode.INodeParser
import optisense.ble.server.ServerApp.Config
import scodec.bits.BitVector

case class DeviceData[T](data: T, macAddress: String)

object MessageProccessing {
  def processMessages: Stream[IO, Message] => Stream[IO, FlatSensorData] =
    _.evalMap(msg => IO.fromEither(Converter(msg)))
      .flatMap(Stream.emits)
      .flatMap { json =>
        json.as[TransmitterData.RawDataItem] match {
          case Left(_) => Stream.eval(putStrLn[IO]("Not an data item, skipping...")) *> Stream.empty
          case Right(td) =>
            Stream.eval(putStrLn[IO](s"Data item detected: $td")) *>
              Stream.eval(IO(DeviceData(BitVector.fromValidHex(td.rawData), td.mac)))
        }
      }
      .flatMap { deviceData =>
        Stream
          .evalSeq(IO.fromTry(BleFrame.framesCodec.decodeValue(deviceData.data).toTry))
          .map(bleFrame => DeviceData(bleFrame, deviceData.macAddress))
      }
      .evalMapFilter {
        case DeviceData(BleFrame.Flags(bytes), macAddress) =>
          putStrLn[IO](s"Flags frame from $macAddress, skipping...").as(none)
        case DeviceData(BleFrame.ManufactureData(bytes), macAddress) =>
          putStrLn[IO](s"ManufactureData detected from $macAddress. Decoding...") *> IO
            .fromTry(INodeParser.codec.decodeValue(bytes).toTry)
            .map(inode => DeviceData(inode, macAddress).some)
        case DeviceData(BleFrame.Unknown(bytes), macAddress) =>
          putStrLn[IO](s"Unknown BLE frame from $macAddress, skipping...").as(none)
      }
      .map(FlatSensorData.from)
}
