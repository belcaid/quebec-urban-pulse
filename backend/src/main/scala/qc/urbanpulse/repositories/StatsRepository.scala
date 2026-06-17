package qc.urbanpulse.repositories

import qc.urbanpulse.db.Database
import qc.urbanpulse.models.{CountByValue, CountByYear, DataQualityIssue, DataQualityMetric, PermitRelation, SummaryStats}

import java.sql.{Connection, ResultSet}
import scala.collection.mutable.ListBuffer

object StatsRepository:

  def summary(): SummaryStats =
    val connection = Database.connection()

    try
      SummaryStats(
        totalPermits = scalarLong(connection, "SELECT COUNT(*) FROM permits"),
        firstYear = scalarString(connection, "SELECT MIN(strftime('%Y', date_delivrance)) FROM permits").toInt,
        lastYear = scalarString(connection, "SELECT MAX(strftime('%Y', date_delivrance)) FROM permits").toInt,
        arrondissementCount = scalarLong(connection, "SELECT COUNT(DISTINCT arrondissement) FROM permits"),
        domaineCount = scalarLong(connection, "SELECT COUNT(DISTINCT domaine) FROM permits"),
        raisonCount = scalarLong(connection, "SELECT COUNT(DISTINCT raison) FROM permits WHERE raison IS NOT NULL"),
        typePermisCount = scalarLong(connection, "SELECT COUNT(DISTINCT type_permis) FROM permits WHERE type_permis IS NOT NULL AND type_permis != ''")
      )
    finally
      connection.close()

  def byYear(): List[CountByYear] =
    val connection = Database.connection()

    try
      val sql =
        """SELECT strftime('%Y', date_delivrance) AS year, COUNT(*) AS count
          |FROM permits
          |GROUP BY year
          |ORDER BY year""".stripMargin

      query(connection, sql) { resultSet =>
        CountByYear(resultSet.getString("year").toInt, resultSet.getLong("count"))
      }
    finally
      connection.close()

  def byMonth(): List[CountByValue] =
    val connection = Database.connection()

    try
      val sql =
        """SELECT strftime('%m', date_delivrance) AS value, COUNT(*) AS count
          |FROM permits
          |GROUP BY value
          |ORDER BY value""".stripMargin

      query(connection, sql) { resultSet =>
        CountByValue(resultSet.getString("value"), resultSet.getLong("count"))
      }
    finally
      connection.close()

  def byColumn(column: String): List[CountByValue] =
    val connection = Database.connection()

    try
      val sql =
        s"""SELECT $column AS value, COUNT(*) AS count
           |FROM permits
           |WHERE $column IS NOT NULL AND $column != ''
           |GROUP BY $column
           |ORDER BY count DESC, value""".stripMargin

      query(connection, sql) { resultSet =>
        CountByValue(resultSet.getString("value"), resultSet.getLong("count"))
      }
    finally
      connection.close()

  def relations(limit: Int): List[PermitRelation] =
    val connection = Database.connection()

    try
      val sql =
        """SELECT type_permis, domaine, raison, COUNT(*) AS count
          |FROM permits
          |WHERE type_permis IS NOT NULL AND type_permis != ''
          |  AND domaine IS NOT NULL AND domaine != ''
          |  AND raison IS NOT NULL AND raison != ''
          |GROUP BY type_permis, domaine, raison
          |ORDER BY count DESC
          |LIMIT ?""".stripMargin

      val statement = connection.prepareStatement(sql)

      try
        statement.setInt(1, limit)

        val resultSet = statement.executeQuery()
        val values = ListBuffer[PermitRelation]()

        while resultSet.next() do
          values += PermitRelation(
            typePermis = resultSet.getString("type_permis"),
            domaine = resultSet.getString("domaine"),
            raison = resultSet.getString("raison"),
            count = resultSet.getLong("count")
          )

        values.toList
      finally
        statement.close()
    finally
      connection.close()

  def dataQualitySummary(): List[DataQualityMetric] =
    val connection = Database.connection()

    try
      query(connection, "SELECT metric, value FROM data_quality_summary ORDER BY metric") { resultSet =>
        DataQualityMetric(resultSet.getString("metric"), resultSet.getString("value"))
      }
    finally
      connection.close()

  def dataQualityIssues(limit: Int, offset: Int): List[DataQualityIssue] =
    val connection = Database.connection()

    try
      val sql =
        """SELECT id, issue_type, field_name, permit_number, date_delivrance, address, detail
          |FROM data_quality_issues
          |ORDER BY id
          |LIMIT ? OFFSET ?""".stripMargin

      val statement = connection.prepareStatement(sql)

      try
        statement.setInt(1, limit)
        statement.setInt(2, offset)

        val resultSet = statement.executeQuery()
        val issues = ListBuffer[DataQualityIssue]()

        while resultSet.next() do
          issues += DataQualityIssue(
            id = resultSet.getLong("id"),
            issueType = resultSet.getString("issue_type"),
            fieldName = optionalString(resultSet, "field_name"),
            permitNumber = optionalString(resultSet, "permit_number"),
            dateDelivrance = optionalString(resultSet, "date_delivrance"),
            address = optionalString(resultSet, "address"),
            detail = resultSet.getString("detail")
          )

        issues.toList
      finally
        statement.close()
    finally
      connection.close()

  private def scalarLong(connection: Connection, sql: String): Long =
    val statement = connection.prepareStatement(sql)

    try
      val resultSet = statement.executeQuery()
      resultSet.next()
      resultSet.getLong(1)
    finally
      statement.close()

  private def scalarString(connection: Connection, sql: String): String =
    val statement = connection.prepareStatement(sql)

    try
      val resultSet = statement.executeQuery()
      resultSet.next()
      resultSet.getString(1)
    finally
      statement.close()

  private def query[A](connection: Connection, sql: String)(read: ResultSet => A): List[A] =
    val statement = connection.prepareStatement(sql)

    try
      val resultSet = statement.executeQuery()
      val values = ListBuffer[A]()

      while resultSet.next() do
        values += read(resultSet)

      values.toList
    finally
      statement.close()

  private def optionalString(resultSet: ResultSet, column: String): Option[String] =
    Option(resultSet.getString(column)).filter(_.nonEmpty)
