package stealthnet.scala.ui.web.comet

import java.text.SimpleDateFormat
import org.cometd.bayeux.server.ServerSession
import akka.actor._
import scala.collection.JavaConversions._
import stealthnet.scala.core.Core
import stealthnet.scala.network.connection.{
  ConnectionListener,
  StealthNetConnection
}
import stealthnet.scala.ui.web.Constants
import stealthnet.scala.util.log.{EmptyLoggingContext, Logging}

class ConnectionInfo(val cnx: StealthNetConnection, val id: Int)

object ConnectionsNotifications {

  case object Start
  case class NewConnection(cnxInfo: ConnectionInfo)
  case class RefreshConnection(cnxInfo: ConnectionInfo)
  case class ClosedConnection(cnxInfo: ConnectionInfo)
  case class Stop()

}

class ConnectionsNotificationsManager(protected val session: ServerSession)
  extends Actor
  with NotificationsManager
  with Logging
  with EmptyLoggingContext
{

  /* XXX: migrate to akka
   *  - use akka logging ?
   *  - refactor classes ?
   */

  import ConnectionsNotifications._

  logger trace s"Starting connections notifications manager[$this] session[$session]"
  self ! Start

  private def deliver(data: Map[String, Object]): Unit =
    deliver(session, "connections", data)

  override def receive = {
    case Start =>
      ConnectionsNotificationsManager.actor ! ConnectionsNotificationsManager.NewActor(self, session.getId)

    case NewConnection(cnxInfo) =>
      val cnx = cnxInfo.cnx
      val output = Map[String, Object](
        ("event", "new"),
        ("id", "connection_" + cnxInfo.id),
        ("host", cnx.peer.map(_.host).getOrElse("<unknown host>")),
        ("port", cnx.peer.map(_.port).getOrElse[Int](-1):java.lang.Integer),
        ("created", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(cnx.createDate)),
        ("receivedCommands", cnx.receivedCommands:java.lang.Integer),
        ("sentCommands", cnx.sentCommands:java.lang.Integer),
        ("status", if (cnx.established) "established" else "initiated")
      )
      deliver(output)

    case RefreshConnection(cnxInfo) =>
      val cnx = cnxInfo.cnx
      val output = Map[String, Object](
        ("event", "refresh"),
        ("id", "connection_" + cnxInfo.id),
        ("receivedCommands", cnx.receivedCommands:java.lang.Integer),
        ("sentCommands", cnx.sentCommands:java.lang.Integer),
        ("status", if (cnx.established) "established" else "initiated")
      )
      deliver(output)

    case ClosedConnection(cnxInfo) =>
      val cnx = cnxInfo.cnx
      val output = Map[String, Object](
        "event" -> "closed",
        "id" -> ("connection_" + cnxInfo.id)
      )
      deliver(output)

    case Stop() =>
      logger trace s"Stopping connections notifications manager[$this] session[$session]"
      context.stop(self)
  }

}

object ConnectionsNotificationsManager {

  /* XXX: migrate to akka
   *  - use akka logging ?
   *  - refactor classes ?
   */

  import ConnectionListener._

  case class NewActor(actor: ActorRef, id: String)
  case class ActorLeft(session: ServerSession)
  /* XXX - manager connection updates (state, etc) and closing */
  private case object RefreshConnections
  case object Stop

  private class ConnectionsNotificationsManagerActor extends Actor {

    private var actors = Map.empty[String, ActorRef]
    private var connections = List.empty[ConnectionInfo]
    private var id: Int = _
    private var refreshRunning = false

    // scalastyle:off method.length
    override def receive = {
      case NewActor(actor, id) =>
        actors += id -> actor
        /* notify the new actor about current connections */
        connections foreach { actor ! ConnectionsNotifications.NewConnection(_) }
        if (!refreshRunning) {
          refreshRunning = true
          self ! RefreshConnections
        }

      case ActorLeft(session) =>
        actors.get(session.getId) match {
          case Some(actor) =>
            actor ! ConnectionsNotifications.Stop()
            actors -= session.getId
            if (actors.isEmpty)
              refreshRunning = false

          case None =>
        }

      case NewConnection(cnx) =>
        val cnxInfo = new ConnectionInfo(cnx, id)
        connections ::= cnxInfo
        id += 1
        /* notify each actor about the new connection */
        actors foreach { _._2 ! ConnectionsNotifications.NewConnection(cnxInfo) }

      case RefreshConnections =>
        if (refreshRunning) {
          connections foreach { cnxInfo =>
            actors foreach { _._2 ! ConnectionsNotifications.RefreshConnection(cnxInfo) }
          }
          Core.schedule(self ! RefreshConnections, Constants.cnxInfoRefreshPeriod)
        }
        /* else: refresh stopped */

      case ClosedConnection(cnx) =>
        /* remove the connection and notify each actor */
        /* Note: comparison on cnx or cnx.channel reference should work, but
         * comparing the channel id shall be more future-proof.
         */
        val (left, right) = connections partition { _.cnx.channel.getId == cnx.channel.getId }
        connections = right
        left foreach { cnxInfo =>
          actors foreach { _._2 ! ConnectionsNotifications.ClosedConnection(cnxInfo) }
        }

      case Stop =>
        actors foreach { _._2 ! ConnectionsNotifications.Stop() }
        context.stop(self)
    }
    // scalastyle:on method.length

  }

  val actor = ActorDSL.actor(Core.actorSystem.system, "UI-ConnectionsNotificationsManager")(
    new ConnectionsNotificationsManagerActor
  )
  Core.actorSystem.watch(actor)

  def stop() = actor ! Stop

  /** Dummy method to start the manager. */
  def start() { }

  def register(session: ServerSession) {
    ActorDSL.actor(Core.actorSystem.system)(
      new ConnectionsNotificationsManager(session)
    )
  }

  def unregister(session: ServerSession) {
    actor ! ConnectionsNotificationsManager.ActorLeft(session)
  }

}
