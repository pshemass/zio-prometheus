package zio.prometheus

import zio.App
import zio.console._
import zio.prometheus.metrics.{Gauge, Labels}

object HelloWorld extends App {

  object requestCounter extends Gauge("request_counter","hhh", Labels.Empty)

  override def run(args: List[String]) =
    myAppLogic.provideCustomLayer(Registry.defaultRegistry >>> requestCounter.register).exitCode


  val myAppLogic =
    for {
      _ <- requestCounter.inc(Labels.Empty)
      _    <- putStrLn("Hello! What is your name?")
      name <- getStrLn
      _    <- putStrLn(s"Hello, $name, welcome to ZIO!")
    } yield ()
}
