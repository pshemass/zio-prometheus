package zio.prometheus

import zio.prometheus.metrics._
import zio._
import zio.prometheus.metrics.Summary.EmptyLabelOps

object Example {

  object exampleCounter extends Counter("example_gauge", "Shows how many requests are in.", Labels("component", "method"))
  object exampleCounter2 extends Counter("example_gauge", "Shows how many requests are in.", Labels.Empty)
  object currentRequest extends Gauge("example_gauge", "Shows how many requests are in.", Labels("component"))
  object requestLatency extends Summary("requests_latency_seconds", "Request latency in seconds.", Labels.Empty)

  type Metrics = exampleCounter.Metric with currentRequest.Metric with requestLatency.Metric with exampleCounter2.Metric

  trait Service {
    def doSomethingImportant: UIO[Unit]
  }

  val xxx = exampleCounter2.inc()
  new EmptyLabelOps(requestLatency).timer(ZIO.unit)
  val x = exampleCounter.inc(labels = Labels("a", "b"))

  def live = (exampleCounter.register ++ currentRequest.register ++ requestLatency.register ++ exampleCounter2.register) >>>
    ZLayer.fromFunction[Metrics, Service] { env =>
      val rl = requestLatency.fromEnv(env)
      val c = exampleCounter.fromEnv(env)
      val g = currentRequest.fromEnv(env)
      val x = exampleCounter2.fromEnv(env)
      new Service {
        override def doSomethingImportant: UIO[Unit] =
          c.inc(labels = Labels("c", "b")) *>
            g.dec(3.0, Labels("blah")) *>
            x.inc() *>
            rl.timer(Labels.Empty)(ZIO.unit)
      }

    }

}
