package zio.prometheus

import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.HTTPServer
import zio.{ Has, ZLayer, ZManaged }

import java.net.InetSocketAddress

object HttpExporter {
  def live(port: Int): ZLayer[Any with Has[CollectorRegistry], Throwable, Has[HTTPServer]] =
    ZLayer.fromServiceManaged((registry: CollectorRegistry) =>
      ZManaged.makeEffect(new HTTPServer(new InetSocketAddress(port), registry, true))(_.stop())
    )
}
