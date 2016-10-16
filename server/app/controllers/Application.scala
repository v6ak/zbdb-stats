package controllers

import javax.inject.Inject

import play.api.Environment
import play.api.libs.json.JsString
import play.api.mvc._
import play.twirl.api.Txt

class Application @Inject() ()(implicit environment: Environment) extends Controller {

  def yearStats(year: Int) = Assets.versioned(path = "/public", file = Assets.Asset(s"$year/statistiky/index.html"))

  def yearStatsRedirect(year: Int) = Action{
    Redirect(routes.Application.yearStats(year))
  }

  def staticFile(year: Int, file: String) = {
    println(s"$year/statistiky/$file")
    Assets.versioned(path = "/public", file = Assets.Asset(s"$year/statistiky/$file"))
  }

  def mainJs() = Action {
    val scriptsHtml = scalajs.html.scripts("zbdb-stats-client", routes.Assets.versioned(_).toString, name => getClass.getResource(s"/public/$name") != null)
    val scriptsLoadJs = "document.write("+JsString(scriptsHtml.toString())+");"
    Ok(Txt(scriptsLoadJs)).as("application/javascript")
  }
}
