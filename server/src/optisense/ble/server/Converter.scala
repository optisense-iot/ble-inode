package optisense.ble.server

import io.circe._
import net.sigusr.mqtt.api.Message

import java.nio.charset.StandardCharsets

class ConversionFailure(msg: String, rootCause: Throwable) extends RuntimeException(msg, rootCause) {
  def this(msg: String) = this(msg, null)
}

object Converter {

  def apply(msg: Message): Either[ConversionFailure, Vector[Json]] = {
    import io.circe.parser._

    def parseJson(payload: Vector[Byte]) =
      parse(new String(payload.toArray, StandardCharsets.UTF_8)).left.map(parsingFailure =>
        ConversionFailure(parsingFailure.message, parsingFailure)
      )

    def extractData(json: Json) =
      json.asObject
        .flatMap(_.toMap.get("data"))
        .flatMap(_.asArray)
        .toRight(ConversionFailure("Missing data field or it was not an array"))

    for {
      json <- parseJson(msg.payload)
      data <- extractData(json)
    } yield data
  }

}
