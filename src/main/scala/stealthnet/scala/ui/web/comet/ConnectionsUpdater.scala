package stealthnet.scala.ui.web.comet

import java.text.SimpleDateFormat
import scala.xml.{NodeSeq, Text}
import net.liftweb.actor.LiftActor
import net.liftweb.http.CometActor
import net.liftweb.http.js.JE._
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.jquery.JqJsCmds
import stealthnet.scala.core.Core
import stealthnet.scala.network.StealthNetConnection

object ConnectionsUpdater {
  
  case object Start
  case class NewConnection(cnxInfo: ConnectionInfo)
  case class RefreshConnection(cnxInfo: ConnectionInfo)
  case class ClosedConnection(cnxInfo: ConnectionInfo)

  private lazy val listId = "connections_list"

}

class ConnectionsUpdater extends CometActor {

  import ConnectionsUpdater._

  private lazy val row = defaultHtml

  override def lowPriority = {
    case Start =>
      ConnectionsUpdaterServer ! ConnectionsUpdaterServer.NewActor(this)

    case NewConnection(cnxInfo) =>
      val cnx = cnxInfo.cnx
      partialUpdate(JqJsCmds.AppendHtml(listId, 
          bind("connection", row,
            AttrBindParam("id", "connection_" + cnxInfo.id, "id"),
            "host" -> cnx.peer.host,
            "port" -> cnx.peer.port,
            "created" -> new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(cnx.createDate),
            "receivedCommands" -> cnx.receivedCommands,
            "sentCommands" -> cnx.sentCommands,
            "misc" -> (if (cnx.established) "established" else "initiated")
          )
        )
      )

    case RefreshConnection(cnxInfo) =>
      val cnx = cnxInfo.cnx
      partialUpdate(Call(
          "connectionRefresh",
          Str("connection_" + cnxInfo.id),
          Num(cnx.receivedCommands),
          Num(cnx.sentCommands),
          Str(if (cnx.established) "established" else "initiated")
        )
      )

    case ClosedConnection(cnxInfo) =>
      /* XXX - it would be better to call a remote javascript function to do that ? */
      partialUpdate(Replace("connection_" + cnxInfo.id, NodeSeq.Empty))
  }

  override def render = {
    /* XXX - refreshing page does sometimes double the number of connected actors ... */
    this ! Start

    NodeSeq.Empty
  }

}

class ConnectionInfo(val cnx: StealthNetConnection, val id: Int)

object ConnectionsUpdaterServer extends LiftActor {

  case class NewActor(actor: CometActor)
  /* XXX - manager connection updates (state, etc) and closing */
  case class NewConnection(cnx: StealthNetConnection)
  private case class RefreshConnections()
  case class ClosedConnection(cnx: StealthNetConnection)

  private var actors = List.empty[CometActor]
  private var connections = List.empty[ConnectionInfo]
  private var id: Int = _
  private var refreshRunning = false

  override def messageHandler = {
    case NewActor(actor) =>
      /* XXX - way to get notified when actor leaves ? Then remove from list, and stop refresh if last */
      actors ::= actor
      /* notify the new actor about current connections */
      connections foreach { actor ! ConnectionsUpdater.NewConnection(_) }
      if (!refreshRunning) {
        refreshRunning = true
        this ! RefreshConnections()
      }

    case NewConnection(cnx) =>
      val cnxInfo = new ConnectionInfo(cnx, id)
      connections ::= cnxInfo
      id += 1
      /* notify each actor about the new connection */
      actors foreach { _ ! ConnectionsUpdater.NewConnection(cnxInfo) }

    case RefreshConnections() =>
      connections foreach { cnxInfo =>
        actors foreach { _ ! ConnectionsUpdater.RefreshConnection(cnxInfo) }
      }
      Core.schedule(this ! RefreshConnections(), 10000)

    case ClosedConnection(cnx) =>
      /* remove the connection and notify each actor */
      val (left, right) = connections span { _.cnx.channel eq cnx.channel }
      connections = right
      left foreach { cnxInfo =>
        actors foreach { _ ! ConnectionsUpdater.ClosedConnection(cnxInfo) }
      }
  }
  
}
