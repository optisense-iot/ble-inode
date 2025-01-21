package optisense.ble.server

import cats.effect.std.Console
import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import cats.implicits._
import com.comcast.ip4s.Host
import com.comcast.ip4s.Port
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

object ServerApp extends IOApp {
  val localSubscriber: String = "Local-Subscriber"
  private val subscribedTopics: Vector[(String, QualityOfService)] = Vector(
    ("AtMostOnce", AtMostOnce),
    ("AtLeastOnce", AtLeastOnce),
  )

  private val unsubscribedTopics: Vector[String] = Vector("AtMostOnce", "AtLeastOnce", "ExactlyOnce")

  override def run(args: List[String]): IO[ExitCode] = {
    val retryConfig: Custom[IO] = Custom[IO](
      RetryPolicies
        .limitRetries[IO](5)
        .join(RetryPolicies.fullJitter[IO](FiniteDuration(2, SECONDS)))
    )
    val transportConfig =
      TransportConfig[IO](
        Host.fromString("localhost").get,
        Port.fromString("1883").get,
        // TLS support looks like
        // 8883,
        // tlsConfig = Some(TLSConfig(TLSContextKind.System)),
        retryConfig = retryConfig,
        traceMessages = true,
      )
    val sessionConfig =
      SessionConfig(
        s"$localSubscriber",
        cleanSession = false,
        user = Some(localSubscriber),
        password = Some("yolo"),
        keepAlive = 5,
      )
    implicit val console: Console[IO] = Console.make[IO]
    Session[IO](transportConfig, sessionConfig)
      .use { session =>
        val reader = session.messages.flatMap(processMessages()).compile.drain
        for {
          s <- session.subscribe(subscribedTopics)
          _ <- s.traverse { p =>
            putStrLn[IO](
              s"Topic ${scala.Console.CYAN}${p._1}${scala.Console.RESET} subscribed with QoS " +
                s"${scala.Console.CYAN}${p._2.show}${scala.Console.RESET}"
            )
          }
          _ <- reader
        } yield ExitCode.Success
      }
  }
    .handleErrorWith(_ => IO.pure(ExitCode.Error))

  private def processMessages(): Message => Stream[IO, Unit] = { case Message(topic, payload) =>
    Stream.eval(
      putStrLn[IO](
        s"Topic ${scala.Console.CYAN}$topic${scala.Console.RESET}: " +
          s"${scala.Console.BOLD}${new String(payload.toArray, "UTF-8")}${scala.Console.RESET}"
      )
    )
  }
}
