package qc.urbanpulse.models

final case class PermitRaw(
    numeroPermis: String,
    dateDelivrance: String,
    adresseTravaux: String,
    domaine: String,
    lotsImpactes: String,
    typePermis: String,
    arrondissement: String,
    raison: String,
    longitude: String,
    latitude: String
)
