package qc.urbanpulse.api

import cats.effect.IO
import io.circe.generic.auto.*
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.io.*
import qc.urbanpulse.repositories.StatsRepository

object StatsRoutes:

  object LimitQueryParam extends OptionalQueryParamDecoderMatcher[Int]("limit")
  object OffsetQueryParam extends OptionalQueryParamDecoderMatcher[Int]("offset")

  val routes: HttpRoutes[IO] = HttpRoutes.of[IO]:
    case GET -> Root / "api" / "stats" / "summary" =>
      IO.blocking(StatsRepository.summary()).flatMap(Ok(_))

    case GET -> Root / "api" / "stats" / "by-year" =>
      IO.blocking(StatsRepository.byYear()).flatMap(Ok(_))

    case GET -> Root / "api" / "stats" / "by-borough" =>
      IO.blocking(StatsRepository.byColumn("arrondissement")).flatMap(Ok(_))

    case GET -> Root / "api" / "stats" / "by-domain" =>
      IO.blocking(StatsRepository.byColumn("domaine")).flatMap(Ok(_))

    case GET -> Root / "api" / "data-quality" / "summary" =>
      IO.blocking(StatsRepository.dataQualitySummary()).flatMap(Ok(_))

    case GET -> Root / "api" / "data-quality" / "issues" :?
        LimitQueryParam(limit) +&
        OffsetQueryParam(offset) =>
      IO.blocking {
        StatsRepository.dataQualityIssues(limit.getOrElse(200).min(1000).max(1), offset.getOrElse(0).max(0))
      }.flatMap(Ok(_))
