package qc.urbanpulse.models

final case class PermitStats(
    totalPermits: Int,
    years: List[Int],
    arrondissementCount: Int,
    domaineCount: Int,
    raisonCount: Int,
    typePermisCount: Int
)
