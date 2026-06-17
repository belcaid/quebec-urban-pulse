package qc.urbanpulse.pages

import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom
import qc.urbanpulse.animations.PageAnimations
import qc.urbanpulse.components.{Filters, PermitCard}
import qc.urbanpulse.models.{FilterOptions, Permit, PermitFilters}
import qc.urbanpulse.services.ApiClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.scalajs.js.JSConverters.*
import scala.util.{Failure, Success}

object MapPage:
  private val QuebecCenter = js.Array(46.8139, -71.2080)
  private var currentMap: Option[js.Dynamic] = None
  private var markerCluster: Option[js.Dynamic] = None
  private var permitMarkers: Map[Long, js.Dynamic] = Map.empty
  private var selectedMarkerId: Option[Long] = None

  def view: Element =
    val permits = Var[List[Permit]](Nil)
    val filterOptions = Var[Option[FilterOptions]](None)
    val filters = Var(PermitFilters())
    val showFilters = Var(false)
    val isMapExpanded = Var(false)
    val isLoading = Var(false)
    val error = Var[Option[String]](None)
    val selectedPermitId = Var[Option[Long]](None)
    val submitFilters = EventBus[Unit]()
    val selectPermit = Observer[Permit] { permit =>
      selectedPermitId.set(Some(permit.id))
      focusPermit(permit)
    }

    def loadPermits(): Unit =
      isLoading.set(true)

      ApiClient.fetchPermits(filters = filters.now(), limit = 1000).onComplete {
        case Success(values) =>
          permits.set(values)
          selectedPermitId.set(None)
          error.set(None)
          isLoading.set(false)
          addPermitMarkers(values)
        case Failure(_) =>
          permits.set(Nil)
          error.set(Some("Impossible de charger les permis depuis l'API."))
          isLoading.set(false)
          clearPermitMarkers()
      }

    mainTag(
      cls := "map-page",
      onMountCallback { _ =>
        PageAnimations.animateMapLayout()

        ApiClient.fetchFilterOptions().onComplete {
          case Success(values) =>
            filterOptions.set(Some(values))
          case Failure(_) =>
            error.set(Some("Impossible de charger les filtres depuis l'API."))
        }

        loadPermits()
      },
      submitFilters.events --> { _ =>
        showFilters.set(false)
        loadPermits()
      },
      cls.toggle("is-map-expanded") <-- isMapExpanded.signal,
      sectionTag(
        cls := "map-results-panel",
        div(
          cls := "map-search-header",
          div(
            cls := "map-search-box",
            input(
              typ := "search",
              placeholder := "Adresse ou numéro de permis",
              value <-- filters.signal.map(_.search),
              onInput.mapToValue --> { value =>
                filters.update(current => current.copy(search = value))
              }
            ),
            button(
              cls := "map-search-button",
              typ := "button",
              aria.label := "Rechercher",
              onClick.mapTo(()) --> submitFilters.writer,
              searchIcon
            )
          ),
          button(
            cls := "map-filter-toggle",
            typ := "button",
            onClick --> (_ => showFilters.update(current => !current)),
            "Filtres"
          )
        ),
        div(
          cls := "map-filters-panel",
          display <-- showFilters.signal.map(if _ then "block" else "none"),
          Filters.view(filterOptions.signal, filters, submitFilters.writer)
        ),
        div(
          cls := "map-results-heading",
          h1("Permis à Québec"),
          p("Classement des résultats")
        ),
        div(
          cls := "api-error compact",
          child.text <-- error.signal.map(_.getOrElse(""))
        ),
        div(
          cls := "results-meta",
          child.text <-- permits.signal.combineWith(isLoading.signal).map { case (values, loading) =>
            if loading then "Chargement..."
            else s"${values.size} permis affichés"
          }
        ),
        div(
          cls := "map-results-scroll",
          div(
            cls := "results-grid",
            children <-- permits.signal.map { values =>
              values.take(20).map { permit =>
                PermitCard.view(
                  permit = permit,
                  selected = selectedPermitId.signal.map(_.contains(permit.id)),
                  selectPermit = selectPermit
                )
              }
            }
          )
        )
      ),
      sectionTag(
        cls := "map-canvas-panel",
        button(
          cls := "map-expand-button",
          typ := "button",
          aria.label <-- isMapExpanded.signal.map(expanded =>
            if expanded then "Réduire la carte" else "Agrandir la carte"
          ),
          onClick --> (_ =>
            isMapExpanded.update(current => !current)
            animateMapLayout()
          ),
          expandIcon
        ),
        div(
          cls := "map-panel",
          div(
            idAttr := "quebec-map",
            cls := "leaflet-map",
            onMountCallback(_ =>
              initMap()
              addPermitMarkers(permits.now())
            ),
            onUnmountCallback(_ => removeMap())
          )
        )
      )
    )

  private def initMap(): Unit =
    removeMap()

    val L = js.Dynamic.global.L
    val map = L
      .map(
        "quebec-map",
        js.Dynamic.literal(
          zoomControl = false
        )
      )
      .setView(QuebecCenter, 11)

    L.control
      .zoom(
        js.Dynamic.literal(
          position = "topright"
        )
      )
      .addTo(map)

    L.tileLayer(
      "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png",
      js.Dynamic.literal(
        maxZoom = 19,
        attribution = "&copy; OpenStreetMap contributors"
      )
    ).addTo(map)

    val cluster = L
      .markerClusterGroup(
        js.Dynamic.literal(
          showCoverageOnHover = false,
          maxClusterRadius = 48,
          iconCreateFunction = (cluster: js.Dynamic) => clusterIcon(cluster)
        )
      )
      .addTo(map)

    currentMap = Some(map)
    markerCluster = Some(cluster)

  private def removeMap(): Unit =
    clearPermitMarkers()
    markerCluster = None
    currentMap.foreach(_.remove())
    currentMap = None

  private def addPermitMarkers(permits: List[Permit]): Unit =
    markerCluster.foreach { cluster =>
      clearPermitMarkers()

      permitMarkers = permits.map { permit =>
        val marker = js.Dynamic.global.L
          .marker(
            js.Array(permit.latitude, permit.longitude),
            js.Dynamic.literal(
              icon = permitIcon(isSelected = false)
            )
          )
          .bindPopup(
            popupContent(permit),
            js.Dynamic.literal(
              className = "permit-popup",
              maxWidth = 286,
              minWidth = 250,
              autoPanPadding = js.Array(32, 32)
            )
          )

        cluster.addLayer(marker)
        permit.id -> marker
      }.toMap

      fitMapToPermits(permits)
    }

  private def clearPermitMarkers(): Unit =
    markerCluster.foreach(_.clearLayers())
    permitMarkers = Map.empty
    selectedMarkerId = None

  private def focusPermit(permit: Permit): Unit =
    selectedMarkerId.flatMap(permitMarkers.get).foreach { marker =>
      marker.setIcon(permitIcon(isSelected = false))
    }

    permitMarkers.get(permit.id).foreach { marker =>
      selectedMarkerId = Some(permit.id)
      marker.setIcon(permitIcon(isSelected = true))

      currentMap.foreach { map =>
        map.setView(
          js.Array(permit.latitude, permit.longitude),
          16,
          js.Dynamic.literal(animate = true)
        )
      }

      markerCluster match
        case Some(cluster) =>
          cluster.zoomToShowLayer(marker, () => marker.openPopup())
        case None =>
          marker.openPopup()
    }

  private def permitIcon(isSelected: Boolean): js.Dynamic =
    val className =
      if isSelected then "permit-marker is-selected"
      else "permit-marker"

    js.Dynamic.global.L.divIcon(
      js.Dynamic.literal(
        className = className,
        html = "<span></span>",
        iconSize = js.Array(24, 24),
        iconAnchor = js.Array(12, 12),
        popupAnchor = js.Array(0, -12)
      )
    )

  private def clusterIcon(cluster: js.Dynamic): js.Dynamic =
    val count = cluster.getChildCount().asInstanceOf[Int]

    js.Dynamic.global.L.divIcon(
      js.Dynamic.literal(
        className = "permit-cluster",
        html = s"<span>$count</span>",
        iconSize = js.Array(44, 44),
        iconAnchor = js.Array(22, 22)
      )
    )

  private def popupContent(permit: Permit): String =
    val typePermis = permit.typePermis.getOrElse("Non précisé")
    val raison = permit.raison.getOrElse("Non précisée")

    s"""<article class="permit-popup-card">
       |  <div class="permit-popup-header">
       |    <strong class="permit-popup-title">${escapeHtml(permit.numeroPermis)}</strong>
       |    <span class="permit-popup-date">${escapeHtml(permit.dateDelivrance)}</span>
       |  </div>
       |  <p class="permit-popup-address">${escapeHtml(permit.adresseTravaux)}</p>
       |  <div class="permit-popup-grid">
       |    ${popupRow("Domaine", permit.domaine)}
       |    ${popupRow("Type", typePermis)}
       |    ${popupRow("Arrondissement", permit.arrondissement)}
       |    ${popupRow("Raison", raison)}
       |  </div>
       |</article>""".stripMargin

  private def popupRow(label: String, value: String): String =
    s"""<div class="permit-popup-row">
       |  <span class="permit-popup-label">${escapeHtml(label)}</span>
       |  <span class="permit-popup-value">${escapeHtml(value)}</span>
       |</div>""".stripMargin

  private def escapeHtml(value: String): String =
    value
      .replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;")
      .replace("\"", "&quot;")
      .replace("'", "&#39;")

  private def fitMapToPermits(permits: List[Permit]): Unit =
    currentMap.foreach { map =>
      if permits.nonEmpty then
        val bounds = js.Dynamic.global.L.latLngBounds(
          permits.map(permit => js.Array(permit.latitude, permit.longitude)).toJSArray
        )

        map.fitBounds(
          bounds,
          js.Dynamic.literal(
            padding = js.Array(48, 48),
            maxZoom = 14
          )
        )
      else
        map.setView(QuebecCenter, 11)
    }

  private def animateMapLayout(): Unit =
    val gsap = js.Dynamic.global.gsap

    if !js.isUndefined(gsap) then
      gsap.fromTo(
        ".map-canvas-panel",
        js.Dynamic.literal(scale = 0.985),
        js.Dynamic.literal(scale = 1, duration = 0.32, ease = "power2.out")
      )

    dom.window.setTimeout(
      () => currentMap.foreach(_.invalidateSize()),
      340
    )

  private def expandIcon: SvgElement =
    svg.svg(
      svg.viewBox := "0 0 32 32",
      svg.width := "18",
      svg.height := "18",
      svg.stroke := "currentColor",
      svg.strokeWidth := "2.6",
      svg.strokeLineCap := "round",
      svg.fill := "none",
      svg.g(
        svg.path(svg.d := "m14 29h-10.2c-.4418278 0-.8-.3581722-.8-.8v-10.2"),
        svg.path(svg.d := "m4 28 10-10"),
        svg.g(
          svg.strokeLineJoin := "round",
          svg.path(svg.d := "m18 3h10c.5522847 0 1 .44771525 1 1v10"),
          svg.path(svg.d := "m18 14 11-11")
        )
      )
    )

  private def searchIcon: SvgElement =
    svg.svg(
      svg.viewBox := "0 0 32 32",
      svg.width := "18",
      svg.height := "18",
      svg.stroke := "currentColor",
      svg.strokeWidth := "2.7",
      svg.strokeLineCap := "round",
      svg.strokeLineJoin := "round",
      svg.fill := "none",
      svg.path(svg.d := "m20.666 20.666 10 10"),
      svg.path(
        svg.d := "m24.0002 12.6668c0 6.2593-5.0741 11.3334-11.3334 11.3334-6.2592 0-11.3333-5.0741-11.3333-11.3334 0-6.2592 5.0741-11.3333 11.3333-11.3333 6.2593 0 11.3334 5.0741 11.3334 11.3333z"
      )
    )
