package qc.urbanpulse.models

final case class SummaryStats(
    totalPermits: Long,
    firstYear: Int,
    lastYear: Int,
    arrondissementCount: Long,
    domaineCount: Long,
    raisonCount: Long,
    typePermisCount: Long
)

final case class CountByYear(year: Int, count: Long)

final case class CountByValue(value: String, count: Long)

final case class PermitRelation(typePermis: String, domaine: String, raison: String, count: Long)

final case class DataQualityMetric(metric: String, value: String)

final case class DataQualityIssue(
    id: Long,
    issueType: String,
    fieldName: Option[String],
    permitNumber: Option[String],
    dateDelivrance: Option[String],
    address: Option[String],
    detail: String
)
