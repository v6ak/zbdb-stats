package com.v6ak.zbdb

import org.scalajs.dom
import org.scalajs.dom.{Event, HTMLElement, HTMLTableElement}

import scala.scalajs.js

object HorizontalStickiness {

  def addHorizontalStickiness(tableElement: HTMLTableElement): Unit = {
    val participantHeaders = dom.window.asInstanceOf[js.Dynamic].Array.prototype.slice.call(tableElement.querySelectorAll(".participant-header")).asInstanceOf[js.Array[HTMLElement]]
    for(h <- participantHeaders){
      h.style.position = "relative"
    }
    val horizontalScrollClassGuard = ValueGuard(false){ (_, scrolledHorizontally) =>
      if(scrolledHorizontally){
        for(t <- participantHeaders){
          t.classList.add("scrolled-horizontally")
        }
      }else{
        for(t <- participantHeaders){
          t.classList.remove("scrolled-horizontally")
        }
      }
    }
    val horizontalScrollPositionGuard = ValueGuard(0.0){(_, scrollLeft) =>
      val left = s"${scrollLeft}px"
      for(t <- participantHeaders){
        t.style.left = left
      }
      horizontalScrollClassGuard.update(scrollLeft != 0)
    }
    dom.document.addEventListener("scroll", (_: Event) => {
      horizontalScrollPositionGuard.update(dom.document.documentElement.scrollLeft)
    })
  }

}
