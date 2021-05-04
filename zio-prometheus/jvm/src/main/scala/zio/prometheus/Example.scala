package zio.prometheus

import zio._
import zio.console.Console
import zio.duration._

object Example {

  val requests           = Counter("requests_total", "Total requests.", Labels("route"))
  val inProgressRequests = Gauge("in_progress_requests", "In progress requests.", Labels.Empty)
  val receivedBytes      = Summary("requests_size_bytes", "Request size in bytes.", Labels.Empty)
  val requestLatency     = Histogram("requests_latency_seconds", "Request latency in seconds.", Labels.Empty)

  type Metrics = requests.Metric with inProgressRequests.Metric with requestLatency.Metric with receivedBytes.Metric

  trait Service {
    def processRequests: UIO[Unit]
  }

  def processRequests = ZIO.accessM[Has[Service]](_.get.processRequests)

  def live = (requests.register ++ inProgressRequests.register ++ requestLatency.register ++ receivedBytes.register) >>>
    ZLayer.fromFunction[Metrics, Service] { env =>
      val requestLatency     = Example.requestLatency.fromEnv(env)
      val requests           = Example.requests.fromEnv(env)
      val inProgressRequests = Example.inProgressRequests.fromEnv(env)
      val receivedBytes      = Example.receivedBytes.fromEnv(env)
      new Service {
        override def processRequests: UIO[Unit] =
          for {
            _ <- requests.inc(Labels("payment"))
            _ <- inProgressRequests.inc()
            _ <- inProgressRequests.dec()
//            _ <- receivedBytes.observe(12.02)
//            _ <- receivedBytes.timer(ZIO.succeed(123).delay(3.seconds))
//            _ <- requestLatency.observe(.01)
//            _ <- requestLatency.timer(ZIO.succeed(123).delay(3.seconds))
          } yield ()

      }

    }

}

object ExampleApp extends App {

  //try to replicate those examples https://github.com/prometheus/client_java
  val requests           = Counter("requests_total", "Total requests.", Labels("route"))
  val inProgressRequests = Gauge("in_progress_requests", "In progress requests.", Labels.Empty)
  val receivedBytes      = Summary("requests_size_bytes", "Request size in bytes.", Labels.Empty)
  val requestLatency     = Histogram("requests_latency_seconds", "Request latency in seconds.", Labels.Empty)

  override def run(args: List[String]): URIO[zio.ZEnv with Console, ExitCode] =
    myAppLogic
      .provideCustomLayer(
        Registry.defaultRegistry >>> (
          requests.register ++
            inProgressRequests.register ++
            requestLatency.register ++
            receivedBytes.register ++
            Example.live
        )
      )
      .exitCode

  val myAppLogic =
    for {
      _ <- requests.inc(Labels("payment"))
      _ <- inProgressRequests.inc()
      _ <- inProgressRequests.dec()
      _ <- receivedBytes.observe(12.02)
      _ <- receivedBytes.timer(ZIO.succeed(123).delay(3.seconds))
      _ <- requestLatency.observe(.01)
      _ <- requestLatency.timer(ZIO.succeed(123).delay(3.seconds))
      _ <- Example.processRequests
      _ <- ZIO.unit.forever
    } yield ()
}
