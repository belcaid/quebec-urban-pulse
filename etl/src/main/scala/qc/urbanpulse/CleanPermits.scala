package qc.urbanpulse

import com.github.tototoshi.csv.CSVWriter

import java.io.File

object CleanPermits:

  private val CleanCsvPath = "../data/processed/permits_clean.csv"
  private val QualitySummaryPath = "../data/processed/data_quality_summary.csv"
  private val QualityIssuesPath = "../data/processed/data_quality_issues.csv"

  def run(): Unit =
    val rawPermits = ImportPermits.readCsv()
    val (uniqueRows, duplicateRows) = AnalyzePermits.splitDuplicateRows(rawPermits.rows)
    val uniquePermits = rawPermits.copy(rows = uniqueRows)

    val missingPermitNumberRows =
      uniqueRows.filter(row => rawPermits.value(row, "NUMERO_PERMIS").trim.isEmpty)
    val invalidDateRows =
      AnalyzePermits.rowsWithInvalidDates(uniquePermits)
    val invalidCoordinateRows =
      AnalyzePermits.rowsWithInvalidCoordinates(uniquePermits)
    val rejectedRows =
      (missingPermitNumberRows ++ invalidDateRows ++ invalidCoordinateRows).toSet
    val cleanRows =
      uniqueRows.filterNot(rejectedRows.contains)

    val allIssues =
      issueRows(rawPermits, duplicateRows, "exact_duplicate", "", "Copie exacte supprimée") ++
        issueRows(rawPermits, missingPermitNumberRows, "missing_required_value", "NUMERO_PERMIS", "Permis exclu du fichier nettoyé") ++
        missingValueIssues(rawPermits) ++
        issueRows(rawPermits, invalidDateRows, "invalid_date", "DATE_DELIVRANCE", "Date invalide") ++
        issueRows(rawPermits, invalidCoordinateRows, "invalid_coordinates", "LONGITUDE,LATITUDE", "Coordonnées invalides")

    writeCsv(CleanCsvPath, rawPermits.headers, cleanRows)
    writeQualityIssues(allIssues)
    writeQualitySummary(rawPermits, cleanRows, duplicateRows, invalidDateRows, invalidCoordinateRows, allIssues)

    println("Nettoyage terminé")
    println(s"- Permis bruts : ${rawPermits.rows.size}")
    println(s"- Permis nettoyés : ${cleanRows.size}")
    println(s"- Doublons supprimés : ${duplicateRows.size}")
    println(s"- Permis sans numéro supprimés : ${missingPermitNumberRows.size}")
    println(s"- Dates invalides supprimées : ${invalidDateRows.size}")
    println(s"- Coordonnées invalides supprimées : ${invalidCoordinateRows.size}")
    println(s"- Fichier nettoyé : $CleanCsvPath")
    println(s"- Rapport qualité : $QualitySummaryPath")
    println(s"- Anomalies qualité : $QualityIssuesPath")

  private def issueRows(
      permits: ImportedPermits,
      rows: List[List[String]],
      issueType: String,
      fieldName: String,
      detail: String
  ): List[List[String]] =
    rows.map { row =>
      issueRow(permits, issueType, fieldName, row, detail)
    }

  private def missingValueIssues(
      permits: ImportedPermits
  ): List[List[String]] =
    AnalyzePermits
      .missingValueLocations(permits, excludedColumns = Set("NUMERO_PERMIS"))
      .map { case (row, column) =>
        issueRow(permits, "missing_optional_value", column, row, "Valeur absente dans le CSV brut")
      }

  private def writeCsv(
      path: String,
      headers: List[String],
      rows: List[List[String]]
  ): Unit =
    val writer = CSVWriter.open(new File(path))

    try
      writer.writeAll(headers :: rows)
    finally
      writer.close()

  private def writeQualityIssues(rows: List[List[String]]): Unit =
    val headers = List("issue_type", "field_name", "permit_number", "date_delivrance", "address", "detail")

    writeCsv(QualityIssuesPath, headers, rows)

  private def writeQualitySummary(
      rawPermits: ImportedPermits,
      cleanRows: List[List[String]],
      duplicateRows: List[List[String]],
      invalidDateRows: List[List[String]],
      invalidCoordinateRows: List[List[String]],
      issueRows: List[List[String]]
  ): Unit =
    val missingCounts = AnalyzePermits.missingValues(rawPermits)
    val missingCountsByColumn = missingCounts.toMap

    val summaryRows =
      List(
        List("raw_permits", rawPermits.rows.size.toString),
        List("clean_permits", cleanRows.size.toString),
        List("removed_exact_duplicates", duplicateRows.size.toString),
        List("removed_missing_permit_number", missingCountsByColumn("NUMERO_PERMIS").toString),
        List("removed_invalid_dates", invalidDateRows.size.toString),
        List("removed_invalid_coordinates", invalidCoordinateRows.size.toString),
        List("duplicate_permit_number_groups", AnalyzePermits.duplicatePermitNumberGroups(rawPermits).toString),
        List("quality_issues", issueRows.size.toString)
      ) ++ missingCounts.map { case (column, count) =>
        List(s"raw_missing_$column", count.toString)
      }

    writeCsv(QualitySummaryPath, List("metric", "value"), summaryRows)

  private def issueRow(
      permits: ImportedPermits,
      issueType: String,
      fieldName: String,
      row: List[String],
      detail: String
  ): List[String] =
    List(
      issueType,
      fieldName,
      permits.value(row, "NUMERO_PERMIS"),
      permits.value(row, "DATE_DELIVRANCE"),
      permits.value(row, "ADRESSE_TRAVAUX"),
      detail
    )
