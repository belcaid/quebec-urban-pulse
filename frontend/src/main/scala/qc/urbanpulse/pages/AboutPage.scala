package qc.urbanpulse.pages

import com.raquo.laminar.api.L.{*, given}
import qc.urbanpulse.animations.PageAnimations

object AboutPage:
  def view: Element =
    mainTag(
      cls := "page-section about-page page-animate",
      onMountCallback(_ => PageAnimations.animatePageItems(".about-page")),
      div(
        cls := "page-heading animate-item",
        h1("À propos"),
        p("Québec Urban Pulse explore les permis délivrés par la Ville de Québec à partir des données ouvertes publiques.")
      ),
      sectionTag(
        cls := "about-content animate-item",
        h2("Architecture"),
        p("Open Data → ETL Scala → SQLite → API REST → Frontend Scala.js."),
        h2("Source"),
        p("Les données proviennent du portail Données Québec et sont traitées localement avant d'être exposées par l'API."),
        h2("Objectif"),
        p("Construire une interface moderne pour explorer les transformations urbaines à travers la carte, les filtres et les indicateurs.")
      )
    )
