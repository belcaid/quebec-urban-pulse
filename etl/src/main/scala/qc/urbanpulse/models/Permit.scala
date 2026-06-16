package qc.urbanpulse.models

import java.time.LocalDate

final case class Permit(
    numeroPermis: String,
    dateDelivrance: LocalDate,
    adresseTravaux: String,
    domaine: String,
    lotsImpactes: Option[String],
    typePermis: String,
    arrondissement: String,
    raison: Option[String],
    longitude: Double,
    latitude: Double
)
