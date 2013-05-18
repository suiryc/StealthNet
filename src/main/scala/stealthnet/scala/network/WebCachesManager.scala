package stealthnet.scala.network

import akka.actor._
import akka.actor.ActorDSL._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent._
import scala.concurrent.duration._
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
object WebCachesManager {

  /* XXX: migrate to aka
   *  - create actor class inside object: DONE
   *  - instantiate actor inside object: DONE
   *  - update API methods to use instantiated actor: DONE
   *  - shutdown system: DONE
   *  - use less ambiguous Stop message
   *  - use same system for all actors
   *    - use actorOf to create actor ?
   *    - address messages inside object to prevent ambigous names ?
   *  - use a shutdown pattern ? (http://letitcrash.com/post/30165507578/shutdown-patterns-in-akka-2)
   *  - use akka logging ?
   *  - cleanup
   */
  implicit val timeout = Timeout(36500 days)

  /** Actor message: refresh WebCaches. */
  protected case object Refresh
  /** Actor message: add (self) peer to WebCaches. */
  protected case object AddPeer
  /** Actor message: remove (self) peer to WebCaches. */
  protected case object RemovePeer
  /** Actor message: get peer from WebCaches. */
  protected case object GetPeer
  /** Actor message: checks WebCaches for which peer adding failed. */
  protected case object CheckWebCaches
  /** Actor message: stop. */
  protected case object Stop

  private class WebCachesActor
    extends ActWithStash
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

    override def postStop() = context.system.shutdown()

    /**
     * Manages this actor messages.
     */
    override def receive = {
      case Refresh =>
        _refresh()

      case AddPeer =>
        _addPeer()

      case RemovePeer =>
        _removePeer()

      case GetPeer =>
        sender ! _getPeer()

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
    protected def _refresh() = {
      val readd = addedPeer
      _removePeer()

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
          val filtered = regex.r.findFirstIn(webCache).isDefined

          if (filtered)
            logger debug("WebCache[" + webCache + "] excluded[" + regex + "]")

          filtered
        } .isDefined
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
          Core.schedule(self ! CheckWebCaches, Settings.core.wsWebCacheCheckPeriod)
        else
          checkOngoing = true
      }
      else
        checkOngoing = false

  }

  private val system = ActorSystem("WebCaches")
  private lazy val actor = ActorDSL.actor(system)(new WebCachesActor)

  /** Refreshes WebCaches. */
  def refresh() = actor ! Refresh

  /** Removes ourself from WebCaches. */
  def removePeer() = actor ! RemovePeer

  /** Adds ourself on WebCaches. */
  def addPeer() = actor ! AddPeer

  /**
   * Gets a peer.
   *
   * @return an option value containing the retrieved peer, or `None` if none
   */
  def getPeer(): Option[Peer] =
    Await.result(actor ? GetPeer, Duration.Inf).asInstanceOf[Option[Peer]]

  /** Stops the manager. */
  def stop() = actor ! Stop

  /** Dummy method to start the manager. */
  def start() {
    actor
  }

}
