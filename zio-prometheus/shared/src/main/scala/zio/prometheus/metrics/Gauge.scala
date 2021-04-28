package zio.prometheus.metrics

import io.prometheus.client.CollectorRegistry
import zio.prometheus.metrics.Gauge.Registered
import zio.{Has, Tag, UIO, ZIO, ZLayer}

abstract class Gauge[A <: Labels: Tag](val name: String, val help: String, val labelNames: A) { self =>
  type Metric = Has[Registered[A, self.type]]

  def inc(labels: A): ZIO[Metric, Nothing, Unit] =
    ZIO.access[Metric](_.get.metric.labels(labels.asSeq: _*).inc())

  final def inc(value: Double, labels: A): ZIO[Metric, Nothing, Unit] =
    ZIO.access[Metric](_.get.metric.labels(labels.asSeq: _*).inc(value))

  def dec(labels: A): ZIO[Metric, Nothing, Unit] =
    ZIO.access[Metric](_.get.metric.labels(labels.asSeq: _*).dec())

  final def dec(value: Double, labels: A): ZIO[Metric, Nothing, Unit] =
    ZIO.access[Metric](_.get.metric.labels(labels.asSeq: _*).dec(value))

  final def fromEnv[R <: Metric](env: R): Registered[A, self.type] =
    env.get[Registered[A, self.type ]]

  val register: ZLayer[Has[CollectorRegistry], Nothing, Metric] =
    ZLayer.fromFunction((reg:Has[CollectorRegistry]) =>
      new Registered[A, self.type](io.prometheus.client.Gauge.build().name(self.name).help(help).register(reg.get))
    )
}

object Gauge {

  final class Registered[B <: Labels, A <: Gauge[B]] private[prometheus] (private[prometheus] val metric: io.prometheus.client.Gauge) {
    def inc(value: Double, labels: B): UIO[Unit] = ZIO.succeed(metric.labels(labels.asSeq: _*).inc(value))

    def inc(labels: B): UIO[Unit] = ZIO.succeed(metric.labels(labels.asSeq: _*).inc())

    def dec(value: Double, labels: B): UIO[Unit] = ZIO.succeed(metric.labels(labels.asSeq: _*).dec(value))

    def dec(labels: B): UIO[Unit] = ZIO.succeed(metric.labels(labels.asSeq: _*).dec())
  }

}


