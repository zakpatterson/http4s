package org.http4s

import scalaz.concurrent.{Strategy, Task}
import io.prometheus.client.{CollectorRegistry, Counter, Gauge, Histogram}
import io.prometheus.client.exporter.common.TextFormat
import org.http4s.syntax.taskResponse._
import org.http4s.util.io.captureWriter

package object prometheus {
  def listener(registry: CollectorRegistry = CollectorRegistry.defaultRegistry): HttpEvent => Unit = new (HttpEvent => Unit) {
    val activeConnections: Gauge = (new Gauge.Builder)
      .name("http4s_connections_active")
      .help("Number of currently active connections")
      .register(registry)

    val connections: Histogram = (new Histogram.Builder)
      .name("http4s_connections_seconds")
      .help("Duration of connections in seconds")
      .register(registry)

    val requestLatency: Histogram = new (Histogram.Builder)
      .name("http4s_request_latency_seconds")
      .help("Request latency in seconds")
      .labelNames("method")
      .register(registry)

    val responses: Counter = new (Counter.Builder)
      .name("http4s_responses_total")
      .help("Responses received")
      .labelNames("status")
      .register(registry)

    val bytesReceived: Counter = (new Counter.Builder)
      .name("http4s_bytes_received")
      .help("Bytes received")
      .register(registry)

    val bytesSent: Counter = (new Counter.Builder)
      .name("http4s_bytes_sent")
      .help("Bytes sent")
      .register(registry)

    def apply(event: HttpEvent): Unit = event match {
      case HttpEvent.Connect =>
        println("EVENT: "+event)
        activeConnections.inc()

      case HttpEvent.Disconnect(duration, received, sent) =>
        println("EVENT: "+event)
        activeConnections.dec()
        this.bytesReceived.inc(received.toDouble)
        this.bytesSent.inc(sent.toDouble)
        connections.observe(duration * 1e-9)

      case HttpEvent.RequestComplete(req, resp, duration) =>
        println("EVENT: "+event)
        requestLatency.labels(req.method.renderString).observe(duration * 1e-9)
        responses.labels(resp.status.code / 100 + "xx").inc()

      case _ =>
        println("EVENT: "+event)
    }
  }

  def exportResponse(registry: CollectorRegistry = CollectorRegistry.defaultRegistry)(implicit S: Strategy): Task[Response] = {
    Response(Status.Ok)
      .withBody(captureWriter { w =>
        TextFormat.write004(w, registry.metricFamilySamples)
      })
      .putHeaders(Header("Content-Type", TextFormat.CONTENT_TYPE_004))
  }

  def exportService(registry: CollectorRegistry = CollectorRegistry.defaultRegistry)(implicit S: Strategy): HttpService =
    HttpService {
      case req if req.method == Method.GET && req.pathInfo == "/" =>
        exportResponse(registry)
    }
}
