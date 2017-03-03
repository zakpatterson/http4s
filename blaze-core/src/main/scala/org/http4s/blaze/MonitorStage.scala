package org.http4s
package blaze

import java.nio.ByteBuffer
import java.util.concurrent.atomic.{AtomicBoolean, LongAdder}

import org.http4s.blaze.channel.BufferPipelineBuilder
import org.http4s.blaze.pipeline.MidStage
import org.http4s.blaze.util.Execution.directec

import scala.concurrent.Future

private[http4s] class MonitorStage(listener: HttpEvent => Unit)
    extends MidStage[ByteBuffer, ByteBuffer] {
  val name = "ServerStatusStage"

  private[this] val cleaned = new AtomicBoolean(false)
  private[this] var startNanos = 0L
  private[this] var bytesReceived = 0L
  private[this] var bytesSent = 0L

  private def onDisconnect() =
    if (!cleaned.getAndSet(true)) {
      listener(HttpEvent.Disconnect(System.nanoTime - startNanos, bytesReceived, bytesSent))
    }

  override def stageStartup(): Unit = {
    startNanos = System.nanoTime
    super.stageStartup()
  }

  override def stageShutdown(): Unit = {
    onDisconnect()
    super.stageShutdown()
  }

  def writeRequest(data: ByteBuffer): Future[Unit] = {
    bytesSent += data.remaining
    channelWrite(data)
  }

  override def writeRequest(data: Seq[ByteBuffer]): Future[Unit] = {
    bytesSent += data.foldLeft(0L)(_ + _.remaining)
    channelWrite(data)
  }

  def readRequest(size: Int): Future[ByteBuffer] =
    channelRead(size).map { b =>
      bytesReceived += b.remaining
      b
    }(directec)
}
