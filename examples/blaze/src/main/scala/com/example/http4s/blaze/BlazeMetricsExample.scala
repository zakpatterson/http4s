package com.example.http4s.blaze

import java.util.concurrent.TimeUnit

import scalaz._, Scalaz._
import com.example.http4s.ExampleService
import org.http4s._
import org.http4s.dsl._
import org.http4s.prometheus
import org.http4s.server.{Router, ServerApp}
import org.http4s.server.blaze.BlazeBuilder

object BlazeMetricsExample extends ServerApp {
  val srvc = Router(
    "/metrics" -> prometheus.exportService(),
    "/http4s" -> ExampleService.service
  )

  def server(args: List[String]) = BlazeBuilder.bindHttp(8080)
    .withListener(prometheus.listener())
    .mountService(srvc)
    .start
}
