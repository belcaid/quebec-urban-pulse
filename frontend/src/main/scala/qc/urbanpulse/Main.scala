package qc.urbanpulse

import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom
import qc.urbanpulse.animations.PageAnimations
import qc.urbanpulse.components.{Footer, Navbar}
import qc.urbanpulse.pages.{AboutPage, DashboardPage, HomePage, MapPage}

@main
def main(): Unit =
  val prefersDark = dom.window.matchMedia("(prefers-color-scheme: dark)").matches
  val isDarkTheme = Var(prefersDark)
  val currentRoute = Var(readRoute())

  def applyTheme(isDark: Boolean): Unit =
    val theme = if isDark then "dark" else "light"
    dom.document.documentElement.setAttribute("data-theme", theme)

  applyTheme(isDarkTheme.now())
  isDarkTheme.signal.foreach(applyTheme)(unsafeWindowOwner)
  currentRoute.signal.foreach(_ => PageAnimations.animateRoute())(unsafeWindowOwner)
  dom.window.addEventListener("hashchange", (_: dom.Event) => currentRoute.set(readRoute()))

  renderOnDomContentLoaded(
    dom.document.getElementById("app"),
    div(
      cls := "app-shell",
      onMountCallback(_ => PageAnimations.animateChrome()),
      Navbar.view(isDarkTheme, currentRoute.signal),
      div(
        cls := "route-view",
        child <-- currentRoute.signal.map {
          case "/map"       => MapPage.view
          case "/dashboard" => DashboardPage.view
          case "/about"     => AboutPage.view
          case _            => HomePage.view
        }
      ),
      child.maybe <-- currentRoute.signal.map { route =>
        if route == "/map" then None else Some(Footer.view)
      }
    )
  )

def readRoute(): String =
  dom.window.location.hash.stripPrefix("#") match
    case "" | "/"    => "/"
    case "/map"      => "/map"
    case "/dashboard" => "/dashboard"
    case "/about"    => "/about"
    case _           => "/"
