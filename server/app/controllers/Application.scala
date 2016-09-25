package controllers

import javax.inject.Inject

import play.api.Environment
import play.api.libs.json.JsString
import play.api.mvc._
import play.twirl.api.Txt

class Application @Inject() ()(implicit environment: Environment) extends Controller {

  def mainJs() = Action {
    val scriptsHtml = scalajs.html.scripts("zbdb-stats-client", routes.Assets.versioned(_).toString, name => getClass.getResource(s"/public/$name") != null)
    val scriptsLoadJs = "document.write("+JsString(scriptsHtml.toString())+");"
    Ok(Txt(scriptsLoadJs)).as("application/javascript")
  }
}
