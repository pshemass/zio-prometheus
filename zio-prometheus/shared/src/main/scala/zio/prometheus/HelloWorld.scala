package zio.prometheus

import zio.console._
import zio.prometheus.metrics.Gauge
import zio.{App, Chunk}

object HelloWorld extends App {

  object requestCounter extends Gauge("request_counter","hhh", Chunk("myService"))

  override def run(args: List[String]) =
    myAppLogic.provideCustomLayer(Registry.defaultRegistry >>> requestCounter.register).exitCode


  val myAppLogic =
    for {
      _ <- metrics.inc(requestCounter)
      _    <- putStrLn("Hello! What is your name?")
      name <- getStrLn
      _    <- putStrLn(s"Hello, $name, welcome to ZIO!")
    } yield ()
}
