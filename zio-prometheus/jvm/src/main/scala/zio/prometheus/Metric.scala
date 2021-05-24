package zio.prometheus

import io.prometheus.client.{ Collector, CollectorRegistry }
import zio.prometheus.Metric.MetricsOpsBase
import zio.prometheus.Summary.SummaryNonEmptyLabelOps
import zio.{ Has, Tag, URIO, ZIO, ZLayer, ZManaged }

import scala.language.implicitConversions

sealed abstract class Metric[A <: Labels] {
  self =>
  type Metric = Has[Registered[self.type, A]]
  private[prometheus] type RealMetric <: Collector

  protected def register(registry: CollectorRegistry): RealMetric

}
object Metric {

  private[prometheus] abstract class MetricsOpsBase[A <: Metric[B]: Tag, B <: Labels: Tag](val metric: A) {
    type HasMetric = Has[Registered[A, B]]
    protected final def access(fn: A#RealMetric => Unit): URIO[HasMetric, Unit] =
      ZIO.access[HasMetric](env => fn(env.get.metric))

    protected final def accessManaged[C](acquire: A#RealMetric => C)(release: C => Any) =
      ZManaged.accessManaged[HasMetric](e => ZManaged.makeEffect(acquire(e.get.metric))(release))

    final def fromEnv[R <: HasMetric](env: R): Registered[A, B] = env.get

    val register: ZLayer[Has[CollectorRegistry], Nothing, HasMetric] =
      ZLayer.fromService((reg: CollectorRegistry) =>
        new Registered[A, B](
          metric.register(reg)
        )
      )
  }

}

case class Counter[A <: Labels](val name: String, val help: String, labelNames: A) extends Metric[A] {
  self =>
  override private[prometheus] type RealMetric = io.prometheus.client.Counter

  override protected def register(registry: CollectorRegistry): RealMetric =
    io.prometheus.client.Counter
      .build()
      .name(name)
      .help(help)
      .labelNames(labelNames.asSeq: _*)
      .register(registry)
}

object Counter {

  implicit def toCounterEmptyLabelOps(counter: Counter[Labels.Empty.type])(implicit
    tag: Tag[counter.type]
  ): CounterEmptyLabelOps[counter.type] =
    new CounterEmptyLabelOps[counter.type](counter)

  final class CounterEmptyLabelOps[C <: Counter[Labels.Empty.type]: Tag](val counter: C)
      extends MetricsOpsBase[C, Labels.Empty.type](counter) {
    def inc(): URIO[HasMetric, Unit] =
      access(_.inc())

    def inc(value: Double): URIO[HasMetric, Unit] =
      access(_.inc(value))

  }

  implicit def toCounterNonEmptyLabelOps[B <: Labels.NonEmpty: Tag](counter: Counter[B])(implicit
    tag: Tag[counter.type]
  ): CounterNonEmptyLabelOps[counter.type, B] =
    new CounterNonEmptyLabelOps[counter.type, B](counter)

  final class CounterNonEmptyLabelOps[A <: Counter[B]: Tag, B <: Labels: Tag](val counter: A)
      extends MetricsOpsBase[A, B](counter) {
    def inc(labels: B): URIO[HasMetric, Unit] = access(_.labels(labels.asSeq: _*).inc())

    def inc(value: Double, labels: B): URIO[HasMetric, Unit] =
      access(_.labels(labels.asSeq: _*).inc(value))
  }

}

case class Gauge[A <: Labels](val name: String, val help: String, labelNames: A) extends Metric[A] {
  self =>
  override private[prometheus] type RealMetric = io.prometheus.client.Gauge

  override protected def register(registry: CollectorRegistry): RealMetric =
    io.prometheus.client.Gauge
      .build()
      .name(name)
      .help(help)
      .labelNames(labelNames.asSeq: _*)
      .register(registry)
}

object Gauge {

  implicit def toGaugeEmptyLabelOps(metric: Gauge[Labels.Empty.type])(implicit
    tag: Tag[metric.type]
  ): GaugeEmptyLabelOps[metric.type] =
    new GaugeEmptyLabelOps[metric.type](metric)

  final class GaugeEmptyLabelOps[A <: Gauge[Labels.Empty.type]: Tag](metric: A)
      extends MetricsOpsBase[A, Labels.Empty.type](metric) {
    def inc(): URIO[HasMetric, Unit] = access(_.inc())

    def dec(): URIO[HasMetric, Unit] = access(_.dec())

    def inc(value: Double): URIO[HasMetric, Unit] = access(_.inc(value))

    def dec(value: Double): URIO[HasMetric, Unit] = access(_.dec(value))

    def set(value: Double): URIO[HasMetric, Unit] = access(_.set(value))
  }

  implicit def toGaugeNonEmptyLabelOps[B <: Labels.NonEmpty: Tag](metric: Gauge[B])(implicit
    tag: Tag[metric.type]
  ): GaugeNonEmptyLabelOps[metric.type, B] =
    new GaugeNonEmptyLabelOps[metric.type, B](metric)

  final class GaugeNonEmptyLabelOps[A <: Gauge[B]: Tag, B <: Labels: Tag](metric: A)
      extends MetricsOpsBase[A, B](metric) {
    def inc(labels: B): URIO[HasMetric, Unit] = access(_.labels(labels.asSeq: _*).inc())

    def dec(labels: B): URIO[HasMetric, Unit] = access(_.labels(labels.asSeq: _*).dec())

    def inc(value: Double, labels: B): URIO[HasMetric, Unit] =
      access(_.labels(labels.asSeq: _*).inc(value))

    def dec(value: Double, labels: B): URIO[HasMetric, Unit] =
      access(_.labels(labels.asSeq: _*).dec(value))

    def set(value: Double, labels: B): URIO[HasMetric, Unit] =
      access(_.labels(labels.asSeq: _*).set(value))
  }

}

case class Summary[A <: Labels](val name: String, val help: String, labelNames: A) extends Metric[A] {
  self =>
  override private[prometheus] type RealMetric = io.prometheus.client.Summary

  override protected def register(registry: CollectorRegistry): RealMetric =
    io.prometheus.client.Summary
      .build()
      .name(name)
      .help(help)
      .labelNames(labelNames.asSeq: _*)
      .register(registry)
}

object Summary {

  implicit def toSummaryEmptyLabelOps(metric: Summary[Labels.Empty.type])(implicit
    tag: Tag[metric.type]
  ): SummaryEmptyLabelOps[metric.type] =
    new SummaryEmptyLabelOps[metric.type](metric)

  final class SummaryEmptyLabelOps[A <: Summary[Labels.Empty.type]: Tag](metric: A)
      extends MetricsOpsBase[A, Labels.Empty.type](metric) {
    def timer[R <: HasMetric, E, B: Tag](zio: ZIO[R, E, B]): ZIO[R, E, B] =
      accessManaged(_.startTimer())(_.observeDuration()).ignore.use_(zio)

    def observe(value: Double): URIO[HasMetric, Unit] =
      access(_.observe(value))
  }

  implicit def toSummaryNonEmptyLabelOps[B <: Labels.NonEmpty: Tag](metric: Summary[B])(implicit
    tag: Tag[metric.type]
  ): SummaryNonEmptyLabelOps[metric.type, B] =
    new SummaryNonEmptyLabelOps[metric.type, B](metric)

  final class SummaryNonEmptyLabelOps[A <: Summary[B]: Tag, B <: Labels: Tag](metric: A)
      extends MetricsOpsBase[A, B](metric) {
    def timer[R <: HasMetric, E, C: Tag](zio: ZIO[R, E, C], labels: B): ZIO[R, E, C] =
      accessManaged(_.labels(labels.asSeq: _*).startTimer())(_.observeDuration()).ignore
        .use_(zio)

    def observe(value: Double, labels: B): URIO[HasMetric, Unit] =
      access(_.labels(labels.asSeq: _*).observe(value))
  }

}

case class Histogram[A <: Labels: Tag](val name: String, val help: String, labelNames: A) extends Metric[A] {
  self =>
  override private[prometheus] type RealMetric = io.prometheus.client.Histogram

  override protected def register(registry: CollectorRegistry): RealMetric =
    io.prometheus.client.Histogram
      .build()
      .name(name)
      .help(help)
      .labelNames(labelNames.asSeq: _*)
      .register(registry)
}

object Histogram {

  implicit def toSummaryEmptyLabelOps(metric: Histogram[Labels.Empty.type])(implicit
    tag: Tag[metric.type]
  ): HistogramEmptyLabelOps[metric.type] =
    new HistogramEmptyLabelOps[metric.type](metric)

  final class HistogramEmptyLabelOps[A <: Histogram[Labels.Empty.type]: Tag](metric: A)
      extends MetricsOpsBase[A, Labels.Empty.type](metric) {
    def timer[R <: HasMetric, E, B: Tag](zio: ZIO[R, E, B]): ZIO[R, E, B] =
      accessManaged(_.startTimer())(_.observeDuration()).ignore.use_(zio)

    def observe(value: Double): URIO[HasMetric, Unit] =
      access(_.observe(value))
  }

  implicit def toHistogramNonEmptyLabelOps[B <: Labels.NonEmpty: Tag](metric: Histogram[B])(implicit
    tag: Tag[metric.type]
  ): HistogramNonEmptyLabelOps[metric.type, B] =
    new HistogramNonEmptyLabelOps[metric.type, B](metric)

  final class HistogramNonEmptyLabelOps[A <: Histogram[B]: Tag, B <: Labels: Tag](metric: A)
      extends MetricsOpsBase[A, B](metric) {
    def timer[R <: HasMetric, E, C: Tag](zio: ZIO[R, E, C], labels: B) =
      accessManaged(_.labels(labels.asSeq: _*).startTimer())(_.observeDuration()).ignore
        .use_(zio)

    def observe(value: Double, labels: B) =
      access(_.labels(labels.asSeq: _*).observe(value))
  }

}
