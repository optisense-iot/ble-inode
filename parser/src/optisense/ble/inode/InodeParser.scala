package optisense.ble.enode

import scodec._
import scodec.bits._
import scodec.codecs._

case class BleData(uuid: String, temperature: Double, humidity: Double)

object BleCodec {
  val uuidCodec: Codec[String] =
    bytes(16).xmap(_.toHex, ByteVector.fromValidHex(_))

  // temperature and humidity codec (16 bits each)
  val telemetryCodec: Codec[(Double, Double)] =
    (uint16 :: uint16).xmap(
      { case (temp, humidity) => (temp / 10.0, humidity / 10.0) },
      { case (temp, humidity) => (temp.toInt * 10, humidity.toInt * 10) },
    )

  val bleCodec: Codec[BleData] =
    (uuidCodec :: telemetryCodec).as[BleData]
}
