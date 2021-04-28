package zio.prometheus.metrics

import io.prometheus.client.CollectorRegistry
import izumi.reflect.Tag
import zio.prometheus.metrics.Histogram.{Registered, WithoutMetricBuilder}
import zio.{Has, ZIO, ZLayer, ZManaged}

abstract class Histogram[A <: Labels: Tag](val name: String, val help: String, val labelNames: Labels) { self =>
  type Metric = Has[Registered[A, self.type]]

  final def timer(labels: A) = new WithoutMetricBuilder[A, self.type](labels)

  final def observe(value: Double): ZIO[Metric, Nothing, Unit] =
    ZIO.access[Metric](_.get.metric.observe(value))

  final def fromEnv[R <: Metric](env: R): Registered[A, self.type] =
    env.get[Registered[A, self.type]]

  val register: ZLayer[Has[CollectorRegistry], Nothing, Metric] =
    ZLayer.fromFunction((reg: Has[CollectorRegistry]) =>
      new Registered[A, self.type](
        io.prometheus.client.Histogram.build()
          .name(name)
          .help(help)
          .labelNames(labelNames.asSeq:_*)
          .register(reg.get)
      )
    )
}

object Histogram {
  final class Registered[B <: Labels, A <: Histogram[B]] private[prometheus] (private[prometheus] val metric: io.prometheus.client.Histogram)

  final class WithoutMetricBuilder[B <: Labels: Tag, A <: Histogram[B] : Tag] private[prometheus](labels: B) {
    def apply[R <: Has[Registered[B, A]], E, C](zio: ZIO[R, E, C]): ZIO[R, E, C] =
      ZManaged.accessManaged[Has[Registered[B, A]]](e =>
        (ZManaged.makeEffect(e.get.metric.labels(labels.asSeq: _*).startTimer())(_.observeDuration()))).ignore.use_(zio)
  }

  final class WithMetricBuilder[B <: Labels, A <: Histogram[B]] private[prometheus](m: Registered[B, A], labels: B) {
    def apply[R, E, C](zio: ZIO[R, E, C]): ZIO[R, E, C] =
      (ZManaged.makeEffect(m.metric.labels(labels.asSeq: _*).startTimer())(_.observeDuration())).ignore.use_(zio)
  }

}




