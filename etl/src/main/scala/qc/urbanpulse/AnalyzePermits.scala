package qc.urbanpulse

import java.time.LocalDate
import scala.collection.mutable
import scala.util.Try

object AnalyzePermits:

  def run(): Unit =
    val permits = ImportPermits.readCsv()

    println(s"Nombre de permis : ${permits.rows.size}")

    printMissingValues(permits)
    printYearDistribution(permits)
    printDistribution(permits, "ARRONDISSEMENT")
    printDistribution(permits, "DOMAINE")
    printDistribution(permits, "RAISON", limit = 20)
    printDistribution(permits, "TYPE_PERMIS")
    checkCoordinates(permits)
    checkDuplicates(permits)

  def missingValues(permits: ImportedPermits): List[(String, Int)] =
    permits.headers.map { column =>
      val count =
        permits.rows.count(row => permits.value(row, column).trim.isEmpty)

      (column, count)
    }

  def missingValueLocations(
      permits: ImportedPermits,
      excludedColumns: Set[String] = Set.empty
  ): List[(List[String], String)] =
    permits.rows.flatMap { row =>
      permits.headers
        .filterNot(excludedColumns.contains)
        .filter(column => permits.value(row, column).trim.isEmpty)
        .map(column => (row, column))
    }

  def yearDistribution(permits: ImportedPermits): List[(Int, Int)] =
    validDates(permits)
      .groupBy(_.getYear)
      .view
      .mapValues(_.size)
      .toList
      .sortBy(_._1)

  def distribution(
      permits: ImportedPermits,
      column: String
  ): List[(String, Int)] =
    permits.rows
      .map(row => permits.value(row, column).trim)
      .filter(_.nonEmpty)
      .groupBy(identity)
      .view
      .mapValues(_.size)
      .toList
      .sortBy { case (value, count) => (-count, value) }

  def rowsWithInvalidDates(permits: ImportedPermits): List[List[String]] =
    permits.rows.filter(row => hasInvalidDate(permits, row))

  def rowsWithInvalidCoordinates(permits: ImportedPermits): List[List[String]] =
    permits.rows.filterNot(row => hasValidCoordinates(permits, row))

  def splitDuplicateRows(
      rows: List[List[String]]
  ): (List[List[String]], List[List[String]]) =
    val seenRows = mutable.Set[List[String]]()
    val uniqueRows = mutable.ListBuffer[List[String]]()
    val duplicateRows = mutable.ListBuffer[List[String]]()

    rows.foreach { row =>
      if seenRows.contains(row) then
        duplicateRows += row
      else
        seenRows += row
        uniqueRows += row
    }

    (uniqueRows.toList, duplicateRows.toList)

  def duplicatePermitNumberGroups(permits: ImportedPermits): Int =
    permitNumbers(permits).groupBy(identity).values.count(_.size > 1)

  def conflictingPermitNumbers(permits: ImportedPermits): Int =
    permits.rows
      .filter(row => permits.value(row, "NUMERO_PERMIS").trim.nonEmpty)
      .groupBy(row => permits.value(row, "NUMERO_PERMIS").trim)
      .values
      .count(sameNumberRows => sameNumberRows.size > 1 && sameNumberRows.distinct.size > 1)

  def hasInvalidDate(permits: ImportedPermits, row: List[String]): Boolean =
    Try(LocalDate.parse(permits.value(row, "DATE_DELIVRANCE"))).isFailure

  def hasValidCoordinates(permits: ImportedPermits, row: List[String]): Boolean =
    val longitude = Try(permits.value(row, "LONGITUDE").toDouble).toOption
    val latitude = Try(permits.value(row, "LATITUDE").toDouble).toOption

    (longitude, latitude) match
      case (Some(lon), Some(lat)) =>
        lon >= -180 && lon <= 180 && lat >= -90 && lat <= 90
      case _ =>
        false

  private def printMissingValues(permits: ImportedPermits): Unit =
    println()
    println("Valeurs manquantes par colonne :")

    missingValues(permits).foreach { case (column, count) =>
      println(f"- $column%-20s : $count")
    }

  private def printYearDistribution(permits: ImportedPermits): Unit =
    val years = yearDistribution(permits)
    val invalidDateCount = rowsWithInvalidDates(permits).size

    println()
    println(s"Années disponibles : ${years.head._1} à ${years.last._1}")
    println(s"Dates invalides : $invalidDateCount")
    println("Répartition par année :")

    years.foreach { case (year, count) =>
      println(s"- $year : $count")
    }

  private def printDistribution(
      permits: ImportedPermits,
      column: String,
      limit: Int = Int.MaxValue
  ): Unit =
    val rows = distribution(permits, column)

    println()
    println(s"$column : ${rows.size} valeurs distinctes")

    rows.take(limit).foreach { case (value, count) =>
      println(s"- $value : $count")
    }

    if rows.size > limit then
      println(s"- ... ${rows.size - limit} autres valeurs")

  private def checkCoordinates(permits: ImportedPermits): Unit =
    val validCoordinates = permits.rows.collect {
      case row if hasValidCoordinates(permits, row) =>
        (
          permits.value(row, "LONGITUDE").toDouble,
          permits.value(row, "LATITUDE").toDouble
        )
    }

    val longitudes = validCoordinates.map(_._1)
    val latitudes = validCoordinates.map(_._2)

    println()
    println("Coordonnées GPS :")
    println(s"- Coordonnées invalides : ${rowsWithInvalidCoordinates(permits).size}")
    println(s"- Longitude : ${longitudes.min} à ${longitudes.max}")
    println(s"- Latitude : ${latitudes.min} à ${latitudes.max}")

  private def checkDuplicates(permits: ImportedPermits): Unit =
    val (_, duplicateRows) = splitDuplicateRows(permits.rows)

    println()
    println("Doublons :")
    println(s"- Lignes identiques supplémentaires : ${duplicateRows.size}")
    println(s"- Numéros de permis répétés : ${duplicatePermitNumberGroups(permits)}")
    println(s"- Numéros répétés avec des données différentes : ${conflictingPermitNumbers(permits)}")

  private def validDates(permits: ImportedPermits): List[LocalDate] =
    permits.rows.flatMap { row =>
      Try(LocalDate.parse(permits.value(row, "DATE_DELIVRANCE"))).toOption
    }

  private def permitNumbers(permits: ImportedPermits): List[String] =
    permits.rows
      .map(row => permits.value(row, "NUMERO_PERMIS").trim)
      .filter(_.nonEmpty)
