package qc.urbanpulse.pages

import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom
import qc.urbanpulse.animations.PageAnimations
import qc.urbanpulse.components.StatsCard
import qc.urbanpulse.models.{CountByValue, CountByYear, PermitRelation, SummaryStats}
import qc.urbanpulse.services.ApiClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.scalajs.js.JSConverters.*
import scala.util.{Failure, Success}

object DashboardPage:
  private var charts: Map[String, js.Dynamic] = Map.empty
  private case class RelationData(
      types: List[CountByValue],
      domains: List[CountByValue],
      reasons: List[CountByValue],
      relations: List[PermitRelation]
  )
  private case class RelationNode(group: String, label: String, count: Long)

  def view: Element =
    val summary = Var[Option[SummaryStats]](None)
    val error = Var[Option[String]](None)
    val relationData = Var(RelationData(Nil, Nil, Nil, Nil))

    mainTag(
      cls := "page-section dashboard-page page-animate",
      onMountCallback { _ =>
        PageAnimations.animatePageItems(".dashboard-page")
        loadSummary(summary, error)
        dom.window.setTimeout(
          () => loadCharts(error, relationData),
          150
        )
      },
      onUnmountCallback { _ =>
        destroyCharts()
      },
      div(
        cls := "page-heading animate-item",
        h1("Comprendre"),
        p("Tendances, volumes et relations clés des permis délivrés par la Ville de Québec.")
      ),
      sectionTag(
        cls := "insight-panel animate-item",
        h2("Ce que les données racontent"),
        ul(
          li("Le volume progresse nettement après 2020, avec un pic observé en 2025. L'année 2026 doit être lue comme une année partielle."),
          li("L'activité est fortement saisonnière : les mois de mai à juillet concentrent le plus de permis."),
          li("Trois domaines dominent le dataset : rénovation/agrandissement, aménagement extérieur, et constructions accessoires."),
          li("Les certificats d'autorisation sont plus nombreux que les permis de construction, ce qui confirme un dataset très orienté interventions courantes.")
        )
      ),
      div(
        cls := "api-error compact",
        child.text <-- error.signal.map(_.getOrElse(""))
      ),
      sectionTag(
        cls := "dashboard-grid animate-item",
        children <-- summary.signal.map {
          case Some(stats) =>
            List(
              StatsCard.view(formatNumber(stats.totalPermits), "permis"),
              StatsCard.view(s"${stats.firstYear}-${stats.lastYear}", "période"),
              StatsCard.view(formatNumber(stats.arrondissementCount), "arrondissements"),
              StatsCard.view(formatNumber(stats.domaineCount), "domaines")
            )
          case None =>
            List(
              StatsCard.view("...", "permis"),
              StatsCard.view("...", "période"),
              StatsCard.view("...", "arrondissements"),
              StatsCard.view("...", "domaines")
            )
        }
      ),
      sectionTag(
        cls := "dashboard-chart-grid animate-item",
        chartCard("year-chart", "Évolution annuelle", "Volume par année. 2026 est partielle."),
        chartCard("month-chart", "Saisonnalité", "Permis cumulés par mois, toutes années confondues."),
        chartCard("borough-chart", "Arrondissements", "Répartition géographique administrative."),
        chartCard("domain-chart", "Domaines", "Nature principale des demandes."),
        chartCard("type-chart", "Types de permis", "Composition administrative des permis."),
        relationPanel(relationData.signal)
      )
    )

  private def loadSummary(summary: Var[Option[SummaryStats]], error: Var[Option[String]]): Unit =
    ApiClient.fetchSummaryStats().onComplete {
      case Success(value) =>
        summary.set(Some(value))
      case Failure(_) =>
        error.set(Some("Impossible de charger le résumé statistique."))
    }

  private def loadCharts(error: Var[Option[String]], relationData: Var[RelationData]): Unit =
    ApiClient.fetchPermitsByYear().onComplete {
      case Success(values) =>
        renderLineChart(
          canvasId = "year-chart",
          labels = values.map(_.year.toString),
          values = values.map(_.count),
          label = "Permis"
        )
      case Failure(_) =>
        error.set(Some("Impossible de charger les statistiques par année."))
    }

    ApiClient.fetchPermitsByMonth().onComplete {
      case Success(values) =>
        renderBarChart(
          canvasId = "month-chart",
          labels = values.map(item => monthLabel(item.value)),
          values = values.map(_.count),
          label = "Permis"
        )
      case Failure(_) =>
        error.set(Some("Impossible de charger les statistiques par mois."))
    }

    ApiClient.fetchPermitsByBorough().onComplete {
      case Success(values) =>
        renderHorizontalBarChart(
          canvasId = "borough-chart",
          labels = values.take(8).map(_.value),
          values = values.take(8).map(_.count),
          label = "Permis"
        )
      case Failure(_) =>
        error.set(Some("Impossible de charger les statistiques par arrondissement."))
    }

    ApiClient.fetchPermitsByDomain().onComplete {
      case Success(values) =>
        relationData.update(current => current.copy(domains = values.take(5)))
        renderHorizontalBarChart(
          canvasId = "domain-chart",
          labels = values.take(8).map(_.value),
          values = values.take(8).map(_.count),
          label = "Permis"
        )
      case Failure(_) =>
        error.set(Some("Impossible de charger les statistiques par domaine."))
    }

    ApiClient.fetchPermitsByType().onComplete {
      case Success(values) =>
        relationData.update(current => current.copy(types = values.take(4)))
        renderDoughnutChart(
          canvasId = "type-chart",
          labels = values.map(_.value),
          values = values.map(_.count)
        )
      case Failure(_) =>
        error.set(Some("Impossible de charger les statistiques par type."))
    }

    ApiClient.fetchPermitsByReason().onComplete {
      case Success(values) =>
        relationData.update(current => current.copy(reasons = values.take(5)))
      case Failure(_) =>
        error.set(Some("Impossible de charger les statistiques par raison."))
    }

    ApiClient.fetchPermitRelations().onComplete {
      case Success(values) =>
        relationData.update(current => current.copy(relations = values))
      case Failure(_) =>
        error.set(Some("Impossible de charger les relations entre les données."))
    }

  private def chartCard(canvasId: String, title: String, description: String): Element =
    articleTag(
      cls := "chart-panel",
      h2(title),
      p(description),
      div(
        cls := "chart-canvas-wrap",
        canvasTag(idAttr := canvasId)
      )
    )

  private def relationPanel(data: Signal[RelationData]): Element =
    val selectedNode = Var[Option[RelationNode]](None)
    val isExpanded = Var(false)

    articleTag(
      cls := "chart-panel relation-panel",
      div(
        cls := "chart-panel-header",
        div(
          h2("Relations clés"),
          p("Graphe interactif des dimensions les plus fréquentes du dataset.")
        ),
        button(
          cls := "relation-expand-button",
          typ := "button",
          onClick --> (_ => isExpanded.set(true)),
          "Agrandir"
        )
      ),
      child <-- data.map { current =>
        if current.relations.isEmpty then
          div(cls := "relation-loading", "Chargement...")
        else
          div(
            cls := "relation-map",
            relationGraphView(current, selectedNode, isLarge = false)
          )
      },
      child.maybe <-- data.combineWith(isExpanded.signal).map {
        case (current, true) if current.relations.nonEmpty =>
          Some(relationModal(current, selectedNode, isExpanded))
        case _ =>
          None
      }
    )

  private def relationModal(
      data: RelationData,
      selectedNode: Var[Option[RelationNode]],
      isExpanded: Var[Boolean]
  ): Element =
    div(
      cls := "relation-modal",
      div(
        cls := "relation-modal-content",
        div(
          cls := "relation-modal-header",
          div(
            h2("Relations clés"),
            p("Explore les relations dominantes entre types, domaines et raisons.")
          ),
          button(
            cls := "relation-modal-close",
            typ := "button",
            aria.label := "Fermer",
            onClick --> (_ => isExpanded.set(false)),
            "×"
          )
        ),
        div(
          cls := "relation-modal-body",
          relationGraphView(data, selectedNode, isLarge = true),
          asideTag(
            cls := "relation-modal-side",
            h3("Lecture"),
            p("La taille des noeuds varie selon le volume de permis. Le centre représente le dataset propre, les branches représentent les dimensions fréquentes."),
            p("Clique sur un noeud pour le mettre en évidence dans le graphe.")
          )
        )
      )
    )

  private def relationGraphView(data: RelationData, selectedNode: Var[Option[RelationNode]], isLarge: Boolean): Element =
    var graph: Option[js.Dynamic] = None

    div(
      cls := (if isLarge then "relation-graph is-large" else "relation-graph"),
      onMountCallback { context =>
        def mountGraph(): Unit =
          if js.isUndefined(js.Dynamic.global.cytoscape) then
            dom.window.setTimeout(() => mountGraph(), 250)
          else
            graph = Some(createRelationGraph(context.thisNode.ref, data, selectedNode, isLarge))

        mountGraph()
      },
      onUnmountCallback(_ =>
        graph.foreach(_.destroy())
        graph = None
      )
    )

  private def createRelationGraph(
      container: dom.Element,
      data: RelationData,
      selectedNode: Var[Option[RelationNode]],
      isLarge: Boolean
  ): js.Dynamic =
    val padding = if isLarge then 56 else 32
    val idealEdgeLength = if isLarge then 130 else 100

    val cy = js.Dynamic.global.cytoscape(
        js.Dynamic.literal(
          container = container,
          elements = relationElements(data),
          style = relationGraphStyle(),
          layout = js.Dynamic.literal(
            name = "cose",
            animate = true,
            animationDuration = 600,
            fit = true,
            padding = padding,
            nodeRepulsion = 5200,
            idealEdgeLength = idealEdgeLength
          ),
          minZoom = 0.6,
          maxZoom = 2.2,
          wheelSensitivity = 0.18,
          boxSelectionEnabled = false
        )
      )

    cy.on(
      "tap",
      "node",
      (event: js.Dynamic) =>
        val node = event.target
        val group = node.data("group").asInstanceOf[String]

        if group != "Hub" then
          cy.elements().removeClass("is-selected")
          node.addClass("is-selected")

          selectedNode.set(
            Some(
              RelationNode(
                group = group,
                label = node.data("name").asInstanceOf[String],
                count = node.data("count").asInstanceOf[Double].toLong
              )
            )
          )
    )

    cy.on(
      "tap",
      (event: js.Dynamic) =>
        if event.target == cy then
          cy.elements().removeClass("is-selected")
          selectedNode.set(None)
    )

    dom.window.setTimeout(
      () => cy.fit(null, padding),
      100
    )

    cy

  private def relationElements(data: RelationData): js.Array[js.Dynamic] =
    val relations = data.relations
    val typeCounts = aggregateCounts(relations.map(item => item.typePermis -> item.count)).take(4)
    val typeIds = typeCounts.zipWithIndex.map { case (item, index) => item.value -> s"type-$index" }.toMap

    val relationsForTopTypes = relations.filter(item => typeIds.contains(item.typePermis))
    val domainCounts = aggregateCounts(relationsForTopTypes.map(item => item.domaine -> item.count)).take(7)
    val domainIds = domainCounts.zipWithIndex.map { case (item, index) => item.value -> s"domain-$index" }.toMap

    val relationsForTopDomains = relationsForTopTypes.filter(item => domainIds.contains(item.domaine))
    val reasonCounts = aggregateCounts(relationsForTopDomains.map(item => item.raison -> item.count)).take(7)
    val reasonIds = reasonCounts.zipWithIndex.map { case (item, index) => item.value -> s"reason-$index" }.toMap

    val visibleRelations = relationsForTopDomains.filter { item =>
      reasonIds.contains(item.raison)
    }

    val rootCount = relations.map(_.count).sum

    val rootEdges =
      aggregateCounts(visibleRelations.map(item => item.typePermis -> item.count)).flatMap { item =>
        typeIds.get(item.value).map(typeId => graphEdge(s"permits-$typeId", "permits", typeId, "Type", item.count))
      }

    val typeDomainEdges =
      aggregateRelationPairs(visibleRelations.map(item => (item.typePermis, item.domaine, item.count))).flatMap { item =>
        for
          source <- typeIds.get(item._1)
          target <- domainIds.get(item._2)
        yield graphEdge(s"$source-$target", source, target, "Domaine", item._3)
      }

    val domainReasonEdges =
      aggregateRelationPairs(visibleRelations.map(item => (item.domaine, item.raison, item.count))).flatMap { item =>
        for
          source <- domainIds.get(item._1)
          target <- reasonIds.get(item._2)
        yield graphEdge(s"$source-$target", source, target, "Raison", item._3)
      }

    val connectedNodeIds =
      (rootEdges ++ typeDomainEdges ++ domainReasonEdges).flatMap { edge =>
        List(
          edge.data.source.asInstanceOf[String],
          edge.data.target.asInstanceOf[String]
        )
      }.toSet + "permits"

    val nodes =
      graphNode("permits", "Permis", "Hub", rootCount) ::
        typeCounts.flatMap(item => keepConnectedNode(typeIds(item.value), item, "Type", connectedNodeIds)) :::
        domainCounts.flatMap(item => keepConnectedNode(domainIds(item.value), item, "Domaine", connectedNodeIds)) :::
        reasonCounts.flatMap(item => keepConnectedNode(reasonIds(item.value), item, "Raison", connectedNodeIds))

    (nodes ++ rootEdges ++ typeDomainEdges ++ domainReasonEdges).toJSArray

  private def keepConnectedNode(
      id: String,
      item: CountByValue,
      group: String,
      connectedNodeIds: Set[String]
  ): Option[js.Dynamic] =
    if connectedNodeIds.contains(id) then Some(graphNode(id, item.value, group, item.count))
    else None

  private def aggregateCounts(values: List[(String, Long)]): List[CountByValue] =
    values
      .groupMapReduce(_._1)(_._2)(_ + _)
      .toList
      .map { case (value, count) => CountByValue(value, count) }
      .sortBy(item => (-item.count, item.value))

  private def aggregateRelationPairs(values: List[(String, String, Long)]): List[(String, String, Long)] =
    values
      .groupMapReduce(item => (item._1, item._2))(_._3)(_ + _)
      .toList
      .map { case ((source, target), count) => (source, target, count) }
      .sortBy(item => (-item._3, item._1, item._2))
      .take(26)

  private def graphNode(id: String, name: String, group: String, count: Long): js.Dynamic =
    js.Dynamic.literal(
      data = js.Dynamic.literal(
        id = id,
        name = name,
        label = graphNodeLabel(name, count),
        group = group,
        count = count.toDouble,
        size = nodeSize(count)
      )
    )

  private def graphEdge(id: String, source: String, target: String, group: String, count: Long): js.Dynamic =
    js.Dynamic.literal(
      data = js.Dynamic.literal(
        id = id,
        source = source,
        target = target,
        group = group,
        count = count.toDouble,
        label = formatCompactNumber(count),
        width = edgeWidth(count)
      )
    )

  private def graphNodeLabel(name: String, count: Long): String =
    s"${shorten(name)}\n${formatCompactNumber(count)}"

  private def relationGraphStyle(): js.Array[js.Dynamic] =
    val rootStyle = dom.window.getComputedStyle(dom.document.documentElement)
    val textColor = rootStyle.getPropertyValue("--text").trim
    val surfaceColor = rootStyle.getPropertyValue("--surface").trim
    val borderColor = rootStyle.getPropertyValue("--border").trim

    js.Array(
      js.Dynamic.literal(
        selector = "node",
        style = js.Dynamic.literal(
          "width" -> "data(size)",
          "height" -> "data(size)",
          "label" -> "data(label)",
          "background-color" -> "#0f5f8f",
          "border-width" -> 3,
          "border-color" -> surfaceColor,
          "color" -> textColor,
          "font-size" -> 11,
          "font-weight" -> 800,
          "text-wrap" -> "wrap",
          "text-max-width" -> 92,
          "text-valign" -> "center",
          "text-halign" -> "center",
          "text-outline-width" -> 3,
          "text-outline-color" -> surfaceColor,
          "overlay-opacity" -> 0
        )
      ),
      js.Dynamic.literal(
        selector = "node[group = 'Hub']",
        style = js.Dynamic.literal(
          "width" -> 92,
          "height" -> 92,
          "background-color" -> "#12304a",
          "color" -> "#ffffff",
          "text-outline-color" -> "#12304a",
          "border-color" -> "#80d7ff",
          "border-width" -> 4
        )
      ),
      js.Dynamic.literal(
        selector = "node[group = 'Type']",
        style = js.Dynamic.literal("background-color" -> "#0f5f8f")
      ),
      js.Dynamic.literal(
        selector = "node[group = 'Domaine']",
        style = js.Dynamic.literal("background-color" -> "#1677aa")
      ),
      js.Dynamic.literal(
        selector = "node[group = 'Raison']",
        style = js.Dynamic.literal("background-color" -> "#5c8fb0")
      ),
      js.Dynamic.literal(
        selector = "node.is-selected",
        style = js.Dynamic.literal(
          "background-color" -> "#12304a",
          "border-color" -> "#80d7ff",
          "border-width" -> 6
        )
      ),
      js.Dynamic.literal(
        selector = "edge",
        style = js.Dynamic.literal(
          "width" -> "data(width)",
          "label" -> "data(label)",
          "line-color" -> borderColor,
          "curve-style" -> "bezier",
          "target-arrow-shape" -> "triangle",
          "target-arrow-color" -> borderColor,
          "font-size" -> 9,
          "font-weight" -> 800,
          "color" -> textColor,
          "text-background-color" -> surfaceColor,
          "text-background-opacity" -> 0.9,
          "text-background-padding" -> 3,
          "text-rotation" -> "autorotate",
          "opacity" -> 0.72
        )
      ),
      js.Dynamic.literal(
        selector = "edge[group = 'Type']",
        style = js.Dynamic.literal("line-color" -> "#0f5f8f", "target-arrow-color" -> "#0f5f8f")
      ),
      js.Dynamic.literal(
        selector = "edge[group = 'Domaine']",
        style = js.Dynamic.literal("line-color" -> "#1677aa", "target-arrow-color" -> "#1677aa")
      ),
      js.Dynamic.literal(
        selector = "edge[group = 'Raison']",
        style = js.Dynamic.literal("line-color" -> "#5c8fb0", "target-arrow-color" -> "#5c8fb0")
      )
    )

  private def nodeSize(count: Long): Double =
    if count <= 0 then 68.0
    else 38.0 + math.min(30.0, math.log10(count.toDouble + 1.0) * 10.0)

  private def edgeWidth(count: Long): Double =
    1.5 + math.min(5.0, math.log10(count.toDouble + 1.0) * 1.2)

  private def formatCompactNumber(value: Long): String =
    if value >= 1000000 then f"${value / 1000000.0}%.1fM"
    else if value >= 1000 then f"${value / 1000.0}%.1fk"
    else value.toString

  private def renderLineChart(canvasId: String, labels: List[String], values: List[Long], label: String): Unit =
    renderChart(
      canvasId = canvasId,
      chartType = "line",
      labels = labels,
      datasets = js.Array(
        js.Dynamic.literal(
          label = label,
          data = values.map(_.toDouble).toJSArray,
          borderColor = "#0f5f8f",
          backgroundColor = "rgba(15, 95, 143, 0.14)",
          fill = true,
          tension = 0.35,
          pointRadius = 4,
          pointHoverRadius = 6
        )
      ),
      extraOptions = js.Dynamic.literal()
    )

  private def renderBarChart(canvasId: String, labels: List[String], values: List[Long], label: String): Unit =
    renderChart(
      canvasId = canvasId,
      chartType = "bar",
      labels = labels,
      datasets = js.Array(
        js.Dynamic.literal(
          label = label,
          data = values.map(_.toDouble).toJSArray,
          backgroundColor = "#0f5f8f",
          borderRadius = 10
        )
      ),
      extraOptions = js.Dynamic.literal()
    )

  private def renderHorizontalBarChart(canvasId: String, labels: List[String], values: List[Long], label: String): Unit =
    renderChart(
      canvasId = canvasId,
      chartType = "bar",
      labels = labels,
      datasets = js.Array(
        js.Dynamic.literal(
          label = label,
          data = values.map(_.toDouble).toJSArray,
          backgroundColor = "#0f5f8f",
          borderRadius = 10
        )
      ),
      extraOptions = js.Dynamic.literal(indexAxis = "y")
    )

  private def renderDoughnutChart(canvasId: String, labels: List[String], values: List[Long]): Unit =
    renderChart(
      canvasId = canvasId,
      chartType = "doughnut",
      labels = labels,
      datasets = js.Array(
        js.Dynamic.literal(
          data = values.map(_.toDouble).toJSArray,
          backgroundColor = js.Array("#0f5f8f", "#12304a", "#80d7ff", "#5e6b7a", "#d9e2ea"),
          borderWidth = 0
        )
      ),
      extraOptions = js.Dynamic.literal(cutout = "62%")
    )

  private def renderChart(
      canvasId: String,
      chartType: String,
      labels: List[String],
      datasets: js.Array[js.Dynamic],
      extraOptions: js.Dynamic
  ): Unit =
    val canvas = dom.document.getElementById(canvasId)

    if canvas == null then
      ()
    else if js.isUndefined(js.Dynamic.global.Chart) then
      dom.window.setTimeout(
        () => renderChart(canvasId, chartType, labels, datasets, extraOptions),
        250
      )
    else
      charts.get(canvasId).foreach(_.destroy())

      val options = js.Dynamic.literal(
        responsive = true,
        maintainAspectRatio = false,
        plugins = js.Dynamic.literal(
          legend = js.Dynamic.literal(
            display = chartType == "doughnut",
            position = "bottom"
          ),
          tooltip = js.Dynamic.literal(enabled = true)
        ),
        scales = js.Dynamic.literal(
          x = js.Dynamic.literal(
            grid = js.Dynamic.literal(display = false),
            ticks = js.Dynamic.literal(color = "#5e6b7a")
          ),
          y = js.Dynamic.literal(
            grid = js.Dynamic.literal(color = "rgba(94, 107, 122, 0.16)"),
            ticks = js.Dynamic.literal(color = "#5e6b7a")
          )
        )
      )

      extraOptions.asInstanceOf[js.Dictionary[js.Any]].foreach { case (key, value) =>
        options.updateDynamic(key)(value)
      }

      val config = js.Dynamic.literal(
        `type` = chartType,
        data = js.Dynamic.literal(
          labels = labels.toJSArray,
          datasets = datasets
        ),
        options = options
      )

      val chart = js.Dynamic.newInstance(js.Dynamic.global.Chart)(canvas, config)
      charts = charts.updated(canvasId, chart)

  private def destroyCharts(): Unit =
    charts.values.foreach(_.destroy())
    charts = Map.empty

  private def monthLabel(value: String): String =
    value match
      case "01" => "Jan"
      case "02" => "Fév"
      case "03" => "Mar"
      case "04" => "Avr"
      case "05" => "Mai"
      case "06" => "Juin"
      case "07" => "Juil"
      case "08" => "Août"
      case "09" => "Sep"
      case "10" => "Oct"
      case "11" => "Nov"
      case "12" => "Déc"
      case _    => value

  private def shorten(value: String): String =
    if value.length <= 58 then value
    else value.take(55).trim + "..."

  private def formatNumber(value: Long): String =
    "%,d".format(value).replace(",", " ")
