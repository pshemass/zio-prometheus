package zio.prometheus.metrics

import io.prometheus.client.CollectorRegistry
import izumi.reflect.Tag
import zio.prometheus.metrics.Histogram.{Registered, WithoutMetricBuilder}
import zio.{Chunk, Has, ZIO, ZLayer, ZManaged}

abstract class Histogram(val name: String, val help: String, val labels: Chunk[String]) { self =>
  type Metric = Has[Registered[self.type]]

  final def timer = new WithoutMetricBuilder[self.type]

  final def observe(value: Double): ZIO[Metric, Nothing, Unit] =
    ZIO.access[Metric](_.get.metric.observe(value))

  def register: ZLayer[Has[CollectorRegistry], Nothing, Metric] =
    ZLayer.fromFunction((reg:Has[CollectorRegistry]) =>
      new Registered[self.type](
        io.prometheus.client.Histogram.build().name(name).help(help).register(reg.get)
      )
    )

}

object Histogram {
  type Metric[A <: Histogram] = Has[Registered[A]]

  final class Registered[A <: Histogram] private[prometheus] (private[prometheus] val metric: io.prometheus.client.Histogram)

  private[prometheus] final class WithoutMetricBuilder[A <: Histogram: Tag] {
    def apply[R <: Histogram.Metric[A], E, B](zio: ZIO[R, E, B]): ZIO[R, E, B] =
      ZManaged.accessManaged[Histogram.Metric[A]](e =>
        (ZManaged.makeEffect(e.get.metric.startTimer())(_.observeDuration()))).ignore.use_(zio)
  }

  private[prometheus] final class WithMetricBuilder[A <: Histogram](m: Histogram.Registered[A]) {
    def apply[R, E, B](zio: ZIO[R, E, B]): ZIO[R, E, B] =
      (ZManaged.makeEffect(m.metric.startTimer())(_.observeDuration())).ignore.use_(zio)
  }

}




