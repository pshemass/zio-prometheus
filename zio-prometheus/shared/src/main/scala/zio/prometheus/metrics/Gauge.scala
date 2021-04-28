package zio.prometheus.metrics

import io.prometheus.client.CollectorRegistry
import zio.prometheus.metrics.Gauge.Registered
import zio.{Chunk, Has, UIO, ZIO, ZLayer}

abstract class Gauge(val name: String, val help: String, val labels: Chunk[String]) { self =>
  type Metric = Has[Registered[self.type]]

  val inc: ZIO[Metric, Nothing, Unit] =
    ZIO.access[Metric](_.get.metric.inc())

  final def inc(value: Double): ZIO[Metric, Nothing, Unit] =
    ZIO.access[Metric](_.get.metric.inc(value))

  val dec: ZIO[Metric, Nothing, Unit] =
    ZIO.access[Metric](_.get.metric.dec())

  final def dec(value: Double): ZIO[Metric, Nothing, Unit] =
    ZIO.access[Metric](_.get.metric.dec(value))

  final def fromEnv[R <: Metric](env: R): Registered[self.type] =
    env.get[Registered[self.type ]]

  val register: ZLayer[Has[CollectorRegistry], Nothing, Metric] =
    ZLayer.fromFunction((reg:Has[CollectorRegistry]) =>
      new Registered[self.type](io.prometheus.client.Gauge.build().name(self.name).help(help).register(reg.get))
    )
}

object Gauge {
  type Metric[A <: Gauge] = Has[Registered[A]]

  final class Registered[A <: Gauge] private[prometheus] (private[prometheus] val metric: io.prometheus.client.Gauge) {
    def inc(value: Double): UIO[Unit] = ZIO.succeed(metric.inc(value))

    def inc: UIO[Unit] = ZIO.succeed(metric.inc())

    def dec(value: Double): UIO[Unit] = ZIO.succeed(metric.dec(value))

    def dec: UIO[Unit] = ZIO.succeed(metric.dec())
  }

}


