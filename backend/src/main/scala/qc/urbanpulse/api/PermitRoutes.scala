package qc.urbanpulse.api

import cats.effect.IO
import io.circe.generic.auto.*
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.io.*
import qc.urbanpulse.models.Filters
import qc.urbanpulse.repositories.PermitRepository

object PermitRoutes:

  object YearQueryParam extends OptionalQueryParamDecoderMatcher[Int]("year")
  object ArrondissementQueryParam extends OptionalQueryParamDecoderMatcher[String]("arrondissement")
  object DomaineQueryParam extends OptionalQueryParamDecoderMatcher[String]("domaine")
  object TypePermisQueryParam extends OptionalQueryParamDecoderMatcher[String]("type_permis")
  object RaisonQueryParam extends OptionalQueryParamDecoderMatcher[String]("raison")
  object SearchQueryParam extends OptionalQueryParamDecoderMatcher[String]("search")
  object LimitQueryParam extends OptionalQueryParamDecoderMatcher[Int]("limit")
  object OffsetQueryParam extends OptionalQueryParamDecoderMatcher[Int]("offset")

  val routes: HttpRoutes[IO] = HttpRoutes.of[IO]:
    case GET -> Root / "api" / "permits" :?
        YearQueryParam(year) +&
        ArrondissementQueryParam(arrondissement) +&
        DomaineQueryParam(domaine) +&
        TypePermisQueryParam(typePermis) +&
        RaisonQueryParam(raison) +&
        SearchQueryParam(search) +&
        LimitQueryParam(limit) +&
        OffsetQueryParam(offset) =>
      val filters = Filters(
        year = year,
        arrondissement = arrondissement,
        domaine = domaine,
        typePermis = typePermis,
        raison = raison,
        search = search,
        limit = limit.getOrElse(500).min(2000).max(1),
        offset = offset.getOrElse(0).max(0)
      )

      IO.blocking(PermitRepository.findPermits(filters)).flatMap(Ok(_))

    case GET -> Root / "api" / "filters" =>
      IO.blocking(PermitRepository.filterOptions()).flatMap(Ok(_))
