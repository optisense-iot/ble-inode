package optisense.ble.server

import cats.data.NonEmptyList
import cats.effect.std.Console
import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import cats.implicits._
import com.comcast.ip4s.Host
import com.comcast.ip4s.Port
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.monovore.decline.effect.CommandIOApp
import com.monovore.decline.Opts
import fs2.concurrent.SignallingRef
import fs2.Stream
import net.sigusr.mqtt.api._
import net.sigusr.mqtt.api.QualityOfService.AtLeastOnce
import net.sigusr.mqtt.api.QualityOfService.AtMostOnce
import net.sigusr.mqtt.api.QualityOfService.ExactlyOnce
import net.sigusr.mqtt.api.RetryConfig.Custom
import optisense.ble.inode.INodeParser
import optisense.ble.server.Json.given
import retry.RetryPolicies
import scodec.bits.BitVector
import sttp.client4.*
import sttp.client4.httpclient.cats.HttpClientCatsBackend
import sttp.client4.jsoniter.*
import sttp.model.Uri

import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.given
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration.SECONDS

object ServerApp
    extends CommandIOApp(
      name = "ble-parse",
      header = "Parser for bluetooth data bundled with mqtt",
      version = "0.1.0",
    ) {

  private val inputTopic = Opts.option[String]("from", "Input topic with raw ble data")
  private val targetHost =
    Opts
      .option[String]("targetHost", "Address of the influxDB instance that the data will be send to")
      .mapValidated(s => Uri.parse(s).toValidatedNel)
  private val influxDbToken = Opts.option[String]("token", "InfluxDB api token")
  private val host          = Opts.option[String]("host", "Host of the mqtt broker").withDefault("localhost")
  private val port          = Opts.option[String]("port", "Port of the mqtt broker").withDefault("1883")
  private val trace         = Opts.flag("trace", help = "Show trace logs").orFalse
  private val debug         = Opts.flag("debug", help = "Show debug logs").orFalse
  private val macWhitelist  = Opts.options[String]("mac", "Allowed mac address")

  case class Config(
      inputTopic: String,
      targetHost: Uri,
      host: String,
      port: String,
      trace: Boolean,
      debug: Boolean,
      token: String,
      macWhitelist: NonEmptyList[String],
  )
  private val readerSessionConfig =
    SessionConfig(
      "optisense-subscriber",
      cleanSession = false,
      user = None,
      password = None,
    )
  private def writerSessionConfig(user: String) = SessionConfig(
    "optisense-publisher",
    cleanSession = false,
    user = user.some,
    password = None,
  )

  override def main: Opts[IO[ExitCode]] =
    (inputTopic, targetHost, host, port, trace, debug, influxDbToken, macWhitelist)
      .mapN(Config.apply)
      .map { config =>
        val transportConfig =
          TransportConfig[IO](
            Host.fromString(config.host).getOrElse(sys.error(s"Incorrect host value: ${config.host}")),
            Port.fromString(config.port).getOrElse(sys.error(s"Incorrect port value: ${config.port}")),
            traceMessages = config.trace,
          )

        implicit val console: Console[IO] = Console.make[IO]

        InfluxDBClient
          .make(config.token, config.targetHost)
          .use { influxDbClient =>
            Session[IO](transportConfig, readerSessionConfig)
              .use { readerSession =>
                val processMessages = readerSession.messages
                  .flatTap(logDebugMessage(config))
                  .through(MessageProccessing.processMessages)
                  .flatMap { msg =>
                    if (config.macWhitelist.contains_(msg.macAddress)) {
                      Stream.eval(IO.pure(msg))
                    } else {
                      Stream.eval(IO.println(s"Dropping message from: ${msg.macAddress}")) *> Stream.empty
                    }
                  }
                  .evalMap { msg =>
                    IO.println(s"Sending data to ${config.targetHost} for ${msg.macAddress}") *>
                      influxDbClient
                        .sendData(msg)
                        .attempt
                        .flatMap(resp => IO.println(resp))
                  }
                  .compile
                  .drain

                for {
                  subscribedTopics <- readerSession.subscribe(Vector((config.inputTopic, AtMostOnce)))
                  _ <- subscribedTopics.traverse { case (topic, qos) =>
                    putStrLn[IO](
                      s"Topic ${scala.Console.CYAN}${topic}${scala.Console.RESET} subscribed with QoS " +
                        s"${scala.Console.CYAN}${qos.show}${scala.Console.RESET}"
                    )
                  }
                  _ <- processMessages
                } yield ExitCode.Success
              }
              .handleErrorWith { err =>
                err.printStackTrace()
                IO.pure(ExitCode.Error)
              }
          }
      }

  private def logDebugMessage(config: Config): Message => Stream[IO, Unit] = { case Message(topic, payload) =>
    if (config.debug) {
      Stream.eval(
        putStrLn[IO](
          s"Topic ${scala.Console.CYAN}$topic${scala.Console.RESET}:\n" +
            BitVector(payload.toArray).toHexDumpColorized
        )
      )
    } else {
      Stream.unit
    }
  }
}
