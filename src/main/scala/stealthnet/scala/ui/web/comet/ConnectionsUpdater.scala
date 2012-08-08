package stealthnet.scala.ui.web.comet

import java.text.SimpleDateFormat
import org.cometd.bayeux.Message
import org.cometd.bayeux.server.{BayeuxServer, ServerSession}
import org.cometd.server.AbstractService
import scala.actors.Actor
import scala.collection.JavaConversions._
import scala.xml.{NodeSeq, Text}
import stealthnet.scala.core.Core
import stealthnet.scala.network.{ConnectionListener, StealthNetConnection}

object ConnectionsUpdater {

  case object Start
  case class NewConnection(cnxInfo: ConnectionInfo)
  case class RefreshConnection(cnxInfo: ConnectionInfo)
  case class ClosedConnection(cnxInfo: ConnectionInfo)
  case class Stop()

  private lazy val listId = "connections_list"

}

class ConnectionsUpdater(val session: ServerSession)
  extends Actor
{

  import ConnectionsUpdater._

  def act() {
    loop {
      react {
        case Start =>
          ConnectionsUpdaterServer ! ConnectionsUpdaterServer.NewActor(this)

        case NewConnection(cnxInfo) =>
          val cnx = cnxInfo.cnx
          val output = Map[String, Object](
            "event" -> "new",
            "id" -> ("connection_" + cnxInfo.id),
            "host" -> cnx.peer.host,
            "port" -> (cnx.peer.port:java.lang.Integer),
            "created" -> new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(cnx.createDate),
            "receivedCommands" -> (cnx.receivedCommands:java.lang.Integer),
            "sentCommands" -> (cnx.sentCommands:java.lang.Integer),
            "misc" -> (if (cnx.established) "established" else "initiated")
          )
          session.deliver(session, "/cnxUpdates", output:java.util.Map[String, Object], null);

        case RefreshConnection(cnxInfo) =>
          val cnx = cnxInfo.cnx
          val output = Map[String, Object](
            "event" -> "refresh",
            "id" -> ("connection_" + cnxInfo.id),
            "receivedCommands" -> (cnx.receivedCommands:java.lang.Integer),
            "sentCommands" -> (cnx.sentCommands:java.lang.Integer),
            "misc" -> (if (cnx.established) "established" else "initiated")
          )
          session.deliver(session, "/cnxUpdates", output:java.util.Map[String, Object], null);

        case ClosedConnection(cnxInfo) =>
          val cnx = cnxInfo.cnx
          val output = Map[String, Object](
            "event" -> "closed",
            "id" -> ("connection_" + cnxInfo.id)
          )
          session.deliver(session, "/cnxUpdates", output:java.util.Map[String, Object], null);

        case Stop() =>
          exit()
      }
    }
  }

  this.start
  this ! Start

}

class ConnectionInfo(val cnx: StealthNetConnection, val id: Int)

object ConnectionsUpdaterServer extends Actor with ConnectionListener {

  import ConnectionListener._

  case class NewActor(actor: ConnectionsUpdater)
  case class ActorLeft(session: ServerSession)
  /* XXX - manager connection updates (state, etc) and closing */
  private case class RefreshConnections()
  case class Stop()

  private var actors = Map.empty[String, Actor]
  private var connections = List.empty[ConnectionInfo]
  private var id: Int = _
  private var refreshRunning = false

  def act() {
    loop {
      react {
        case NewActor(actor) =>
          actors += actor.session.getId -> actor
          /* notify the new actor about current connections */
          connections foreach { actor ! ConnectionsUpdater.NewConnection(_) }
          if (!refreshRunning) {
            refreshRunning = true
            this ! RefreshConnections()
          }

        case ActorLeft(session) =>
          actors.get(session.getId) match {
            case Some(actor) =>
              actor ! ConnectionsUpdater.Stop()
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
          actors foreach { _._2 ! ConnectionsUpdater.NewConnection(cnxInfo) }

        case RefreshConnections() =>
          if (refreshRunning) {
            connections foreach { cnxInfo =>
              actors foreach { _._2 ! ConnectionsUpdater.RefreshConnection(cnxInfo) }
            }
            Core.schedule(this ! RefreshConnections(), 10000)
          }
          /* else: refresh stopped */

        case ClosedConnection(cnx) =>
          /* remove the connection and notify each actor */
          val (left, right) = connections span { _.cnx.channel eq cnx.channel }
          connections = right
          left foreach { cnxInfo =>
            actors foreach { _._2 ! ConnectionsUpdater.ClosedConnection(cnxInfo) }
          }

        case Stop() =>
          actors foreach { _._2 ! ConnectionsUpdater.Stop() }
          exit()
      }
    }
  }

  def stop() = this ! Stop()

}

class ConnectionsUpdaterService(bayeux: BayeuxServer)
  extends AbstractService(bayeux, "cnxUpdater")
{

  addService("/service/cnxUpdater", "processClient")

  def processClient(remote: ServerSession, message: Message) {
    val input = message.getDataAsMap()
    val active = input.get("active").asInstanceOf[String] match {
      case null =>
        false

      case v =>
        v.toBoolean
    }

    if (active)
      new ConnectionsUpdater(remote)
    else
      ConnectionsUpdaterServer ! ConnectionsUpdaterServer.ActorLeft(remote)
  }

}
