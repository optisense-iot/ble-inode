package optisense.ble.server

import cats.effect.kernel.Clock
import cats.effect.kernel.Resource
import cats.effect.IO
import sttp.client4._
import sttp.client4.httpclient.cats.HttpClientCatsBackend
import sttp.model.Uri

import java.time.Instant

class InfluxDBClient(backend: WebSocketBackend[IO], influxDBToken: String, targetHost: Uri) {
  val influxDBUrl   = uri"$targetHost/api/v2/write?org=my-org&bucket=sensors&precision=s"

  def sendData(sensorData: FlatSensorData): IO[Response[Either[String, String]]] = {
    val dataF = Clock[IO].realTimeInstant.map(_.getEpochSecond()).map { timestamp =>
      // InfluxDB Line Protocol
      Seq(
        Some(
          s"""sensor_data,type=${sensorData.`type`},mac_address=${sensorData.macAddress} temperature=${sensorData.temperature} $timestamp"""
        ),
        sensorData.humidity.map(h =>
          s"sensor_data,type=${sensorData.`type`},mac_address=${sensorData.macAddress} humidity=$h $timestamp"
        ),
        sensorData.pressure.map(p =>
          s"sensor_data,type=${sensorData.`type`},mac_address=${sensorData.macAddress} pressure=$p $timestamp"
        ),
        sensorData.positionMotion.map(m =>
          s"sensor_data,type=${sensorData.`type`},mac_address=${sensorData.macAddress} position_motion=${if (m) 1 else 0} $timestamp"
        ),
        sensorData.positionAccX.map(x =>
          s"sensor_data,type=${sensorData.`type`},mac_address=${sensorData.macAddress} position_acc_x=$x $timestamp"
        ),
        sensorData.positionAccY.map(y =>
          s"sensor_data,type=${sensorData.`type`},mac_address=${sensorData.macAddress} position_acc_y=$y $timestamp"
        ),
        sensorData.positionAccZ.map(z =>
          s"sensor_data,type=${sensorData.`type`},mac_address=${sensorData.macAddress} position_acc_z=$z $timestamp"
        ),
        Some(
          s"sensor_data,type=${sensorData.`type`},mac_address=${sensorData.macAddress} battery_info_level=${sensorData.batteryInfoLevel} $timestamp"
        ),
        Some(
          s"sensor_data,type=${sensorData.`type`},mac_address=${sensorData.macAddress} battery_info_voltage=${sensorData.batteryInfoVoltage} $timestamp"
        ),
        Some(s"sensor_data,type=${sensorData.`type`},mac_address=${sensorData.macAddress} rtto=${if (sensorData.rtto) 1
          else 0} $timestamp"),
        Some(
          s"sensor_data,type=${sensorData.`type`},mac_address=${sensorData.macAddress} low_battery=${if (sensorData.lowBattery) 1
            else 0} $timestamp"
        ),
      ).flatten.mkString("\n")
    }

    dataF.flatMap { data =>
      val request = basicRequest
        .post(influxDBUrl)
        .header("Authorization", s"Token $influxDBToken")
        .header("Content-Type", "text/plain; charset=utf-8")
        .body(data)

      IO.println(s"Sent to InfluxDB: $data") *>
        request
          .send(backend)
    }
  }
}

object InfluxDBClient {
  def make(token: String, targetHost: Uri): Resource[IO, InfluxDBClient] =
    HttpClientCatsBackend
      .resource[IO]()
      .map(new InfluxDBClient(_, token, targetHost))
}
