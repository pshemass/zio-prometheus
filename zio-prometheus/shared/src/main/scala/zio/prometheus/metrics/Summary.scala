package zio.prometheus.metrics

import io.prometheus.client.CollectorRegistry
import izumi.reflect.Tag
import zio.prometheus.metrics.Summary.{Registered, WithoutMetricBuilder}
import zio.{Has, UIO, ZIO, ZLayer, ZManaged}

abstract class Summary[A <: Labels: Tag](val name: String, val help: String, labelNames: A) {
  self =>

  type Metric = Has[Registered[A, self.type]]

  final def timer(labels: A): WithoutMetricBuilder[A, self.type] = new WithoutMetricBuilder[A, self.type](labels)

  final def observe(value: Double): ZIO[Metric, Nothing, Unit] =
    ZIO.access[Metric](_.get.metric.observe(value))

  final def fromEnv[R <: Metric](env: R): Registered[A, self.type] =
    env.get[Registered[A, self.type]]

  val register: ZLayer[Has[CollectorRegistry], Nothing, Metric] =
    ZLayer.fromFunction((reg: Has[CollectorRegistry]) =>
      new Registered[A, self.type](
        io.prometheus.client.Summary.build()
          .name(name)
          .help(help)
          .labelNames(labelNames.asSeq:_*)
          .register(reg.get)
      )
    )
}

object Summary {

  final class Registered[B <: Labels, A <: Summary[B]] private[prometheus](private[prometheus] val metric: io.prometheus.client.Summary) {
    def timer(labels: B): WithMetricBuilder[B, A] =
      new WithMetricBuilder[B, A](this, labels)

    def observe(value: Double, labels: B): UIO[Unit] =
      ZIO.succeed(metric.labels(labels.asSeq:_*).observe(value))
  }

  implicit class EmptyLabelOps(val s: Summary[Labels.Empty.type]) extends AnyVal {
    def timer: WithoutMetricBuilder[Labels.Empty.type, s.type] = s.timer(Labels.Empty)
  }

  implicit class EmptyLabelRegisteredOps[A <: Summary[Labels.Empty.type]](val s: Registered[Labels.Empty.type, A]) extends AnyVal {
    def timer: WithMetricBuilder[Labels.Empty.type, A] = s.timer(Labels.Empty)
  }

  final class WithoutMetricBuilder[B <: Labels: Tag, A <: Summary[B] : Tag] private[prometheus](labels: B) {
    def apply[R <: Has[Registered[B, A]], E, C](zio: ZIO[R, E, C]): ZIO[R, E, C] =
      ZManaged.accessManaged[Has[Registered[B, A]]](e =>
        (ZManaged.makeEffect(e.get.metric.labels(labels.asSeq: _*).startTimer())(_.observeDuration()))).ignore.use_(zio)
  }

  final class WithMetricBuilder[B <: Labels, A <: Summary[B]] private[prometheus](m: Summary.Registered[B, A], labels: B) {
    def apply[R, E, C](zio: ZIO[R, E, C]): ZIO[R, E, C] =
      (ZManaged.makeEffect(m.metric.labels(labels.asSeq: _*).startTimer())(_.observeDuration())).ignore.use_(zio)
  }

}




