package controllers

import javax.inject.Inject

import play.api.Environment
import play.api.libs.json.JsString
import play.api.mvc._
import play.twirl.api.Txt

class Application @Inject() (cc: ControllerComponents)(implicit environment: Environment)
  extends AbstractController(cc)
{

  def yearStatsRedirect(year: Int) = Action{
    Redirect(routes.Assets.at(s"$year/statistiky/index.html"))
  }

  def mainJs() = Action {
    val scriptsHtml = scalajs.html.scripts("zbdb-stats-client", routes.Assets.versioned(_).toString, name => getClass.getResource(s"/public/$name") != null)
    val scriptsLoadJs = "document.write("+JsString(scriptsHtml.toString())+");"
    Ok(Txt(scriptsLoadJs)).as("application/javascript")
  }
}
