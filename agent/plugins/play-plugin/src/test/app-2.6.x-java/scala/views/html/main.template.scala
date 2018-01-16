
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

object main extends _root_.play.twirl.api.BaseScalaTemplate[play.twirl.api.HtmlFormat.Appendable, _root_.play.twirl.api.Format[play.twirl.api.HtmlFormat.Appendable]](play.twirl.api.HtmlFormat) with _root_.play.twirl.api.Template2[String, Html, play.twirl.api.HtmlFormat.Appendable] {

  /*
 * This template is called from the `index` template. This template
 * handles the rendering of the page header and body tags. It takes
 * two arguments, a `String` for the title of the page and an `Html`
 * object to insert into the body of the page.
 */
  def apply /*7.2*/ (title: String)(content: Html): play.twirl.api.HtmlFormat.Appendable = {
    _display_ {
      {

        Seq[Any](format.raw /*7.32*/ ("""

"""), format.raw /*9.1*/ ("""<!DOCTYPE html>
<html lang="en">
    <head>
        """), format.raw /*12.62*/ ("""
        """), format.raw /*13.9*/ ("""<title>"""), _display_( /*13.17*/ title), format.raw /*13.22*/ ("""</title>
        <link rel="stylesheet" media="screen" href=""""), _display_( /*14.54*/ routes /*14.60*/ .Assets.versioned("stylesheets/main.css")), format.raw /*14.101*/ ("""">
        <link rel="shortcut icon" type="image/png" href=""""), _display_( /*15.59*/ routes /*15.65*/ .Assets.versioned("images/favicon.png")), format.raw /*15.104*/ ("""">
        <script src=""""), _display_( /*16.23*/ routes /*16.29*/ .Assets.versioned("javascripts/hello.js")), format.raw /*16.70*/ ("""" type="text/javascript"></script>
    </head>
    <body>
        """), format.raw /*20.32*/ ("""
        """), _display_( /*21.10*/ content), format.raw /*21.17*/ ("""
    """), format.raw /*22.5*/ ("""</body>
</html>
"""))
      }
    }
  }

  def render(title: String, content: Html): play.twirl.api.HtmlFormat.Appendable = apply(title)(content)

  def f: ((String) => (Html) => play.twirl.api.HtmlFormat.Appendable) = (title) => (content) => apply(title)(content)

  def ref: this.type = this

}

/*
                  -- GENERATED --
                  DATE: Fri Nov 10 19:09:28 PST 2017
                  SOURCE: D:/git/glowroot/agent/plugins/play-plugin/tmp-router-files/app/views/main.scala.html
                  HASH: 6ea0cc6fffc14ad60386de863a153b880fcdcbb3
                  MATRIX: 1206->260|1331->290|1359->292|1439->397|1475->406|1510->414|1536->419|1625->481|1640->487|1703->528|1791->589|1806->595|1867->634|1919->659|1934->665|1996->706|2090->862|2127->872|2155->879|2187->884
                  LINES: 33->7|38->7|40->9|43->12|44->13|44->13|44->13|45->14|45->14|45->14|46->15|46->15|46->15|47->16|47->16|47->16|50->20|51->21|51->21|52->22
                  -- GENERATED --
              */
