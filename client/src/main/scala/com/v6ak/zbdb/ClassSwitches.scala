package com.v6ak.zbdb

import org.scalajs.dom
import org.scalajs.dom.*
import scalatags.JsDom.all.{button as buttonTag, *}


final class ClassSwitches(
  initialSwitchesState: Map[String, String],
  alreadySwitchedClasses: Map[String, String] = Map(),
  handler: (String, String, () => Unit) => Unit = (_: String, _: String, f: () => Unit) => f(),
) {
  private val switchesState = collection.mutable.Map(initialSwitchesState.toSeq: _*)

  def values = switchesState.values

  private def update(switchName: String, newClass: String, allValues: Set[String]): Unit =
    val oldClasses = allValues - newClass
    val classList = dom.document.body.classList
    handler(switchName, newClass, () => {
      classList.add(newClass)
      oldClasses.foreach(classList.remove)
      switchesState(switchName) = newClass
      alreadySwitchedClasses.get(switchName).foreach(classList.add)
    })

  def classSelect(switchName: String)(items: (String, String)*) = select(
    onchange := { (e: Event) =>
      val el = e.currentTarget.asInstanceOf[HTMLSelectElement]
      update(switchName, el.value, items.map(_._1).toSet)
    }
  )(
    for ((cls, name) <- items) yield
      option(value := cls, if (cls == switchesState(switchName)) selected := true else "")(name)
  )

  def checkbox(switchName: String, description: Frag)(onClass: String, offClass: String) = label(
    input(
      `type` := "checkbox",
      if (onClass == switchesState(switchName)) checked := true else "",
      onchange := { (e: Event) =>
        val el = e.currentTarget.asInstanceOf[HTMLInputElement]
        update(switchName, if (el.checked) onClass else offClass, Set(onClass, offClass))
      }
    ),
    " ",
    description,
  )

  def button(switchName: String, to: String, allValues: Set[String]) = buttonTag(
    onclick := { (e: Event) =>
      update(switchName, to, allValues)
    }
  )

}
