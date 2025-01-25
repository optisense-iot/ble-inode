package optisense.ble.server

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
      .option[String]("targetHost", "Address of the thingsBoard instance that the data will be send to")
      .mapValidated(s => Uri.parse(s).toValidatedNel)
  private val host  = Opts.option[String]("host", "host").withDefault("localhost")
  private val port  = Opts.option[String]("port", "port").withDefault("1883")
  private val trace = Opts.flag("trace", help = "Show trace logs").orFalse
  private val debug = Opts.flag("debug", help = "Show debug logs").orFalse

  case class Config(
      inputTopic: String,
      targetHost: Uri,
      host: String,
      port: String,
      trace: Boolean,
      debug: Boolean,
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
    (inputTopic, targetHost, host, port, trace, debug)
      .mapN(Config.apply)
      .map { config =>
        val transportConfig =
          TransportConfig[IO](
            Host.fromString(config.host).getOrElse(sys.error(s"Incorrect host value: ${config.host}")),
            Port.fromString(config.port).getOrElse(sys.error(s"Incorrect port value: ${config.port}")),
            traceMessages = config.trace,
          )

        implicit val console: Console[IO] = Console.make[IO]

        HttpClientCatsBackend.resource[IO]().use { httpBackend =>
          Session[IO](transportConfig, readerSessionConfig)
            .use { readerSession =>
              val processMessages = readerSession.messages
                .flatTap(logDebugMessage(config))
                .through(MessageProccessing.processMessages)
                .evalMap { msg =>
                  val request = basicRequest
                    .post(uri"${config.targetHost}/api/v1/${msg.macAddress}/telemetry")
                    .body(asJson(msg))

                  request
                    .send(httpBackend)
                    .flatMap(resp => IO.println(request.toCurl) *> IO.println(resp))
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
