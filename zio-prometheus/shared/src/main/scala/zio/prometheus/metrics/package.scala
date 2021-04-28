package zio.prometheus

import zio.prometheus.metrics.Summary.{WithMetricBuilder, WithoutMetricBuilder}
import zio.{UIO, ZIO}

package object metrics {

  //Counter
  def inc(m: Counter): ZIO[Counter.Metric[m.type], Nothing, Unit] =
    ZIO.access[Counter.Metric[m.type]](_.get.metric.inc())

  def inc(m: Counter, value: Double): ZIO[Counter.Metric[m.type], Nothing, Unit] =
    ZIO.access[Counter.Metric[m.type]](_.get.metric.inc(value))

  def inc[A <: Counter](m: Counter.Registered[A], value: Double) =
    ZIO.succeed(m.metric.inc(value))

  def inc[A <: Counter](m: Counter.Registered[A]) =
    ZIO.succeed(m.metric.inc())

  //Gauge
  def inc(m: Gauge): ZIO[Gauge.Metric[m.type], Nothing, Unit] =
    ZIO.access[Gauge.Metric[m.type]](_.get.metric.inc())

  def inc(m: Gauge, value: Double): ZIO[Gauge.Metric[m.type], Nothing, Unit] =
    ZIO.access[Gauge.Metric[m.type]](_.get.metric.inc(value))

  def inc[A <: Gauge](m: Gauge.Registered[A], value: Double) =
    ZIO.succeed(m.metric.inc(value))

  def inc[A <: Gauge](m: Gauge.Registered[A]) =
    ZIO.succeed(m.metric.inc())

  def dec(m: Gauge): ZIO[Gauge.Metric[m.type], Nothing, Unit] =
    ZIO.access[Gauge.Metric[m.type]](_.get.metric.dec())

  def dec(m: Gauge, value: Double): ZIO[Gauge.Metric[m.type], Nothing, Unit] =
    ZIO.access[Gauge.Metric[m.type]](_.get.metric.dec(value))

  def dec[A <: Gauge](m: Gauge.Registered[A], value: Double) =
    ZIO.succeed(m.metric.dec(value))

  def dec[A <: Gauge](m: Gauge.Registered[A]) =
    ZIO.succeed(m.metric.dec())

  //Summary
  def timer(m: Summary) =
    new WithoutMetricBuilder[m.type]

  def observe(m: Summary, value: Double): ZIO[Summary.Metric[m.type], Nothing, Unit] =
    ZIO.access[Summary.Metric[m.type]](_.get.metric.observe(value))

  def timer[A <: Summary](m: Summary.Registered[A]): WithMetricBuilder[A] =
    new WithMetricBuilder[A](m)

  def observe[A <: Summary](m: Summary.Registered[A], value: Double): UIO[Unit] =
    ZIO.succeed(m.metric.observe(value))

  //Histogram
  def timer(m: Histogram) =
    new Histogram.WithoutMetricBuilder[m.type]

  def timer[A <: Histogram](m: Histogram.Registered[A]): Histogram.WithMetricBuilder[A] =
    new Histogram.WithMetricBuilder[A](m)



}
