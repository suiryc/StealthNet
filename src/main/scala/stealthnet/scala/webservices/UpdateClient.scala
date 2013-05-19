package stealthnet.scala.webservices

import scala.xml.XML
import stealthnet.scala.util.log.{EmptyLoggingContext, Logging}

/**
 * ''RShare'' update client.
 */
object UpdateClient extends Logging with EmptyLoggingContext {

  /**
   * ''GetWebCaches'' ''SOAP'' action.
   *
   * @param url ''URL'' to call
   * @return webcache (''URL'') list upon success
   */
  def getWebCaches(url: String): Option[List[String]] = {
    SoapClient.doRequest(url, <GetWebCaches xmlns="http://rshare.de/rshareupdates.asmx" />) match {
      case Left(l) =>
        logger error s"Failed to get WebCaches from service[$url]: $l"
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
            logger error (s"Failed to get WebCaches from service[$url]!", e)
            None
        }
    }
  }

}
