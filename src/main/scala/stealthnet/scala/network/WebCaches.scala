package stealthnet.scala.network

import stealthnet.scala.Config
import stealthnet.scala.core.Core
import stealthnet.scala.util.Peer
import stealthnet.scala.util.log.{EmptyLoggingContext, Logging}
import stealthnet.scala.webservices.{UpdateClient, WebCacheClient}

/**
 * Manages WebCaches.
 */
object WebCaches extends Logging with EmptyLoggingContext {

  /** Known WebCaches. */
  protected var webCaches: List[String] = List()
  /** Cyclic iterator on WebCaches. */
  private var cyclicIt: Iterator[String] = Nil.iterator
  /** Whether we are currently added on WebCaches. */
  protected var addedPeer = false

  /**
   * Refreshes WebCaches.
   *
   * Removes ourself, get updated list of WebCaches, and re-adds ourself if needed.
   *
   * If no WebCache could be retrieved, use default:
   *   - `http://rshare.de/rshare.asmx`
   *   - `http://webcache.stealthnet.at/rwpmws.php`
   *
   * @todo configuration: allow manual list of WebCaches without refresh
   */
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
    cyclicIt = webCaches.iterator

    if (readd)
      addPeer()
  }

  /**
   * Removes ourself from WebCaches.
   *
   * Done if we were added.
   */
  def removePeer() =
    if (addedPeer) {
      for (webCache <- webCaches)
        WebCacheClient.removePeer(webCache)
      addedPeer = false
    }

  /**
   * Adds ourself on WebCaches.
   *
   * Done if we were not already added.
   */
  def addPeer() =
    if (!addedPeer && !Core.stopping) {
      removePeer()

      for (webCache <- webCaches)
        WebCacheClient.addPeer(webCache, Config.serverPort)

      addedPeer = true
    }

  /**
   * Gets a peer.
   *
   * Each WebCache is requested until a peer is retrieved. The last requested
   * WebCache is remembered, and successive calls do start from the next one.
   *
   * Adds ourself on WebCaches if needed before requesting a peer.
   *
   * @return an option value containing the retrieved peer, or `None` if none
   */
  def getPeer(): Option[Peer] = {
    if (webCaches.size == 0)
      return None

    if (!addedPeer)
      addPeer()

    for (i <- 1 to webCaches.size) {
      val webCache = cyclicIt.next
      if (!cyclicIt.hasNext)
        cyclicIt = webCaches.iterator

      WebCacheClient.getPeer(webCache) match {
        case None =>

        case v =>
          return v
      }
    }

    return None
  }

}
