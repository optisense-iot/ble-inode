package optisense.ble.server

import io.circe.Decoder

case class AdvertisingFrame(
    timestamp: String,
    mac: String,
    rssi: Int,
    rawData: String,
    rawResp: Option[String],
)

object AdvertisingFrame {
}
