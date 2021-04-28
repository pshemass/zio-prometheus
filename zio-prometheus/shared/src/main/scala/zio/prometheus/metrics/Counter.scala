package zio.prometheus.metrics

import io.prometheus.client.CollectorRegistry
import zio.prometheus.metrics.Counter.Registered
import zio.{Has, Tag, UIO, ZIO, ZLayer}

abstract class Counter[A <: Labels : Tag](val name: String, val help: String, labels: A) {
  self =>
  type Metric = Has[Registered[A, self.type]]

  def inc(labels: A): ZIO[Metric, Nothing, Unit] =
    ZIO.access[Metric](_.get.metric.inc())

  final def inc(value: Double, labels: A): ZIO[Metric, Nothing, Unit] =
    ZIO.access[Metric](_.get.metric.inc(value))

  final def fromEnv[R <: Metric](env: R): Registered[A, self.type] =
    env.get[Registered[A, self.type]]

  val register: ZLayer[Has[CollectorRegistry], Nothing, Metric] =
    ZLayer.fromFunction((reg: Has[CollectorRegistry]) =>
      new Registered[A, self.type](
        io.prometheus.client.Counter.build()
          .name(self.name)
          .help(help)
          .labelNames(labels.asSeq:_*)
          .register(reg.get))
    )
}

object Counter {

  final class Registered[B <: Labels, A <: Counter[B]] private[prometheus](private[prometheus] val metric: io.prometheus.client.Counter) {
    def inc(labels: B): UIO[Unit] = ZIO.succeed(metric.inc())

    def inc(value: Double, labels: B): UIO[Unit] = ZIO.succeed(metric.inc(value))
  }

}
