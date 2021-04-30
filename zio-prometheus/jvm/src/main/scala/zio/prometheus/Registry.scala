package zio.prometheus

import io.prometheus.client.CollectorRegistry
import zio.{Has, ZLayer}

object Registry {
  val defaultRegistry: ZLayer[Any, Nothing, Has[CollectorRegistry]] =
    ZLayer.succeed(CollectorRegistry.defaultRegistry)
}
