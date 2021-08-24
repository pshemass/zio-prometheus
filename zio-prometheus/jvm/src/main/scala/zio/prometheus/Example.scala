package zio.prometheus

import zio._
import zio.clock.Clock
import zio.console.{ Console, putStrLn }
import zio.duration._

object Example {

  val requests           = Counter("_requests_total", "Total requests.", Labels("route"))
  val inProgressRequests = Gauge("_in_progress_requests", "In progress requests.", Labels.Empty)
  val receivedBytes      = Summary("_requests_size_bytes", "Request size in bytes.", Labels.Empty)
  val requestLatency     = Histogram("_requests_latency_seconds", "Request latency in seconds.", Labels.Empty)

  type Metrics = requests.Metric with inProgressRequests.Metric with requestLatency.Metric with receivedBytes.Metric

  trait Service {
    def processRequests: UIO[Unit]
  }

  def processRequests = ZIO.accessM[Has[Service]](_.get.processRequests)

  def live =
    (Clock.any ++
      requests.register ++
      inProgressRequests.register ++
      requestLatency.register ++
      receivedBytes.register) >>>
      ZLayer.fromFunction[Clock with Metrics, Service] { env =>
        val requestLatency     = Example.this.requestLatency.fromEnv(env)
        val requests           = Example.requests.fromEnv(env)
        val inProgressRequests = Example.inProgressRequests.fromEnv(env)
        val receivedBytes      = Example.receivedBytes.fromEnv(env)
        new Service {
          override def processRequests: UIO[Unit] =
            for {
              _ <- requests.inc(Labels("payment"))
              _ <- inProgressRequests.inc()
              _ <- inProgressRequests.dec()
              _ <- receivedBytes.observe(12.02)
              _ <- receivedBytes.timer(ZIO.succeed(123).delay(3.seconds)).provide(env)
              _ <- requestLatency.observe(.01)
              _ <- requestLatency.timer(ZIO.succeed(123).delay(3.seconds)).provide(env)
            } yield ()

        }

      }

}

object ExampleApp extends App {

  //try to replicate those examples https://github.com/prometheus/client_java
  val requests            = Counter("requests_total", "Total requests.", Labels("route"))
  val requests2           = Counter("requests_2_total", "Total requests.", Labels.Empty)
  val inProgressRequests  = Gauge("in_progress_requests", "In progress requests.", Labels.Empty)
  val inProgressRequests2 = Gauge("in_progress_2_requests", "In progress requests.", Labels("route"))
  val receivedBytes       = Summary("requests_size_bytes", "Request size in bytes.", Labels.Empty)
  val receivedBytes2      = Summary("requests_size_2_bytes", "Request size in bytes.", Labels("route"))
  val requestLatency      = Histogram("requests_latency_seconds", "Request latency in seconds.", Labels.Empty)
  val requestLatency2     = Histogram("requests_latency_2_seconds", "Request latency in seconds.", Labels("route"))

  override def run(args: List[String]): URIO[zio.ZEnv with Console, ExitCode] =
    myAppLogic
      .provideCustomLayer(
        (Clock.any ++ Registry.defaultRegistry) >>> (
          requests2.register ++
            requests.register ++
            inProgressRequests.register ++
            inProgressRequests2.register ++
            receivedBytes.register ++
            receivedBytes2.register ++
            requestLatency.register ++
            requestLatency2.register ++
            Example.live ++
            JvmMetrics.register ++
            HttpExporter.live(9999)
        )
      )
      .exitCode

  val myAppLogic =
    for {
      _ <- requests2.inc()
      _ <- requests.inc(Labels("payments"))
      _ <- inProgressRequests.inc()
      _ <- inProgressRequests2.inc(Labels("payments"))
      _ <- receivedBytes.observe(12.0)
      _ <- receivedBytes2.observe(12.0, Labels("payments"))
      _ <- requestLatency.observe(12.0)
      _ <- requestLatency2.observe(12.0, Labels("payments"))
      _ <- putStrLn("open http://localhost:9999")
      _ <- ZIO.unit.forever
    } yield ()
}
