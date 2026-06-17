package qc.urbanpulse

import cats.effect.{IO, IOApp}
import com.comcast.ip4s.{host, port}
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits.*
import org.http4s.server.Router
import org.http4s.server.middleware.CORS
import qc.urbanpulse.api.{HealthRoutes, PermitRoutes, StatsRoutes}

object Main extends IOApp.Simple:

  private val httpApp =
    CORS.httpApp(Router(
      "/" -> HealthRoutes.routes,
      "/" -> PermitRoutes.routes,
      "/" -> StatsRoutes.routes
    ).orNotFound)

  override def run: IO[Unit] =
    EmberServerBuilder
      .default[IO]
      .withHost(host"0.0.0.0")
      .withPort(port"8080")
      .withHttpApp(httpApp)
      .build
      .use(_ => IO.println("Backend running on http://localhost:8080") *> IO.never)
