package qc.urbanpulse.models

final case class Permit(
    id: Long,
    numeroPermis: String,
    dateDelivrance: String,
    adresseTravaux: String,
    domaine: String,
    lotsImpactes: Option[String],
    typePermis: Option[String],
    arrondissement: String,
    raison: Option[String],
    longitude: Double,
    latitude: Double
)

final case class FilterOptions(
    years: List[Int],
    arrondissements: List[String],
    domaines: List[String],
    typesPermis: List[String],
    raisons: List[String]
)

final case class PermitFilters(
    search: String = "",
    year: String = "",
    arrondissement: String = "",
    domaine: String = "",
    typePermis: String = "",
    raison: String = ""
)
