package qc.urbanpulse.components

import com.raquo.laminar.api.L.{*, given}

object StatsCard:
  def view(value: String, label: String): Element =
    articleTag(
      cls := "stats-card",
      strong(value),
      span(label)
    )
