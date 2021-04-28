package zio.prometheus

import zio.prometheus.metrics._
import zio._

object Example {

  case class LabelName[A](name: String)
  case class Label[A](value: A)

  object exampleCounter extends Counter("example_gauge", "Shows how many requests are in.", Chunk("myService")) {
    val label: LabelName[String] = LabelName("component")
  }
  object currentRequest extends Gauge("example_gauge", "Shows how many requests are in.", Chunk("myService"))
  object requestLatency extends Summary("requests_latency_seconds", "Request latency in seconds.", Chunk("myService"))

  type Metrics = exampleCounter.Metric with currentRequest.Metric with requestLatency.Metric

  trait Service {
    def doSomethingImportant: UIO[Unit]
  }

  //Metrics.register(exampleCounter, currentRequest, requestLatency): Metric[(Counter, Gauge, Summary)]


  val x = exampleCounter.inc

  def live = (exampleCounter.register ++ currentRequest.register ++ requestLatency.register) >>>
    ZLayer.fromFunction[Metrics, Service] { env =>
      val rl = env.get[Summary.Registered[requestLatency.type]]
      val c = exampleCounter.fromEnv(env)
      val g = currentRequest.fromEnv(env)
      new Service {
        override def doSomethingImportant: UIO[Unit] =
          c.inc *>
            metrics.inc(g) *>
            metrics.timer(rl)(ZIO.unit)
      }

    }

}
