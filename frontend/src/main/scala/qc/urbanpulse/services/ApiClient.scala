package qc.urbanpulse.services

import org.scalajs.dom
import qc.urbanpulse.models.{CountByValue, CountByYear, DataQualityMetric, FilterOptions, Permit, PermitFilters, PermitRelation, SummaryStats}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.Thenable.Implicits.*

object ApiClient:
  private val BaseUrl = "http://localhost:8080"

  def fetchSummaryStats(): Future[SummaryStats] =
    fetchJson("/api/stats/summary").map { data =>
      SummaryStats(
        totalPermits = number(data, "totalPermits").toLong,
        firstYear = number(data, "firstYear").toInt,
        lastYear = number(data, "lastYear").toInt,
        arrondissementCount = number(data, "arrondissementCount").toLong,
        domaineCount = number(data, "domaineCount").toLong,
        raisonCount = number(data, "raisonCount").toLong,
        typePermisCount = number(data, "typePermisCount").toLong
      )
    }

  def fetchPermits(filters: PermitFilters = PermitFilters(), limit: Int): Future[List[Permit]] =
    fetchJson(permitsPath(filters, limit)).map { data =>
      data.asInstanceOf[js.Array[js.Dynamic]].toList.map(readPermit)
    }

  def fetchFilterOptions(): Future[FilterOptions] =
    fetchJson("/api/filters").map { data =>
      FilterOptions(
        years = numbers(data, "years").map(_.toInt),
        arrondissements = texts(data, "arrondissements"),
        domaines = texts(data, "domaines"),
        typesPermis = texts(data, "typesPermis"),
        raisons = texts(data, "raisons")
      )
    }

  def fetchPermitsByYear(): Future[List[CountByYear]] =
    fetchJson("/api/stats/by-year").map { data =>
      data.asInstanceOf[js.Array[js.Dynamic]].toList.map { item =>
        CountByYear(
          year = number(item, "year").toInt,
          count = number(item, "count").toLong
        )
      }
    }

  def fetchPermitsByMonth(): Future[List[CountByValue]] =
    fetchCountByValue("/api/stats/by-month")

  def fetchPermitsByBorough(): Future[List[CountByValue]] =
    fetchCountByValue("/api/stats/by-borough")

  def fetchPermitsByDomain(): Future[List[CountByValue]] =
    fetchCountByValue("/api/stats/by-domain")

  def fetchPermitsByType(): Future[List[CountByValue]] =
    fetchCountByValue("/api/stats/by-type")

  def fetchPermitsByReason(): Future[List[CountByValue]] =
    fetchCountByValue("/api/stats/by-reason")

  def fetchPermitRelations(limit: Int = 500): Future[List[PermitRelation]] =
    fetchJson(s"/api/stats/relations?limit=$limit").map { data =>
      data.asInstanceOf[js.Array[js.Dynamic]].toList.map { item =>
        PermitRelation(
          typePermis = text(item, "typePermis"),
          domaine = text(item, "domaine"),
          raison = text(item, "raison"),
          count = number(item, "count").toLong
        )
      }
    }

  def fetchDataQualitySummary(): Future[List[DataQualityMetric]] =
    fetchJson("/api/data-quality/summary").map { data =>
      data.asInstanceOf[js.Array[js.Dynamic]].toList.map { item =>
        DataQualityMetric(
          metric = text(item, "metric"),
          value = text(item, "value")
        )
      }
    }

  private def fetchJson(path: String): Future[js.Dynamic] =
    dom.fetch(BaseUrl + path).toFuture.flatMap { response =>
      if response.ok then
        response.text().toFuture.map(text => js.JSON.parse(text))
      else
        Future.failed(new RuntimeException(s"API error ${response.status}"))
    }

  private def fetchCountByValue(path: String): Future[List[CountByValue]] =
    fetchJson(path).map { data =>
      data.asInstanceOf[js.Array[js.Dynamic]].toList.map { item =>
        CountByValue(
          value = text(item, "value"),
          count = number(item, "count").toLong
        )
      }
    }

  private def number(data: js.Dynamic, field: String): Double =
    data.selectDynamic(field).asInstanceOf[Double]

  private def numbers(data: js.Dynamic, field: String): List[Double] =
    data.selectDynamic(field).asInstanceOf[js.Array[Double]].toList

  private def texts(data: js.Dynamic, field: String): List[String] =
    data.selectDynamic(field).asInstanceOf[js.Array[String]].toList

  private def text(data: js.Dynamic, field: String): String =
    data.selectDynamic(field).asInstanceOf[String]

  private def optionalText(data: js.Dynamic, field: String): Option[String] =
    val value = data.selectDynamic(field)

    if js.isUndefined(value) || value == null then None
    else Some(value.asInstanceOf[String]).filter(_.nonEmpty)

  private def readPermit(data: js.Dynamic): Permit =
    Permit(
      id = number(data, "id").toLong,
      numeroPermis = text(data, "numeroPermis"),
      dateDelivrance = text(data, "dateDelivrance"),
      adresseTravaux = text(data, "adresseTravaux"),
      domaine = text(data, "domaine"),
      lotsImpactes = optionalText(data, "lotsImpactes"),
      typePermis = optionalText(data, "typePermis"),
      arrondissement = text(data, "arrondissement"),
      raison = optionalText(data, "raison"),
      longitude = number(data, "longitude"),
      latitude = number(data, "latitude")
    )

  private def permitsPath(filters: PermitFilters, limit: Int): String =
    val params = List(
      "limit" -> Some(limit.toString),
      "search" -> nonEmpty(filters.search),
      "year" -> nonEmpty(filters.year),
      "arrondissement" -> nonEmpty(filters.arrondissement),
      "domaine" -> nonEmpty(filters.domaine),
      "type_permis" -> nonEmpty(filters.typePermis),
      "raison" -> nonEmpty(filters.raison)
    ).flatMap { case (key, value) =>
      value.map(v => s"$key=${encode(v)}")
    }

    s"/api/permits?${params.mkString("&")}"

  private def nonEmpty(value: String): Option[String] =
    Option(value.trim).filter(_.nonEmpty)

  private def encode(value: String): String =
    js.URIUtils.encodeURIComponent(value)
