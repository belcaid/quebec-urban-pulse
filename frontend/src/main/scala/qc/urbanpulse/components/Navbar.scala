package qc.urbanpulse.components

import com.raquo.laminar.api.L.{*, given}

object Navbar:
  def view(isDarkTheme: Var[Boolean]): Element =
    val menuOpen = Var(false)

    headerTag(
      cls := "navbar",
      cls.toggle("is-menu-open") <-- menuOpen.signal,
      div(
        cls := "navbar-main",
        a(
          cls := "navbar-brand",
          href := "#/",
          onClick --> (_ => menuOpen.set(false)),
          img(
            cls := "navbar-logo",
            src := "public/logo.svg",
            alt := ""
          ),
          span("Québec Urban Pulse")
        ),
        div(
          cls := "navbar-controls",
          button(
            cls := "theme-toggle",
            typ := "button",
            aria.label <-- isDarkTheme.signal.map(isDark =>
              if isDark then "Activer le mode clair" else "Activer le mode sombre"
            ),
            onClick --> (_ => isDarkTheme.update(current => !current)),
            child <-- isDarkTheme.signal.map(isDark =>
              if isDark then sunIcon else moonIcon
            )
          ),
          button(
            cls := "navbar-menu-toggle",
            typ := "button",
            aria.label := "Ouvrir le menu",
            aria.expanded <-- menuOpen.signal,
            onClick --> (_ => menuOpen.update(current => !current)),
            span(
              cls := "navbar-menu-icon",
              span(),
              span(),
              span()
            )
          )
        )
      ),
      navTag(
        cls := "navbar-actions",
        a(cls := "nav-link", href := "#/map", onClick --> (_ => menuOpen.set(false)), "Explorer"),
        a(cls := "nav-link", href := "#/dashboard", onClick --> (_ => menuOpen.set(false)), "Comprendre"),
        a(cls := "nav-link", href := "#/about", onClick --> (_ => menuOpen.set(false)), "À propos")
      )
    )

  private def sunIcon: SvgElement =
    svg.svg(
      svg.viewBox := "0 0 16 16",
      svg.width := "16",
      svg.height := "16",
      svg.fill := "none",
      svg.g(
        svg.transform := "scale(1.15) translate(-1, -1)",
        svg.circle(
          svg.cx := "8",
          svg.cy := "8",
          svg.r := "3.33",
          svg.stroke := "currentColor",
          svg.strokeWidth := "1.33",
          svg.strokeLineCap := "round",
          svg.strokeLineJoin := "round"
        ),
        svg.path(
          svg.d := "M8 1.33V2.67M8 13.33V14.67M14.67 8H13.33M2.67 8H1.33M12.72 3.28L11.78 4.22M4.22 11.78L3.28 12.72M12.72 12.72L11.78 11.78M4.22 4.22L3.28 3.28",
          svg.stroke := "currentColor",
          svg.strokeWidth := "1.33",
          svg.strokeLineCap := "round",
          svg.strokeLineJoin := "round"
        )
      )
    )

  private def moonIcon: SvgElement =
    svg.svg(
      svg.viewBox := "0 0 16 16",
      svg.width := "16",
      svg.height := "16",
      svg.fill := "none",
      svg.g(
        svg.transform := "scale(1.05)",
        svg.path(
          svg.fillRule := "evenodd",
          svg.clipRule := "evenodd",
          svg.d := "M8.06207 1.68681C8.19154 1.90981 8.18081 2.18749 8.03446 2.39981C7.59054 3.04388 7.33074 3.82385 7.33074 4.66651C7.33074 6.87566 9.12159 8.66651 11.3307 8.66651C12.1734 8.66651 12.9535 8.40668 13.5975 7.96274C13.8099 7.81641 14.0875 7.80559 14.3105 7.93506C14.5336 8.06454 14.6619 8.31093 14.6401 8.56793C14.3509 11.983 11.4883 14.664 7.99874 14.664C4.31755 14.664 1.33333 11.6798 1.33333 7.99861C1.33333 4.50915 4.01427 1.64657 7.42919 1.35721C7.68619 1.33543 7.93259 1.46379 8.06207 1.68681ZM6.28002 2.94944C4.17867 3.66452 2.66667 5.65523 2.66667 7.99861C2.66667 10.9434 5.05421 13.3307 7.99874 13.3307C10.3422 13.3307 12.3329 11.8186 13.0479 9.71713C12.5088 9.90041 11.9311 9.99984 11.3307 9.99984C8.38516 9.99984 5.99741 7.61209 5.99741 4.66651C5.99741 4.06621 6.09679 3.48853 6.28002 2.94944Z",
          svg.fill := "currentColor"
        ),
        svg.path(
          svg.d := "M10.8249 3.34491L11.366 2.26279C11.4888 2.01711 11.8394 2.01711 11.9623 2.26279L12.5033 3.34491C12.5356 3.40942 12.5879 3.46173 12.6524 3.49398L13.7346 4.03503C13.9802 4.15788 13.9802 4.50848 13.7346 4.63132L12.6524 5.17237C12.5879 5.20461 12.5356 5.25692 12.5033 5.32143L11.9623 6.40356C11.8394 6.64924 11.4888 6.64924 11.366 6.40356L10.8249 5.32143C10.7927 5.25692 10.7404 5.20461 10.6759 5.17237L9.59375 4.63132C9.34807 4.50848 9.34807 4.15788 9.59375 4.03503L10.6759 3.49398C10.7404 3.46173 10.7927 3.40942 10.8249 3.34491Z",
          svg.fill := "currentColor"
        )
      )
    )
