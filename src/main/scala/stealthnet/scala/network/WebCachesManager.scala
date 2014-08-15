package stealthnet.scala.network

import akka.actor._
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
 *
 * XXX - use akka.io/actors and do not block with WebCache client actions
 */
object WebCachesManager {

  /* XXX: use akka logging ? */

  /** Actor message: refresh WebCaches. */
  case object Refresh
  /** Actor message: add (self) peer to WebCaches. */
  case object AddPeer
  /** Actor message: remove (self) peer to WebCaches. */
  case object RemovePeer
  /** Actor message: get peer from WebCaches. */
  case object GetPeer
  /** Actor message: got peer from WebCaches. */
  case class GotPeer(peer: Peer)
  /** Actor message: checks WebCaches for which peer adding failed. */
  protected case object CheckWebCaches
  /** Actor message: stop. */
  protected case object Stop

  private class WebCachesManagerActor
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

    /** Whether we are stopping. */
    private var stopping = false

    /**
     * Manages this actor messages.
     */
    override def receive = {
      case Refresh =>
        refresh()

      case AddPeer =>
        addPeer()

      case RemovePeer =>
        removePeer()

      case GetPeer =>
        getPeer.foreach { sender ! GotPeer(_) }

      case CheckWebCaches =>
        checkWebCaches()

      case Stop =>
        context.stop(self)
    }

    /**
     * Refreshes WebCaches.
     *
     * Removes ourself, get updated list of WebCaches, and re-adds ourself if needed.
     *
     * If no WebCache could be retrieved, use default (configured) ones.
     */
    protected def refresh() = {
      val readd = addedPeer
      removePeer()

      val unfiltered: List[String] = (if (Settings.core.wsWebCacheUpdateEnabled)
          UpdateClient.getWebCaches(Settings.core.wsWebCacheUpdateURL)
        else
          None
      ) match {
        case Some(urls) =>
          logger.debug(s"Got webCaches: $urls")
          urls

        case None =>
          val default = Settings.core.wsWebCacheDefault
          if (Settings.core.wsWebCacheUpdateEnabled)
            logger.debug(s"Got no webCache: using default[$default]")
          else
            logger.debug(s"WebCache update disabled: using default[$default]")
          default
      }

      val excluded = Settings.core.wsWebCacheExcluded
      webCaches = unfiltered filterNot { webCache =>
        excluded.exists { regex =>
          val filtered = regex.r.findFirstIn(webCache).isDefined

          if (filtered)
            logger.debug(s"WebCache[$webCache] excluded[$regex]")

          filtered
        }
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
    protected def removePeer() =
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
    protected def addPeer() =
      if (!addedPeer && !Core.stopping) {
        removePeer()

        val results = for (webCache <- webCaches)
          yield (webCache, WebCacheClient.addPeer(webCache, Settings.core.serverPort))

        toCheck = results filterNot { _._2 } map { _._1 }
        if (toCheck.nonEmpty && !checkOngoing) {
          self ! CheckWebCaches
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
    protected def getPeer: Option[Peer] = {
      if (webCaches.size == 0)
        return None

      if (!addedPeer)
        addPeer()

      for (i <- 1 to webCaches.size) {
        val webCache = cyclicIt.next()
        if (!cyclicIt.hasNext)
          cyclicIt = webCaches.iterator

        WebCacheClient.getPeer(webCache) match {
          case None =>

          case v =>
            return v
        }
      }

      None
    }

    /**
     * Check WebCaches.
     *
     * Periodically tries to (re-)add ourself on WebCaches which failed.
     */
    protected def checkWebCaches() =
      if (addedPeer && toCheck.nonEmpty) {
        val results = for (webCache <- toCheck)
          yield (webCache, WebCacheClient.addPeer(webCache, Settings.core.serverPort))

        toCheck = results filterNot { _._2 } map { _._1 }

        /* check again later */
        if (toCheck.nonEmpty)
          Core.schedule(self ! CheckWebCaches,
            Settings.core.wsWebCacheCheckPeriod)
        else
          checkOngoing = true
      }
      else
        checkOngoing = false

  }

  val actor = Core.actorSystem.system.actorOf(Props[WebCachesManagerActor], "WebCachesManager")
  Core.actorSystem.watch(actor)

  /** Stops the manager. */
  def stop() = actor ! Stop

  /** Dummy method to start the manager. */
  def start() { }

}
