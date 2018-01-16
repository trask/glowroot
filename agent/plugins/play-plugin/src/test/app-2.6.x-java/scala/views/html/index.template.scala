
package views.html

import _root_.play.twirl.api.TwirlFeatureImports._
import _root_.play.twirl.api.TwirlHelperImports._
import _root_.play.twirl.api.Html
import _root_.play.twirl.api.JavaScript
import _root_.play.twirl.api.Txt
import _root_.play.twirl.api.Xml
import models._
import controllers._
import play.api.i18n._
import views.html._
import play.api.templates.PlayMagic._
import java.lang._
import java.util._
import scala.collection.JavaConverters._
import play.core.j.PlayMagicForJava._
import play.mvc._
import play.api.data.Field
import play.mvc.Http.Context.Implicit._

object index extends _root_.play.twirl.api.BaseScalaTemplate[play.twirl.api.HtmlFormat.Appendable, _root_.play.twirl.api.Format[play.twirl.api.HtmlFormat.Appendable]](play.twirl.api.HtmlFormat) with _root_.play.twirl.api.Template1[String, play.twirl.api.HtmlFormat.Appendable] {

  /*
 * This template takes a single argument, a String containing a
 * message to display.
 */
  def apply /*5.2*/ (message: String): play.twirl.api.HtmlFormat.Appendable = {
    _display_ {
      {

        Seq[Any](format.raw /*5.19*/ ("""

"""), format.raw /*11.4*/ ("""
"""), _display_( /*12.2*/ main("Welcome to Play") /*12.25*/ {
          _display_(Seq[Any](format.raw /*12.27*/ ("""

""")))
        }), format.raw /*14.2*/ ("""
"""))
      }
    }
  }

  def render(message: String): play.twirl.api.HtmlFormat.Appendable = apply(message)

  def f: ((String) => play.twirl.api.HtmlFormat.Appendable) = (message) => apply(message)

  def ref: this.type = this

}

/*
                  -- GENERATED --
                  DATE: Fri Nov 10 19:09:28 PST 2017
                  SOURCE: D:/git/glowroot/agent/plugins/play-plugin/tmp-router-files/app/views/index.scala.html
                  HASH: 5efe4aa28d57709959b008a1f4e251aa918b5721
                  MATRIX: 1037->95|1149->112|1178->308|1206->310|1238->333|1278->335|1311->338
                  LINES: 31->5|36->5|38->11|39->12|39->12|39->12|41->14
                  -- GENERATED --
              */
