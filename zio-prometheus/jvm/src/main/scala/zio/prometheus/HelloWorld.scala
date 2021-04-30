package zio.prometheus

import zio.prometheus.Counter.CounterEmptyLabelOps
import zio.prometheus.Registered.EmptyLabelsCounterOps
import zio.{App, ZIO}

object HelloWorld extends App {

//  object counter1 extends Counter1("counter_1", "Help.", Labels("component", "method"))
//  object counter2 extends Counter1("counter_2", "Help.", Labels.Empty)

  val counter2: Counter[Labels.Empty.type] = Counter("counter_2", "Help.", Labels.Empty)
  val counter1 = Counter("counter_2", "Help.", Labels("component"))

  override def run(args: List[String]) =
    myAppLogic.provideCustomLayer(Registry.defaultRegistry >>> (counter2.register ++ counter1.register)).exitCode


  val myAppLogic: ZIO[counter2.Metric with counter1.Metric, Nothing, Unit] =
    for {
      _ <- counter2.inc()
      _ <- counter1.inc(Labels("payment"))
    } yield ()
}
