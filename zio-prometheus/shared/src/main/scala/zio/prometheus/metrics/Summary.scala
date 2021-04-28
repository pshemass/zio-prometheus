package zio.prometheus.metrics

import io.prometheus.client.CollectorRegistry
import izumi.reflect.Tag
import zio.prometheus.metrics.Summary.{Registered, WithoutMetricBuilder}
import zio.{Chunk, Has, UIO, ZIO, ZLayer, ZManaged}

abstract class Summary(val name: String, val help: String, val labels: Chunk[String]) { self =>

  type Metric = Has[Registered[self.type]]

  final def timer = new WithoutMetricBuilder[self.type]

  final def observe(value: Double): ZIO[Metric, Nothing, Unit] =
    ZIO.access[Metric](_.get.metric.observe(value))

  def register: ZLayer[Has[CollectorRegistry], Nothing, Metric] =
    ZLayer.fromFunction((reg:Has[CollectorRegistry]) =>
      new Registered[self.type](
        io.prometheus.client.Summary.build().name(name).help(help).register(reg.get)
      )
    )

}

object Summary {

  final class Registered[A <: Summary] private[prometheus] (private[prometheus] val metric: io.prometheus.client.Summary) {
    def timer: WithMetricBuilder[A] =
      new WithMetricBuilder[A](this)

    def observe(value: Double): UIO[Unit] =
      ZIO.succeed(metric.observe(value))
  }

  final class WithoutMetricBuilder[A <: Summary: Tag] private[prometheus] {
    def apply[R <: Has[Registered[A]], E, B](zio: ZIO[R, E, B]): ZIO[R, E, B] =
      ZManaged.accessManaged[Has[Registered[A]]](e =>
        (ZManaged.makeEffect(e.get.metric.startTimer())(_.observeDuration()))).ignore.use_(zio)
  }

  final class WithMetricBuilder[A <: Summary] private[prometheus] (m: Summary.Registered[A]) {
    def apply[R, E, B](zio: ZIO[R, E, B]): ZIO[R, E, B] =
      (ZManaged.makeEffect(m.metric.startTimer())(_.observeDuration())).ignore.use_(zio)
  }

}




