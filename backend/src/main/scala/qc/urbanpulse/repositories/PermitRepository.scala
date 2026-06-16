package qc.urbanpulse.repositories

import qc.urbanpulse.db.Database
import qc.urbanpulse.models.{FilterOptions, Filters, Permit}

import java.sql.{Connection, PreparedStatement, ResultSet}
import scala.collection.mutable.ListBuffer

object PermitRepository:

  def findPermits(filters: Filters): List[Permit] =
    val connection = Database.connection()

    try
      val (whereClause, params) = buildWhereClause(filters)
      val sql =
        s"""SELECT
           |  id,
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
           |FROM permits
           |$whereClause
           |ORDER BY date_delivrance DESC, numero_permis
           |LIMIT ? OFFSET ?""".stripMargin

      val statement = connection.prepareStatement(sql)

      try
        bindParams(statement, params)
        statement.setInt(params.size + 1, filters.limit)
        statement.setInt(params.size + 2, filters.offset)

        val resultSet = statement.executeQuery()
        val permits = ListBuffer[Permit]()

        while resultSet.next() do
          permits += readPermit(resultSet)

        permits.toList
      finally
        statement.close()
    finally
      connection.close()

  def filterOptions(): FilterOptions =
    val connection = Database.connection()

    try
      FilterOptions(
        years = distinctYears(connection),
        arrondissements = distinctValues(connection, "arrondissement"),
        domaines = distinctValues(connection, "domaine"),
        typesPermis = distinctValues(connection, "type_permis"),
        raisons = distinctValues(connection, "raison")
      )
    finally
      connection.close()

  private def buildWhereClause(filters: Filters): (String, List[String]) =
    val clauses = ListBuffer[String]()
    val params = ListBuffer[String]()

    filters.year.foreach { year =>
      clauses += "strftime('%Y', date_delivrance) = ?"
      params += year.toString
    }

    filters.arrondissement.foreach { value =>
      clauses += "arrondissement = ?"
      params += value
    }

    filters.domaine.foreach { value =>
      clauses += "domaine = ?"
      params += value
    }

    filters.typePermis.foreach { value =>
      clauses += "type_permis = ?"
      params += value
    }

    filters.raison.foreach { value =>
      clauses += "raison = ?"
      params += value
    }

    filters.search.foreach { value =>
      clauses += "(adresse_travaux LIKE ? OR numero_permis LIKE ?)"
      params += s"%$value%"
      params += s"%$value%"
    }

    if clauses.isEmpty then
      ("", params.toList)
    else
      (s"WHERE ${clauses.mkString(" AND ")}", params.toList)

  private def bindParams(statement: PreparedStatement, params: List[String]): Unit =
    params.zipWithIndex.foreach { case (value, index) =>
      statement.setString(index + 1, value)
    }

  private def readPermit(resultSet: ResultSet): Permit =
    Permit(
      id = resultSet.getLong("id"),
      numeroPermis = resultSet.getString("numero_permis"),
      dateDelivrance = resultSet.getString("date_delivrance"),
      adresseTravaux = resultSet.getString("adresse_travaux"),
      domaine = resultSet.getString("domaine"),
      lotsImpactes = optionalString(resultSet, "lots_impactes"),
      typePermis = optionalString(resultSet, "type_permis"),
      arrondissement = resultSet.getString("arrondissement"),
      raison = optionalString(resultSet, "raison"),
      longitude = resultSet.getDouble("longitude"),
      latitude = resultSet.getDouble("latitude")
    )

  private def distinctYears(connection: Connection): List[Int] =
    val statement =
      connection.prepareStatement("SELECT DISTINCT strftime('%Y', date_delivrance) AS year FROM permits ORDER BY year")

    try
      val resultSet = statement.executeQuery()
      val values = ListBuffer[Int]()

      while resultSet.next() do
        values += resultSet.getString("year").toInt

      values.toList
    finally
      statement.close()

  private def distinctValues(connection: Connection, column: String): List[String] =
    val statement =
      connection.prepareStatement(s"SELECT DISTINCT $column AS value FROM permits WHERE $column IS NOT NULL AND $column != '' ORDER BY value")

    try
      val resultSet = statement.executeQuery()
      val values = ListBuffer[String]()

      while resultSet.next() do
        values += resultSet.getString("value")

      values.toList
    finally
      statement.close()

  private def optionalString(resultSet: ResultSet, column: String): Option[String] =
    Option(resultSet.getString(column)).filter(_.nonEmpty)
