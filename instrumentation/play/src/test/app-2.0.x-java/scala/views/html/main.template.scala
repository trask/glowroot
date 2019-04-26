
package views.html

import play.templates._
import play.templates.TemplateMagic._

import play.api.templates._
import play.api.templates.PlayMagic._
import models._
import controllers._
import java.lang._
import java.util._
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import play.api.i18n._
import play.api.templates.PlayMagicForJava._
import play.mvc._
import play.data._
import play.api.data.Field
import com.avaje.ebean._
import play.mvc.Http.Context.Implicit._
import views.html._
/*
 * This template is called from the `index` template. This template
 * handles the rendering of the page header and body tags. It takes
 * two arguments, a `String` for the title of the page and an `Html`
 * object to insert into the body of the page.
 */
object main extends BaseScalaTemplate[play.api.templates.Html,Format[play.api.templates.Html]](play.api.templates.HtmlFormat) with play.api.templates.Template2[String,Html,play.api.templates.Html] {

    /*
 * This template is called from the `index` template. This template
 * handles the rendering of the page header and body tags. It takes
 * two arguments, a `String` for the title of the page and an `Html`
 * object to insert into the body of the page.
 */
    def apply/*7.2*/(title: String)(content: Html):play.api.templates.Html = {
        _display_ {

Seq[Any](format.raw/*7.32*/("""

<!DOCTYPE html>
<html lang="en">
    <head>
        """),format.raw/*12.62*/("""
        <title>"""),_display_(Seq[Any](/*13.17*/title)),format.raw/*13.22*/("""</title>
        <script src=""""),_display_(Seq[Any](/*14.23*/routes/*14.29*/.Assets.at("scripts/empty.js"))),format.raw/*14.59*/("""" type="text/javascript"></script>
    </head>
    <body>
        """),format.raw/*18.32*/("""
        """),_display_(Seq[Any](/*19.10*/content)),format.raw/*19.17*/("""
    </body>
</html>
"""))}
    }
    
    def render(title:String,content:Html) = apply(title)(content)
    
    def f:((String) => (Html) => play.api.templates.Html) = (title) => (content) => apply(title)(content)
    
    def ref = this

}
                /*
                    -- GENERATED --
                    DATE: Sat Apr 09 19:37:22 PDT 2016
                    SOURCE: D:/git/trask/glowroot/agent-parent/plugins/play-plugin/tmp-router-files/app/views/main.scala.html
                    HASH: ebf9ba9ebb5444bf57ea0ee85eac9386b2085564
                    MATRIX: 1267->260|1374->290|1456->397|1509->414|1536->419|1603->450|1618->456|1670->486|1764->642|1810->652|1839->659
                    LINES: 37->7|40->7|45->12|46->13|46->13|47->14|47->14|47->14|50->18|51->19|51->19
                    -- GENERATED --
                */
            