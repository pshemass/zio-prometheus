package zio.prometheus

import io.prometheus.client.CollectorRegistry
import io.prometheus.client.hotspot.DefaultExports
import zio.{ Has, ZLayer }

object JvmMetrics {
  val register: ZLayer[Has[CollectorRegistry], Nothing, Has[Unit]] =
    ZLayer.fromService((registry: CollectorRegistry) => DefaultExports.register(registry))
}
