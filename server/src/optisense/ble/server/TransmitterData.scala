package optisense.ble.server

import io.circe.Decoder

enum TransmitterData {
  case RawDataItem(
      timestamp: String,
      mac: String,
      rssi: Int,
      rawData: String,
      rawResp: Option[String],
  )
}

object TransmitterData {

  given Decoder[TransmitterData.RawDataItem] = io.circe.generic.semiauto.deriveDecoder
  given Decoder[TransmitterData]             = io.circe.generic.semiauto.deriveDecoder

}
