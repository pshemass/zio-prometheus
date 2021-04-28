package zio.prometheus.metrics

import io.prometheus.client.CollectorRegistry
import zio.prometheus.metrics.Counter.{Metric, Registered}
import zio.{Chunk, Has, UIO, ZIO, ZLayer}

abstract class Counter(val name: String, val help: String, val labels: Chunk[String]) { self =>
  type Metric = Has[Registered[self.type]]

  val inc: ZIO[Metric, Nothing, Unit] =
    ZIO.access[Metric](_.get.metric.inc())

  final def inc(value: Double): ZIO[Counter.Metric[self.type], Nothing, Unit] =
    ZIO.access[Metric](_.get.metric.inc(value))

  final def fromEnv[R <: Metric](env: R): Registered[self.type] =
    env.get[Registered[self.type ]]

  val register: ZLayer[Has[CollectorRegistry], Nothing, Metric] =
    ZLayer.fromFunction((reg:Has[CollectorRegistry]) =>
      new Registered[self.type](io.prometheus.client.Counter.build().name(self.name).help(help).register(reg.get))
    )
}

object Counter {

  type Metric[A <: Counter] = Has[Registered[A]]

  final class Registered[A <: Counter] private[prometheus] (private[prometheus] val metric: io.prometheus.client.Counter) {
    def inc(value: Double): UIO[Unit] = ZIO.succeed(metric.inc(value))

    def inc: UIO[Unit] = ZIO.succeed(metric.inc())
  }

}
