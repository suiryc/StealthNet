package stealthnet.scala.network

import stealthnet.scala.util.{EmptyLoggingContext, Logging, Peer}
import stealthnet.scala.webservices.{UpdateClient, WebCacheClient}

/* XXX - when needing connections, use both addPeer and getPeer ... */
object WebCaches extends Logging with EmptyLoggingContext {

  protected var webCaches: List[String] = List()
  protected var addedPeer = false

  def refresh() = {
    val readd = addedPeer
    removePeer()

    webCaches = UpdateClient.getWebCaches("http://rshare.de/rshareupdates.asmx") match {
      case Some(urls) =>
        logger debug("Got webCaches: " + urls)
        urls

      case None =>
        logger debug("Got no webCache: using default")
        List("http://rshare.de/rshare.asmx", "http://webcache.stealthnet.at/rwpmws.php")
    }

    if (readd)
      addPeer()
  }

  def removePeer() =
    if (addedPeer) {
      for (webCache <- webCaches)
        WebCacheClient.removePeer(webCache)
      addedPeer = false
    }

  def addPeer() = 
    if (!addedPeer) {
      removePeer()

      for (webCache <- webCaches)
        /* XXX - Configuration */
        WebCacheClient.addPeer(webCache, 6097)

      addedPeer = true
    }

  def getPeer(): Option[Peer] = {
    /* XXX - iterate over WebCaches for successive calls */
    for (webCache <- webCaches) {
      WebCacheClient.getPeer(webCache) match {
        case None =>

        case v =>
          return v
      }
    }

    return None
  }

}
