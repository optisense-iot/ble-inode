package optisense.ble.server

import cats.effect.std.Console
import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import cats.implicits._
import com.comcast.ip4s.Host
import com.comcast.ip4s.Port
import com.monovore.decline.effect.CommandIOApp
import com.monovore.decline.Opts
import fs2.concurrent.SignallingRef
import fs2.Stream
import net.sigusr.mqtt.api._
import net.sigusr.mqtt.api.QualityOfService.AtLeastOnce
import net.sigusr.mqtt.api.QualityOfService.AtMostOnce
import net.sigusr.mqtt.api.QualityOfService.ExactlyOnce
import net.sigusr.mqtt.api.RetryConfig.Custom
import retry.RetryPolicies

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration.SECONDS

object ServerApp
    extends CommandIOApp(
      name = "ble-parse",
      header = "Parser for bluetooth data bundled with mqtt",
      version = "0.1.0",
    ) {

  private val input    = Opts.option[String]("from", "Input topic with raw ble data")
  private val output   = Opts.option[String]("to", "Output that processed data will be send")
  private val user     = Opts.option[String]("user", "username").orNone
  private val password = Opts.option[String]("password", "password").orNone
  private val host     = Opts.option[String]("host", "host").withDefault("localhost")
  private val port     = Opts.option[String]("port", "port").withDefault("1883")
  private val trace    = Opts.flag("trace", help = "Show trace logs").orFalse

  case class Config(
      input: String,
      output: String,
      user: Option[String],
      password: Option[String],
      host: String,
      port: String,
      trace: Boolean,
  )

  override def main: Opts[IO[ExitCode]] =
    (input, output, user, password, host, port, trace)
      .mapN(Config.apply)
      .map { config =>
        val retryConfig: Custom[IO] = Custom[IO](
          RetryPolicies
            .limitRetries[IO](5)
            .join(RetryPolicies.fullJitter[IO](FiniteDuration(2, SECONDS)))
        )
        val transportConfig =
          TransportConfig[IO](
            Host.fromString(config.host).getOrElse(sys.error(s"Incorrect host value: ${config.host}")),
            Port.fromString(config.port).getOrElse(sys.error(s"Incorrect port value: ${config.port}")),
            // TLS support looks like
            // 8883,
            // tlsConfig = Some(TLSConfig(TLSContextKind.System)),
            retryConfig = retryConfig,
            traceMessages = config.trace,
          )
        val sessionConfig =
          SessionConfig(
            "optisense-subscriber",
            cleanSession = false,
            user = config.user,
            password = config.password,
            keepAlive = 5,
          )
        implicit val console: Console[IO] = Console.make[IO]
        Session[IO](transportConfig, sessionConfig)
          .use { session =>
            val processMessages = session.messages
              .flatTap(logMessage())
              .evalMap(msg => session.publish(config.output, msg.payload, AtMostOnce))
              .compile
              .drain
            for {
              s <- session.subscribe(Vector((config.input, AtMostOnce)))
              _ <- s.traverse { p =>
                putStrLn[IO](
                  s"Topic ${scala.Console.CYAN}${p._1}${scala.Console.RESET} subscribed with QoS " +
                    s"${scala.Console.CYAN}${p._2.show}${scala.Console.RESET}"
                )
              }
              _ <- processMessages
            } yield ExitCode.Success
          }
          .handleErrorWith(_ => IO.pure(ExitCode.Error))
      }

  private def logMessage(): Message => Stream[IO, Unit] = { case Message(topic, payload) =>
    Stream.eval(
      putStrLn[IO](
        s"Topic ${scala.Console.CYAN}$topic${scala.Console.RESET}: " +
          s"${scala.Console.BOLD}${new String(payload.toArray, "UTF-8")}${scala.Console.RESET}"
      )
    )
  }
}
