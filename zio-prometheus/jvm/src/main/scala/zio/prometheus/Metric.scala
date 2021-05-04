package zio.prometheus

import io.prometheus.client.{ Collector, CollectorRegistry }
import zio.{ Has, Tag, URIO, ZIO, ZLayer, ZManaged }

sealed abstract class Metric[A <: Labels: Tag] {
  self =>
  type Metric = Has[Registered[self.type, A]]
  private[prometheus] type RealMetric <: Collector

  final def fromEnv[R <: Metric](env: R): Registered[self.type, A] =
    env.get[Registered[self.type, A]]

  private[prometheus] def access(fn: RealMetric => Unit): URIO[Metric, Unit] =
    ZIO.access[Metric](env => fn(env.get.metric))

  private[prometheus] def accessManaged[B](acquire: RealMetric => B)(release: B => Any) =
    ZManaged.accessManaged[Metric](e => ZManaged.makeEffect(acquire(e.get.metric))(release))

  protected def register(registry: CollectorRegistry): RealMetric

  val register: ZLayer[Has[CollectorRegistry], Nothing, Metric] =
    ZLayer.fromService((reg: CollectorRegistry) =>
      new Registered[self.type, A](
        register(reg)
      )
    )
}

case class Counter[A <: Labels: Tag](val name: String, val help: String, labelNames: A) extends Metric[A] {
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

  implicit class CounterEmptyLabelOps[M <: Has[_]](val counter: Counter[Labels.Empty.type] { type Metric = M })
      extends AnyVal {
    def inc(): URIO[M, Unit] = counter.access(_.inc()).asInstanceOf[URIO[M, Unit]]

    def inc(value: Double): URIO[M, Unit] = counter.access(_.inc(value)).asInstanceOf[URIO[M, Unit]]
  }

  implicit class CounterNonEmptyLabelOps[M <: Has[_], A <: Labels](val counter: Counter[A] { type Metric = M })
      extends AnyVal {
    def inc(labels: A): URIO[M, Unit] = counter.access(_.labels(labels.asSeq: _*).inc()).asInstanceOf[URIO[M, Unit]]

    def inc(value: Double, labels: A): URIO[M, Unit] =
      counter.access(_.labels(labels.asSeq: _*).inc(value)).asInstanceOf[URIO[M, Unit]]
  }

}

case class Gauge[A <: Labels: Tag](val name: String, val help: String, labelNames: A) extends Metric[A] {
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

  implicit class GaugeEmptyLabelOps[M <: Has[_]](val metric: Gauge[Labels.Empty.type] { type Metric = M })
      extends AnyVal {
    def inc(): URIO[M, Unit] = metric.access(_.inc()).asInstanceOf[URIO[M, Unit]]

    def dec(): URIO[M, Unit] = metric.access(_.dec()).asInstanceOf[URIO[M, Unit]]

    def inc(value: Double): URIO[M, Unit] = metric.access(_.inc(value)).asInstanceOf[URIO[M, Unit]]

    def dec(value: Double): URIO[M, Unit] = metric.access(_.dec(value)).asInstanceOf[URIO[M, Unit]]

    def set(value: Double): URIO[M, Unit] = metric.access(_.set(value)).asInstanceOf[URIO[M, Unit]]
  }

  implicit class GaugeNonEmptyLabelOps[M <: Has[_], A <: Labels](val metric: Gauge[A] { type Metric = M })
      extends AnyVal {
    def inc(labels: A): URIO[M, Unit] = metric.access(_.labels(labels.asSeq: _*).inc()).asInstanceOf[URIO[M, Unit]]

    def dec(labels: A): URIO[M, Unit] = metric.access(_.labels(labels.asSeq: _*).dec()).asInstanceOf[URIO[M, Unit]]

    def inc(value: Double, labels: A): URIO[M, Unit] =
      metric.access(_.labels(labels.asSeq: _*).inc(value)).asInstanceOf[URIO[M, Unit]]

    def dec(value: Double, labels: A): URIO[M, Unit] =
      metric.access(_.labels(labels.asSeq: _*).dec(value)).asInstanceOf[URIO[M, Unit]]

    def set(value: Double, labels: A): URIO[M, Unit] =
      metric.access(_.labels(labels.asSeq: _*).set(value)).asInstanceOf[URIO[M, Unit]]
  }

}

case class Summary[A <: Labels: Tag](val name: String, val help: String, labelNames: A) extends Metric[A] {
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

  implicit class SummaryEmptyLabelOps[M <: Has[_]](val metric: Summary[Labels.Empty.type] { type Metric = M })
      extends AnyVal {
    final def timer[R <: M, E, B: Tag](zio: ZIO[R, E, B]): ZIO[R, E, B] =
      metric.accessManaged(_.startTimer())(_.observeDuration()).ignore.use_(zio).asInstanceOf[ZIO[R, E, B]]

    final def observe(value: Double): URIO[M, Unit] =
      metric.access(_.observe(value)).asInstanceOf[URIO[M, Unit]]
  }

  implicit class SummaryNonEmptyLabelOps[M <: Has[_], A <: Labels](val metric: Summary[A] { type Metric = M })
      extends AnyVal {
    final def timer[R <: M, E, B: Tag](zio: ZIO[R, E, B], labels: A): ZIO[R, E, B] =
      metric
        .accessManaged(_.labels(labels.asSeq: _*).startTimer())(_.observeDuration())
        .ignore
        .use_(zio)
        .asInstanceOf[ZIO[R, E, B]]

    final def observe(value: Double, labels: A): URIO[M, Unit] =
      metric.access(_.labels(labels.asSeq: _*).observe(value)).asInstanceOf[URIO[M, Unit]]
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

  implicit class HistogramEmptyLabelOps[M <: Has[_]](val metric: Histogram[Labels.Empty.type] { type Metric = M })
      extends AnyVal {
    final def timer[R <: M, E, B: Tag](zio: ZIO[R, E, B]): ZIO[R, E, B] =
      metric.accessManaged(_.startTimer())(_.observeDuration()).ignore.use_(zio).asInstanceOf[ZIO[R, E, B]]

    final def observe(value: Double): URIO[M, Unit] =
      metric.access(_.observe(value)).asInstanceOf[URIO[M, Unit]]
  }

  implicit class HistogramNonEmptyLabelOps[M <: Has[_], A <: Labels](val metric: Histogram[A] { type Metric = M })
      extends AnyVal {
    final def timer[R <: M, E, B: Tag](zio: ZIO[R, E, B], labels: A) =
      metric
        .accessManaged(_.labels(labels.asSeq: _*).startTimer())(_.observeDuration())
        .ignore
        .use_(zio)
        .asInstanceOf[ZIO[R, E, B]]

    final def observe(value: Double, labels: A) =
      metric.access(_.labels(labels.asSeq: _*).observe(value)).asInstanceOf[URIO[M, Unit]]
  }

}
