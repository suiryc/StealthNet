package stealthnet.scala.ui.web.comet

import java.text.SimpleDateFormat
import org.cometd.bayeux.server.ServerSession
import scala.actors.Actor
import scala.collection.JavaConversions._
import stealthnet.scala.core.Core
import stealthnet.scala.network.connection.{
  ConnectionListener,
  StealthNetConnection
}
import stealthnet.scala.ui.web.Constants

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
{

  import ConnectionsNotifications._

  start
  this ! Start

  private def deliver(data: Map[String, Object]): Unit =
    deliver(session, "connections", data)

  def act() {
    loop {
      react {
        case Start =>
          ConnectionsNotificationsManager ! ConnectionsNotificationsManager.NewActor(this)

        case NewConnection(cnxInfo) =>
          val cnx = cnxInfo.cnx
          val output = Map[String, Object](
            "event" -> "new",
            "id" -> ("connection_" + cnxInfo.id),
            "host" -> cnx.peer.map(_.host).getOrElse("<unknown host>"),
            "port" -> (cnx.peer.map(_.port).getOrElse[Int](-1):java.lang.Integer),
            "created" -> new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(cnx.createDate),
            "receivedCommands" -> (cnx.receivedCommands:java.lang.Integer),
            "sentCommands" -> (cnx.sentCommands:java.lang.Integer),
            "status" -> (if (cnx.established) "established" else "initiated")
          )
          deliver(output)

        case RefreshConnection(cnxInfo) =>
          val cnx = cnxInfo.cnx
          val output = Map[String, Object](
            "event" -> "refresh",
            "id" -> ("connection_" + cnxInfo.id),
            "receivedCommands" -> (cnx.receivedCommands:java.lang.Integer),
            "sentCommands" -> (cnx.sentCommands:java.lang.Integer),
            "status" -> (if (cnx.established) "established" else "initiated")
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
          println("************************************************** Stopping ConnectionsNotificationsManager")
          exit()
      }
    }
  }

}

object ConnectionsNotificationsManager
  extends Actor
  with ConnectionListener
{

  import ConnectionListener._

  case class NewActor(actor: ConnectionsNotificationsManager)
  case class ActorLeft(session: ServerSession)
  /* XXX - manager connection updates (state, etc) and closing */
  private case class RefreshConnections()
  case class Stop()

  private var actors = Map.empty[String, Actor]
  private var connections = List.empty[ConnectionInfo]
  private var id: Int = _
  private var refreshRunning = false

  // scalastyle:off method.length
  def act() {
    loop {
      react {
        case NewActor(actor) =>
          actors += actor.session.getId -> actor
          /* notify the new actor about current connections */
          connections foreach { actor ! ConnectionsNotifications.NewConnection(_) }
          if (!refreshRunning) {
            refreshRunning = true
            this ! RefreshConnections()
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

        case RefreshConnections() =>
          if (refreshRunning) {
            connections foreach { cnxInfo =>
              actors foreach { _._2 ! ConnectionsNotifications.RefreshConnection(cnxInfo) }
            }
            Core.schedule(this ! RefreshConnections(), Constants.cnxInfoRefreshPeriod)
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

        case Stop() =>
          actors foreach { _._2 ! ConnectionsNotifications.Stop() }
          exit()
      }
    }
  }
  // scalastyle:on method.length

  def stop() = this ! Stop()

}
