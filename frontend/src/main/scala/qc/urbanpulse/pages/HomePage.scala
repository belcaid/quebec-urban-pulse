package qc.urbanpulse.pages

import com.raquo.laminar.api.L.{*, given}
import qc.urbanpulse.animations.PageAnimations
import qc.urbanpulse.components.StatsCard
import qc.urbanpulse.models.SummaryStats
import qc.urbanpulse.services.ApiClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object HomePage:
  def view: Element =
    val stats = Var[Option[SummaryStats]](None)
    val error = Var[Option[String]](None)

    mainTag(
      cls := "home-page page-animate",
      onMountCallback { _ =>
        PageAnimations.animatePageItems(".home-page")

        ApiClient.fetchSummaryStats().onComplete {
          case Success(value) =>
            stats.set(Some(value))
            error.set(None)
          case Failure(_) =>
            stats.set(None)
            error.set(Some("Impossible de charger les statistiques depuis l'API."))
        }
      },
      sectionTag(
        cls := "hero",
        div(
          cls := "hero-copy animate-item",
          h1(
            "Explorer l'évolution urbaine de ",
            span("Québec.")
          ),
          p(
            "Québec Urban Pulse transforme les permis délivrés par la Ville de Québec en une expérience interactive pour analyser les lieux, les tendances et les transformations du territoire."
          ),
          div(
            cls := "hero-actions animate-item",
            a(cls := "primary-button", href := "#/map", "Explorer les permis"),
            a(cls := "secondary-button", href := "#/dashboard", "Comprendre les tendances")
          )
        )
      ),
      sectionTag(
        idAttr := "stats",
        cls := "stats-row animate-item",
        children <-- stats.signal.map {
          case Some(summary) =>
            List(
              StatsCard.view(formatNumber(summary.totalPermits), "permis analysés"),
              StatsCard.view(s"${summary.firstYear}-${summary.lastYear}", "années couvertes"),
              StatsCard.view(formatNumber(summary.arrondissementCount), "arrondissements"),
              StatsCard.view(formatNumber(summary.domaineCount), "domaines")
            )
          case None =>
            List(
              StatsCard.view("...", "permis analysés"),
              StatsCard.view("...", "années couvertes"),
              StatsCard.view("...", "arrondissements"),
              StatsCard.view("...", "domaines")
            )
        }
      ),
      div(
        cls := "api-error",
        child.text <-- error.signal.map(_.getOrElse(""))
      )
    )

  private def formatNumber(value: Long): String =
    "%,d".format(value).replace(",", " ")
