package qc.urbanpulse.animations

import org.scalajs.dom

import scala.scalajs.js

object PageAnimations:
  def animateChrome(): Unit =
    withGsap { gsap =>
      gsap.fromTo(
            ".navbar",
            js.Dynamic.literal(autoAlpha = 0, y = -14),
            js.Dynamic.literal(autoAlpha = 1, y = 0, duration = 0.45, ease = "power3.out", clearProps = "transform")
          )
    }

  def animateRoute(): Unit =
    dom.window.setTimeout(
      () =>
        withGsap { gsap =>
          gsap.fromTo(
            ".route-view > *",
            js.Dynamic.literal(autoAlpha = 0, y = 18),
            js.Dynamic.literal(autoAlpha = 1, y = 0, duration = 0.5, ease = "power3.out", clearProps = "transform")
          )
        },
      30
    )

  def animatePageItems(containerSelector: String): Unit =
    dom.window.setTimeout(
      () =>
        withGsap { gsap =>
          gsap.fromTo(
            s"$containerSelector .animate-item",
            js.Dynamic.literal(autoAlpha = 0, y = 18),
            js.Dynamic.literal(
              autoAlpha = 1,
              y = 0,
              duration = 0.55,
              stagger = 0.08,
              ease = "power3.out",
              clearProps = "transform"
            )
          )
        },
      80
    )

  def animateMapLayout(): Unit =
    withGsap { gsap =>
      gsap.fromTo(
        ".map-results-panel, .map-canvas-panel",
        js.Dynamic.literal(autoAlpha = 0, y = 14),
        js.Dynamic.literal(autoAlpha = 1, y = 0, duration = 0.55, stagger = 0.08, ease = "power3.out", clearProps = "transform")
      )
    }

  private def withGsap(run: js.Dynamic => Unit): Unit =
    val gsap = js.Dynamic.global.gsap

    if prefersReducedMotion then revealAnimatedElements()
    else if !js.isUndefined(gsap) then run(gsap)
    else revealAnimatedElements()

  private def prefersReducedMotion: Boolean =
    dom.window.matchMedia("(prefers-reduced-motion: reduce)").matches

  private def revealAnimatedElements(): Unit =
    val nodes = dom.document.querySelectorAll(".animate-item, .navbar, .route-view > *, .map-results-panel, .map-canvas-panel")

    for index <- 0 until nodes.length do
      val element = nodes.item(index).asInstanceOf[dom.HTMLElement]
      element.style.visibility = "visible"
      element.style.opacity = "1"
