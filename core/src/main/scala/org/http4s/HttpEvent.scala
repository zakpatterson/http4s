package org.http4s

sealed abstract class HttpEvent extends Product with Serializable

object HttpEvent {
  case object Connect extends HttpEvent
  case class Disconnect(durationNanos: Long, bytesSent: Long, byteReceived: Long) extends HttpEvent
  case class RequestComplete(req: Request, resp: Response, durationNanos: Long) extends HttpEvent
  case class Error(t: Throwable) extends HttpEvent
}
