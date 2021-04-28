package zio.prometheus.metrics

import io.prometheus.client.CollectorRegistry
import zio.prometheus.metrics.Counter.Registered
import zio.{Has, Tag, UIO, ZIO, ZLayer}

abstract class Counter[A <: Labels : Tag](val name: String, val help: String, labelNames: A) {
  self =>
  type Metric = Has[Registered[A, self.type]]

  def inc(labels: A): ZIO[Metric, Nothing, Unit] =
    ZIO.access[Metric](_.get.metric.labels(labels.asSeq: _*).inc())

  final def inc(value: Double, labels: A): ZIO[Metric, Nothing, Unit] =
    ZIO.access[Metric](_.get.metric.labels(labels.asSeq: _*).inc(value))

  final def fromEnv[R <: Metric](env: R): Registered[A, self.type] =
    env.get[Registered[A, self.type]]

  val register: ZLayer[Has[CollectorRegistry], Nothing, Metric] =
    ZLayer.fromFunction((reg: Has[CollectorRegistry]) =>
      new Registered[A, self.type](
        io.prometheus.client.Counter.build()
          .name(self.name)
          .help(help)
          .labelNames(labelNames.asSeq: _*)
          .register(reg.get))
    )
}

object Counter {

  final class Registered[B <: Labels, A <: Counter[B]] private[prometheus](private[prometheus] val metric: io.prometheus.client.Counter) {
    def inc(labels: B): UIO[Unit] = ZIO.succeed(metric.labels(labels.asSeq: _*).inc())

    def inc(value: Double, labels: B): UIO[Unit] = ZIO.succeed(metric.labels(labels.asSeq: _*).inc(value))
  }
  implicit class CounterEmptyLabelOps(val s: Counter[Labels.Empty.type]) extends AnyVal {
    def inc() = ZIO.access[s.Metric](_.get.metric.inc())
    def inc(value: Double) = ZIO.access[s.Metric](_.get.metric.inc(value))
  }

  implicit class RegisteredCounterEmptyLabelOps[A <: Counter[Labels.Empty.type]](val s: Registered[Labels.Empty.type, A]) extends AnyVal {
    def inc() = s.inc(Labels.Empty)
    def inc(value: Double) = s.inc(value, Labels.Empty)
  }

}
