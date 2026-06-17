package qc.urbanpulse.components

import com.raquo.laminar.api.L.{*, given}
import qc.urbanpulse.models.Permit

object PermitCard:
  def view(permit: Permit, selected: Signal[Boolean], selectPermit: Observer[Permit]): Element =
    articleTag(
      cls := "permit-card",
      cls.toggle("is-selected") <-- selected,
      onClick.mapTo(permit) --> selectPermit,
      div(
        cls := "permit-card-top",
        span(cls := "permit-number", permit.numeroPermis),
        span(cls := "permit-date", permit.dateDelivrance)
      ),
      h3(permit.adresseTravaux),
      div(
        cls := "permit-card-tags",
        span(cls := "permit-chip", permit.arrondissement),
        span(cls := "permit-chip muted", permit.typePermis.getOrElse("Type non précisé"))
      ),
      p(cls := "permit-card-domain", permit.domaine),
      small(cls := "permit-card-reason", permit.raison.map(shorten).getOrElse("Raison non précisée"))
    )

  def view(permit: Permit): Element =
    view(permit, Val(false), Observer.empty)

  def view(number: String, address: String, detail: String): Element =
    articleTag(
      cls := "permit-card",
      strong(number),
      span(address),
      small(detail)
    )

  private def shorten(value: String): String =
    if value.length <= 94 then value
    else value.take(91).trim + "..."
