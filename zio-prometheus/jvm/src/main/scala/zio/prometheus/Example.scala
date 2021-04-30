//package zio.prometheus
//
//import zio.prometheus.metrics._
//import zio._
//
//object Example {
//
//  object exampleCounter extends Counter("example_gauge", "Shows how many requests are in.", Labels("component", "method"))
//  object exampleCounter2 extends Counter("example_gauge", "Shows how many requests are in.", Labels.Empty)
//  object currentRequest extends Gauge("example_gauge", "Shows how many requests are in.", Labels("component"))
//  object requestLatency extends Summary("requests_latency_seconds", "Request latency in seconds.", Labels.Empty)
//
////  val aa = Counter1("aaa", "sss", Labels.Empty)
//
//
//  type Metrics = exampleCounter.Metric with currentRequest.Metric with requestLatency.Metric with exampleCounter2.Metric
//
//  trait Service {
//    def doSomethingImportant: UIO[Unit]
//  }
//
//
//  def live = (exampleCounter.register ++ currentRequest.register ++ requestLatency.register ++ exampleCounter2.register) >>>
//    ZLayer.fromFunction[Metrics, Service] { env =>
//      val rl = requestLatency.fromEnv(env)
//      val c = exampleCounter.fromEnv(env)
//      val g = currentRequest.fromEnv(env)
//      val x = exampleCounter2.fromEnv(env)
//      new Service {
//        override def doSomethingImportant: UIO[Unit] =
//          c.inc(Labels("c", "b")) *>
//            g.dec(3.0, Labels("blah")) *>
//            x.inc(1.3) *>
//            rl.timer(ZIO.unit, Labels.Empty) //*>
////            rl.observe(3.0)
//      }
//
//    }
//
//}
