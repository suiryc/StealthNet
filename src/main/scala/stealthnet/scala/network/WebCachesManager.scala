package stealthnet.scala.network

import scala.actors.Actor
import stealthnet.scala.Settings
import stealthnet.scala.core.Core
import stealthnet.scala.util.Peer
import stealthnet.scala.util.log.{EmptyLoggingContext, Logging}
import stealthnet.scala.webservices.{UpdateClient, WebCacheClient}

/**
 * Manages WebCaches.
 *
 * The actual implementation uses actor messages to ensure thread-safety, and
 * provides methods to interact with the manager without worrying about it.
 */
object WebCachesManager
  extends Actor
  with Logging
  with EmptyLoggingContext
{

  /** Known WebCaches. */
  private var webCaches: List[String] = Nil
  /** WebCaches to check. */
  private var toCheck: List[String] = Nil
  /** Cyclic iterator on WebCaches. */
  private var cyclicIt: Iterator[String] = Nil.iterator
  /** Whether we are currently added on WebCaches. */
  private var addedPeer = false
  /** Whether WebCaches check is ongoing. */
  private var checkOngoing = false

  /** Actor message: refresh WebCaches. */
  protected case class Refresh()
  /** Actor message: add (self) peer to WebCaches. */
  protected case class AddPeer()
  /** Actor message: remove (self) peer to WebCaches. */
  protected case class RemovePeer()
  /** Actor message: get peer from WebCaches. */
  protected case class GetPeer()
  /** Actor message: checks WebCaches for which peer adding failed. */
  protected case class CheckWebCaches()
  /** Actor message: stop. */
  protected case class Stop()

  /**
   * Manages this actor messages.
   */
  def act() {
    loop {
      react {
        case Refresh() =>
          _refresh()

        case AddPeer() =>
          _addPeer()

        case RemovePeer() =>
          _removePeer()

        case GetPeer() =>
          reply(_getPeer())

        case CheckWebCaches() =>
          checkWebCaches()

        case Stop() =>
          exit()
      }
    }
  }

  /**
   * Refreshes WebCaches.
   *
   * Removes ourself, get updated list of WebCaches, and re-adds ourself if needed.
   *
   * If no WebCache could be retrieved, use default (configured) ones.
   */
  protected def _refresh() = {
    val readd = addedPeer
    _removePeer()

    Settings.core.wsWebCacheUpdateEnabled
    val unfiltered: List[String] = (if (Settings.core.wsWebCacheUpdateEnabled)
        UpdateClient.getWebCaches(Settings.core.wsWebCacheUpdateURL)
      else
        None
    ) match {
      case Some(urls) =>
        logger debug("Got webCaches: " + urls)
        urls

      case None =>
        val default = Settings.core.wsWebCacheDefault
        if (Settings.core.wsWebCacheUpdateEnabled)
          logger debug("Got no webCache: using default[" + default + "]")
        else
          logger debug("WebCache update disabled: using default[" + default + "]")
        default
    }

    val excluded = Settings.core.wsWebCacheExcluded
    webCaches = unfiltered filterNot { webCache =>
      excluded.find { regex =>
        val filtered = regex.r findFirstIn(webCache) isDefined

        if (filtered)
          logger debug("WebCache[" + webCache + "] excluded[" + regex + "]")

        filtered
      } isDefined
    }

    cyclicIt = webCaches.iterator

    if (readd)
      _addPeer()
  }

  /**
   * Removes ourself from WebCaches.
   *
   * Done if we were added.
   */
  protected def _removePeer() =
    if (addedPeer) {
      for (webCache <- webCaches)
        WebCacheClient.removePeer(webCache)
      toCheck = Nil
      /* Note: do not reset checkOngoing; done in checkWebCaches */
      addedPeer = false
    }

  /**
   * Adds ourself on WebCaches.
   *
   * Done if we were not already added.
   */
  protected def _addPeer() =
    if (!addedPeer && !Core.stopping) {
      _removePeer()

      val results = for (webCache <- webCaches)
        yield (webCache -> WebCacheClient.addPeer(webCache, Settings.core.serverPort))

      toCheck = results filterNot { _._2 } map { _._1 }
      if (!toCheck.isEmpty && !checkOngoing) {
        this ! CheckWebCaches()
        checkOngoing = true
      }

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
  protected def _getPeer(): Option[Peer] = {
    if (webCaches.size == 0)
      return None

    if (!addedPeer)
      _addPeer()

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

  /**
   * Check WebCaches.
   *
   * Periodically tries to (re-)add ourself on WebCaches which failed.
   */
  protected def checkWebCaches() =
    if (addedPeer && !toCheck.isEmpty) {
      val results = for (webCache <- toCheck)
        yield (webCache -> WebCacheClient.addPeer(webCache, Settings.core.serverPort))

      toCheck = results filterNot { _._2 } map { _._1 }

      /* check again later */
      if (!toCheck.isEmpty)
        Core.schedule(this ! CheckWebCaches(), Settings.core.wsWebCacheCheckPeriod)
      else
        checkOngoing = true
    }
    else
      checkOngoing = false

  /** Refreshes WebCaches. */
  def refresh() = this ! Refresh()

  /** Removes ourself from WebCaches. */
  def removePeer() = this ! RemovePeer()

  /** Adds ourself on WebCaches. */
  def addPeer() = this ! AddPeer()

  /**
   * Gets a peer.
   *
   * @return an option value containing the retrieved peer, or `None` if none
   */
  def getPeer(): Option[Peer] =
    (this !? GetPeer()).asInstanceOf[Option[Peer]]

  /** Stops the manager. */
  def stop() = this ! Stop()

}
