package perso.stealthnet.network

import perso.stealthnet.util.EmptyLoggingContext
import perso.stealthnet.util.Logging
import perso.stealthnet.webservices.UpdateClient
import perso.stealthnet.webservices.WebCacheClient
import perso.stealthnet.util.Peer

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

  def addPeer() = {
    removePeer()

    for (webCache <- webCaches)
      /* XXX - Configuration */
      WebCacheClient.addPeer(webCache, 6097)

    addedPeer = true
  }

  def getPeer(): Peer = {
    /* XXX - iterate over WebCaches for successive calls */
    for (webCache <- webCaches)
      WebCacheClient.getPeer(webCache) match {
        case Some(peer) =>
          return peer

        case None =>
      }

      return null
    }

}
