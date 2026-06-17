package qc.urbanpulse.components

import com.raquo.laminar.api.L.{*, given}

object Footer:
  def view: Element =
    footerTag(
      cls := "footer",
      "Données ouvertes de la Ville de Québec. Application construite avec Scala.js et Laminar."
    )
