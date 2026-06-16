package qc.urbanpulse.models

final case class PermitFilters(
    year: Option[Int] = None,
    arrondissement: Option[String] = None,
    domaine: Option[String] = None,
    typePermis: Option[String] = None,
    raison: Option[String] = None,
    search: Option[String] = None
)
