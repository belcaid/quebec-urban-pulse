package qc.urbanpulse.components

import com.raquo.laminar.api.L.{*, given}
import qc.urbanpulse.models.{FilterOptions, PermitFilters}

object Filters:
  def view(
      optionsSignal: Signal[Option[FilterOptions]],
      filters: Var[PermitFilters],
      submitFilters: Observer[Unit]
  ): Element =
    form(
      cls := "filters",
      onSubmit.preventDefault.mapTo(()) --> submitFilters,
      label(
        "Année",
        select(
          value <-- filters.signal.map(_.year),
          onChange.mapToValue --> { value =>
            filters.update(current => current.copy(year = value))
          },
          children <-- optionsSignal.map { options =>
            option(value := "", "Toutes les années") ::
              options.toList.flatMap(_.years).map { year =>
                option(value := year.toString, year.toString)
              }
          }
        )
      ),
      label(
        "Arrondissement",
        select(
          value <-- filters.signal.map(_.arrondissement),
          onChange.mapToValue --> { value =>
            filters.update(current => current.copy(arrondissement = value))
          },
          children <-- optionsSignal.map { options =>
            option(value := "", "Tous les arrondissements") ::
              options.toList.flatMap(_.arrondissements).map { arrondissement =>
                option(value := arrondissement, arrondissement)
              }
          }
        )
      ),
      label(
        "Domaine",
        select(
          value <-- filters.signal.map(_.domaine),
          onChange.mapToValue --> { value =>
            filters.update(current => current.copy(domaine = value))
          },
          children <-- optionsSignal.map { options =>
            option(value := "", "Tous les domaines") ::
              options.toList.flatMap(_.domaines).map { domaine =>
                option(value := domaine, domaine)
              }
          }
        )
      ),
      label(
        "Type de permis",
        select(
          value <-- filters.signal.map(_.typePermis),
          onChange.mapToValue --> { value =>
            filters.update(current => current.copy(typePermis = value))
          },
          children <-- optionsSignal.map { options =>
            option(value := "", "Tous les types") ::
              options.toList.flatMap(_.typesPermis).map { permitType =>
                option(value := permitType, permitType)
            }
          }
        )
      ),
      div(
        cls := "filters-actions",
        button(cls := "filter-submit", typ := "submit", "Appliquer")
      )
    )
