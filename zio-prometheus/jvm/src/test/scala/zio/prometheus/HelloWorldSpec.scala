package zio.prometheus

import zio.test.Assertion._
import zio.test._
import zio.test.environment._

object HelloWorldSpec extends DefaultRunnableSpec {

  def spec: ZSpec[Environment, Failure] = suite("HelloWorldSpec")(
    testM("sayHello correctly displays output") {
      for {
        output <- TestConsole.output
      } yield assert(output)(equalTo(Vector("Hello, World!\n")))
    }
  )
}
