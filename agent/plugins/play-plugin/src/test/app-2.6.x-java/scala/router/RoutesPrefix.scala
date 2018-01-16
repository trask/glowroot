
// @GENERATOR:play-routes-compiler
// @SOURCE:D:/git/glowroot-copy-for-fixing-play-2.6/agent/plugins/play-plugin/tmp-router-files/conf/routes
// @DATE:Sun Nov 26 10:16:26 PST 2017


package router {
  object RoutesPrefix {
    private var _prefix: String = "/"
    def setPrefix(p: String): Unit = {
      _prefix = p
    }
    def prefix: String = _prefix
    val byNamePrefix: Function0[String] = { () => prefix }
  }
}
