package qc.urbanpulse.models

final case class DataQualityIssue(
    issueType: String,
    fieldName: Option[String],
    permitNumber: Option[String],
    dateDelivrance: Option[String],
    address: Option[String],
    detail: String
)
