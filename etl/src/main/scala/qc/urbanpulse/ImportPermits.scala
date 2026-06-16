package qc.urbanpulse

import com.github.tototoshi.csv.CSVReader

import java.io.File

final case class ImportedPermits(headers: List[String], rows: List[List[String]]):
  private val headerIndex = headers.zipWithIndex.toMap

  def value(row: List[String], column: String): String =
    row.lift(headerIndex(column)).getOrElse("")

object ImportPermits:

  val RawCsvPath = "../data/raw/permits.csv"

  def readCsv(csvPath: String = RawCsvPath): ImportedPermits =
    val reader = CSVReader.open(new File(csvPath))

    try
      val rows = reader.all()
      val headers = cleanHeaders(rows.head)

      ImportedPermits(headers, rows.tail)
    finally
      reader.close()

  private def cleanHeaders(headers: List[String]): List[String] =
    headers.updated(0, headers.head.stripPrefix("\uFEFF"))
