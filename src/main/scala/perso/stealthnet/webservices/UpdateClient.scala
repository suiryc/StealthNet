package perso.stealthnet.webservices

import scala.xml.XML

/**
 * RShare update client.
 */
object UpdateClient {

  /**
   * GetWebCaches SOAP action.
   *
   * @param url URL to call
   * @return webcache (URL) list upon success
   */
  def getWebCaches(host: String): Option[List[String]] = {
    SoapClient.doRequest(host, <GetWebCaches xmlns="http://rshare.de/rshareupdates.asmx" />) match {
      case Left(l) =>
        /* XXX - report failure in logs ? */
        println("Failed: " + l)
        None

      case Right(r) =>
        try {
          val webCachesNode = XML.loadString((r \\ "GetWebCachesResult").text)
          val webCaches = for { webCacheNode <- webCachesNode \\ "webcache"
            webCache <- webCacheNode \ "@url"
              if (webCache != "") } yield webCache.text
          Some(webCaches.toList)
        }
        catch {
          case e: Exception =>
            /* XXX - report failure in logs ? */
            println("Failed: " + e)
            None
        }
    }
  }

}
