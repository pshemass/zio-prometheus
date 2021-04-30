package zio.prometheus

import zio.{UIO, ZIO}

final class Registered[A <: Metric[B], B <: Labels] private[prometheus](private[prometheus] val metric: A#RealMetric)

object Registered {

  implicit class EmptyLabelsCounterOps(val r: Registered[Counter[Labels.Empty.type], Labels.Empty.type]) extends AnyVal {
    def inc(): UIO[Unit] =
      ZIO.succeed(r.metric.inc())

    def inc(value: Double): UIO[Unit] =
      ZIO.succeed(r.metric.inc(value))
  }

  implicit class NonEmptyLabelsCounterOps[A <: Labels](val r: Registered[Counter[A], A]) extends AnyVal {
    def inc(labels: A): UIO[Unit] =
      ZIO.succeed(r.metric.labels(labels.asSeq:_*).inc())

    def inc(value: Double, labels: A): UIO[Unit] =
      ZIO.succeed(r.metric.labels(labels.asSeq:_*).inc(value))
  }

  implicit class EmptyLabelsGaugeOps(val r: Registered[Gauge[Labels.Empty.type], Labels.Empty.type]) extends AnyVal {
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

  implicit class NonEmptyLabelsGaugeOps[A <: Labels](val r: Registered[Gauge[A], A]) extends AnyVal {
    def inc(labels: A): UIO[Unit] =
      ZIO.succeed(r.metric.labels(labels.asSeq:_*).inc())

    def dec(labels: A): UIO[Unit] =
      ZIO.succeed(r.metric.labels(labels.asSeq:_*).dec())

    def inc(value: Double, labels: A): UIO[Unit] =
      ZIO.succeed(r.metric.labels(labels.asSeq:_*).inc(value))

    def dec(value: Double, labels: A): UIO[Unit] =
      ZIO.succeed(r.metric.labels(labels.asSeq:_*).dec(value))

    def set(value: Double, labels: A): UIO[Unit] =
      ZIO.succeed(r.metric.labels(labels.asSeq:_*).set(value))
  }

}