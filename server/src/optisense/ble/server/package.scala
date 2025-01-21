package optisense.ble

import cats.effect.Sync
import cats.syntax.all._
import net.sigusr.mqtt.api.ConnectionState
import net.sigusr.mqtt.api.ConnectionState.Connected
import net.sigusr.mqtt.api.ConnectionState.Connecting
import net.sigusr.mqtt.api.ConnectionState.Disconnected
import net.sigusr.mqtt.api.ConnectionState.Error
import net.sigusr.mqtt.api.ConnectionState.SessionStarted
import net.sigusr.mqtt.api.Errors.ConnectionFailure
import net.sigusr.mqtt.api.Errors.ProtocolError

package object server {
  val payload: String => Vector[Byte] = (_: String).getBytes("UTF-8").toVector

  def logSessionStatus[F[_]: Sync]: ConnectionState => F[ConnectionState] =
    s =>
      (s match {
        case Error(ConnectionFailure(reason)) =>
          putStrLn(s"${Console.RED}${reason.show}${Console.RESET}")
        case Error(ProtocolError) =>
          putStrLn(s"${Console.RED}Ṕrotocol error${Console.RESET}")
        case Disconnected =>
          putStrLn(s"${Console.BLUE}Transport disconnected${Console.RESET}")
        case Connecting(nextDelay, retriesSoFar) =>
          putStrLn(
            s"${Console.BLUE}Transport connecting. $retriesSoFar attempt(s) so far, next attempt in $nextDelay ${Console.RESET}"
          )
        case Connected =>
          putStrLn(s"${Console.BLUE}Transport connected${Console.RESET}")
        case SessionStarted =>
          putStrLn(s"${Console.BLUE}Session started${Console.RESET}")
      }) >> Sync[F].pure(s)

  def putStrLn[F[_]: Sync](s: String): F[Unit] = Sync[F].delay(println(s))

  def onSessionError[F[_]: Sync]: ConnectionState => F[Unit] = {
    case Error(e) => Sync[F].raiseError(e)
    case _        => Sync[F].pure(())
  }
}
