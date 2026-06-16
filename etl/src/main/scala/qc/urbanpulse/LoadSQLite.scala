package qc.urbanpulse

import java.sql.{Connection, DriverManager}

object LoadSQLite:

  private val DatabasePath = "../data/sqlite/quebec_urban_pulse.db"
  private val DatabaseUrl = s"jdbc:sqlite:$DatabasePath"
  private val CleanCsvPath = "../data/processed/permits_clean.csv"
  private val QualitySummaryPath = "../data/processed/data_quality_summary.csv"
  private val QualityIssuesPath = "../data/processed/data_quality_issues.csv"

  def run(): Unit =
    val connection = DriverManager.getConnection(DatabaseUrl)

    try
      createTables(connection)
      createIndexes(connection)

      clearTables(connection)

      val permits = ImportPermits.readCsv(CleanCsvPath)
      val qualitySummary = ImportPermits.readCsv(QualitySummaryPath)
      val qualityIssues = ImportPermits.readCsv(QualityIssuesPath)

      insertPermits(connection, permits)
      insertQualitySummary(connection, qualitySummary)
      insertQualityIssues(connection, qualityIssues)

      println("Chargement SQLite terminé")
      println(s"- Permis insérés : ${permits.rows.size}")
      println(s"- Métriques qualité insérées : ${qualitySummary.rows.size}")
      println(s"- Anomalies qualité insérées : ${qualityIssues.rows.size}")
      println(s"- Base SQLite : $DatabasePath")
    finally
      connection.close()

  private def createTables(connection: Connection): Unit =
    execute(
      connection,
      """CREATE TABLE IF NOT EXISTS permits (
        |  id INTEGER PRIMARY KEY AUTOINCREMENT,
        |  numero_permis TEXT NOT NULL UNIQUE,
        |  date_delivrance TEXT NOT NULL,
        |  adresse_travaux TEXT NOT NULL,
        |  domaine TEXT NOT NULL,
        |  lots_impactes TEXT,
        |  type_permis TEXT NOT NULL,
        |  arrondissement TEXT NOT NULL,
        |  raison TEXT,
        |  longitude REAL NOT NULL,
        |  latitude REAL NOT NULL
        |)""".stripMargin
    )

    execute(
      connection,
      """CREATE TABLE IF NOT EXISTS data_quality_summary (
        |  metric TEXT PRIMARY KEY,
        |  value TEXT NOT NULL
        |)""".stripMargin
    )

    execute(
      connection,
      """CREATE TABLE IF NOT EXISTS data_quality_issues (
        |  id INTEGER PRIMARY KEY AUTOINCREMENT,
        |  issue_type TEXT NOT NULL,
        |  field_name TEXT,
        |  permit_number TEXT,
        |  date_delivrance TEXT,
        |  address TEXT,
        |  detail TEXT NOT NULL
        |)""".stripMargin
    )

  private def createIndexes(connection: Connection): Unit =
    execute(connection, "CREATE INDEX IF NOT EXISTS idx_permits_date_delivrance ON permits(date_delivrance)")
    execute(connection, "CREATE INDEX IF NOT EXISTS idx_permits_arrondissement ON permits(arrondissement)")
    execute(connection, "CREATE INDEX IF NOT EXISTS idx_permits_domaine ON permits(domaine)")
    execute(connection, "CREATE INDEX IF NOT EXISTS idx_permits_raison ON permits(raison)")
    execute(connection, "CREATE INDEX IF NOT EXISTS idx_permits_type_permis ON permits(type_permis)")
    execute(connection, "CREATE INDEX IF NOT EXISTS idx_quality_issue_type ON data_quality_issues(issue_type)")

  private def clearTables(connection: Connection): Unit =
    execute(connection, "DELETE FROM permits")
    execute(connection, "DELETE FROM data_quality_summary")
    execute(connection, "DELETE FROM data_quality_issues")

  private def insertPermits(connection: Connection, permits: ImportedPermits): Unit =
    val sql =
      """INSERT INTO permits (
        |  numero_permis,
        |  date_delivrance,
        |  adresse_travaux,
        |  domaine,
        |  lots_impactes,
        |  type_permis,
        |  arrondissement,
        |  raison,
        |  longitude,
        |  latitude
        |) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""".stripMargin

    val statement = connection.prepareStatement(sql)

    try
      permits.rows.foreach { row =>
        statement.setString(1, permits.value(row, "NUMERO_PERMIS"))
        statement.setString(2, permits.value(row, "DATE_DELIVRANCE"))
        statement.setString(3, permits.value(row, "ADRESSE_TRAVAUX"))
        statement.setString(4, permits.value(row, "DOMAINE"))
        setNullableString(statement, 5, permits.value(row, "LOTS_IMPACTES"))
        statement.setString(6, permits.value(row, "TYPE_PERMIS"))
        statement.setString(7, permits.value(row, "ARRONDISSEMENT"))
        setNullableString(statement, 8, permits.value(row, "RAISON"))
        statement.setDouble(9, permits.value(row, "LONGITUDE").toDouble)
        statement.setDouble(10, permits.value(row, "LATITUDE").toDouble)
        statement.addBatch()
      }

      statement.executeBatch()
    finally
      statement.close()

  private def insertQualitySummary(connection: Connection, summary: ImportedPermits): Unit =
    val sql = "INSERT INTO data_quality_summary (metric, value) VALUES (?, ?)"
    val statement = connection.prepareStatement(sql)

    try
      summary.rows.foreach { row =>
        statement.setString(1, summary.value(row, "metric"))
        statement.setString(2, summary.value(row, "value"))
        statement.addBatch()
      }

      statement.executeBatch()
    finally
      statement.close()

  private def insertQualityIssues(connection: Connection, issues: ImportedPermits): Unit =
    val sql =
      """INSERT INTO data_quality_issues (
        |  issue_type,
        |  field_name,
        |  permit_number,
        |  date_delivrance,
        |  address,
        |  detail
        |) VALUES (?, ?, ?, ?, ?, ?)""".stripMargin

    val statement = connection.prepareStatement(sql)

    try
      issues.rows.foreach { row =>
        statement.setString(1, issues.value(row, "issue_type"))
        setNullableString(statement, 2, issues.value(row, "field_name"))
        setNullableString(statement, 3, issues.value(row, "permit_number"))
        setNullableString(statement, 4, issues.value(row, "date_delivrance"))
        setNullableString(statement, 5, issues.value(row, "address"))
        statement.setString(6, issues.value(row, "detail"))
        statement.addBatch()
      }

      statement.executeBatch()
    finally
      statement.close()

  private def execute(connection: Connection, sql: String): Unit =
    val statement = connection.createStatement()

    try
      statement.execute(sql)
    finally
      statement.close()

  private def setNullableString(
      statement: java.sql.PreparedStatement,
      index: Int,
      value: String
  ): Unit =
    if value.trim.isEmpty then
      statement.setNull(index, java.sql.Types.VARCHAR)
    else
      statement.setString(index, value)
