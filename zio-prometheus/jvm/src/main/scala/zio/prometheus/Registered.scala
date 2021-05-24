package zio.prometheus

import zio.{ UIO, ZIO, ZManaged }

final class Registered[A <: Metric[B], B <: Labels] private[prometheus] (private[prometheus] val metric: A#RealMetric)

object Registered {

  implicit class EmptyLabelsCounterOps(val r: Registered[_ <: Counter[Labels.Empty.type], Labels.Empty.type])
      extends AnyVal {
    def inc(): UIO[Unit] =
      ZIO.succeed(r.metric.inc())

    def inc(value: Double): UIO[Unit] =
      ZIO.succeed(r.metric.inc(value))
  }

  implicit class NonEmptyLabelsCounterOps[A <: Labels](val r: Registered[_ <: Counter[A], A]) extends AnyVal {
    def inc(labels: A): UIO[Unit] =
      ZIO.succeed(r.metric.labels(labels.asSeq: _*).inc())

    def inc(value: Double, labels: A): UIO[Unit] =
      ZIO.succeed(r.metric.labels(labels.asSeq: _*).inc(value))
  }

  implicit class EmptyLabelsGaugeOps(val r: Registered[_ <: Gauge[Labels.Empty.type], Labels.Empty.type])
      extends AnyVal {
    def inc(): UIO[Unit] =
      ZIO.succeed(r.metric.inc())

    def dec(): UIO[Unit] =
      ZIO.succeed(r.metric.dec())

    def inc(value: Double): UIO[Unit] =
      ZIO.succeed(r.metric.inc(value))

    def dec(value: Double): UIO[Unit] =
      ZIO.succeed(r.metric.dec(value))

    def set(value: Double): UIO[Unit] =
      ZIO.succeed(r.metric.set(value))
  }

  implicit class NonEmptyLabelsGaugeOps[A <: Labels](val r: Registered[_ <: Gauge[A], A]) extends AnyVal {
    def inc(labels: A): UIO[Unit] =
      ZIO.succeed(r.metric.labels(labels.asSeq: _*).inc())

    def dec(labels: A): UIO[Unit] =
      ZIO.succeed(r.metric.labels(labels.asSeq: _*).dec())

    def inc(value: Double, labels: A): UIO[Unit] =
      ZIO.succeed(r.metric.labels(labels.asSeq: _*).inc(value))

    def dec(value: Double, labels: A): UIO[Unit] =
      ZIO.succeed(r.metric.labels(labels.asSeq: _*).dec(value))

    def set(value: Double, labels: A): UIO[Unit] =
      ZIO.succeed(r.metric.labels(labels.asSeq: _*).set(value))
  }

  implicit class EmptyLabelsSummaryOps(val r: Registered[_ <: Summary[Labels.Empty.type], Labels.Empty.type])
      extends AnyVal {
    def observe(value: Double): UIO[Unit] =
      ZIO.succeed(r.metric.observe(value))

    def timer[R, E, B](zio: ZIO[R, E, B]): ZIO[R, E, B] =
      ZManaged.makeEffect(r.metric.startTimer())(_.observeDuration()).ignore.use_(zio)
  }

  implicit class NonEmptyLabelsSummaryOps[A <: Labels.NonEmpty](val r: Registered[_ <: Summary[A], A]) extends AnyVal {
    def observe(value: Double, labels: A): UIO[Unit] =
      ZIO.succeed(r.metric.labels(labels.asSeq: _*).observe(value))

    def timer[R, E, B](zio: ZIO[R, E, B], labels: A): ZIO[R, E, B] =
      ZManaged.makeEffect(r.metric.labels(labels.asSeq: _*).startTimer())(_.observeDuration()).ignore.use_(zio)
  }

  implicit class EmptyLabelsHistogramOps(val r: Registered[_ <: Histogram[Labels.Empty.type], Labels.Empty.type])
      extends AnyVal {
    def observe(value: Double): UIO[Unit] =
      ZIO.succeed(r.metric.observe(value))

    def timer[R, E, B](zio: ZIO[R, E, B]): ZIO[R, E, B] =
      ZManaged.makeEffect(r.metric.startTimer())(_.observeDuration()).ignore.use_(zio)
  }

  implicit class NonEmptyLabelsHistogramOps[A <: Labels.NonEmpty](val r: Registered[_ <: Histogram[A], A])
      extends AnyVal {
    def observe(value: Double, labels: A): UIO[Unit] =
      ZIO.succeed(r.metric.labels(labels.asSeq: _*).observe(value))

    def timer[R, E, B](zio: ZIO[R, E, B], labels: A): ZIO[R, E, B] =
      ZManaged.makeEffect(r.metric.labels(labels.asSeq: _*).startTimer())(_.observeDuration()).ignore.use_(zio)
  }
}
