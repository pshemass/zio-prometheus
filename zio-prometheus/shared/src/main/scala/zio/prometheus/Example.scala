package zio.prometheus

import zio.prometheus.metrics._
import zio._

object Example {

  object exampleCounter extends Counter("example_gauge", "Shows how many requests are in.", Labels("component", "method"))
  object currentRequest extends Gauge("example_gauge", "Shows how many requests are in.", Labels("component"))
  object requestLatency extends Summary("requests_latency_seconds", "Request latency in seconds.", Labels.Empty)

  type Metrics = exampleCounter.Metric with currentRequest.Metric with requestLatency.Metric

  trait Service {
    def doSomethingImportant: UIO[Unit]
  }


  val x = exampleCounter.inc(Labels("a", "b"))

  def live = (exampleCounter.register ++ currentRequest.register ++ requestLatency.register) >>>
    ZLayer.fromFunction[Metrics, Service] { env =>
      val rl = requestLatency.fromEnv(env)
      val c = exampleCounter.fromEnv(env)
      val g = currentRequest.fromEnv(env)
      new Service {
        override def doSomethingImportant: UIO[Unit] =
          c.inc(Labels("c", "b")) *>
            g.dec(3.0, Labels("blah")) *>
            rl.timer(Labels.Empty)(ZIO.unit)
      }

    }

}
