package qc.urbanpulse.db

import java.sql.{Connection, DriverManager}

object Database:
  private val DatabasePath = "../data/sqlite/quebec_urban_pulse.db"
  private val DatabaseUrl = s"jdbc:sqlite:$DatabasePath"

  def connection(): Connection =
    DriverManager.getConnection(DatabaseUrl)
