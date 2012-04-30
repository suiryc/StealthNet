package stealthnet.scala.ui.web.comet

import scala.xml.{NodeSeq, Text}
import net.liftweb.actor.LiftActor
import net.liftweb.http.CometActor
//import net.liftweb.http.js.JsCmds
import net.liftweb.http.js.jquery.JqJsCmds
import stealthnet.scala.network.StealthNetConnection

/* XXX - manager connection updates (state, etc) and closing */
case class NewConnection(cnx: StealthNetConnection)

class ConnectionsUpdater extends CometActor {

  case object Start

  private lazy val listId = "connections_list"
  private lazy val row = defaultHtml

  override def lowPriority = {
    case Start =>
      ConnectionsUpdaterServer ! ConnectionsUpdaterServer.NewActor(this)

    case NewConnection(cnx) =>
      partialUpdate(JqJsCmds.AppendHtml(listId, 
          bind("connection", row,
            "host" -> cnx.peer.host,
            "port" -> cnx.peer.port,
            "misc" -> "<>")
        )
      )
  }

  override def render = {
    this ! Start

    NodeSeq.Empty
  }

}

object ConnectionsUpdaterServer extends LiftActor {

  case class NewActor(actor: CometActor)

  private var actors = List.empty[CometActor]
  var connections = List.empty[StealthNetConnection]

  override def messageHandler = {
    case NewActor(actor) =>
      actors ::= actor
      /* tell the new actor about current connections */
      connections foreach { actor ! NewConnection(_) }

    case msg @ NewConnection(cnx) =>
      connections ::= cnx
      /* tell each actor about the new connection */
      actors foreach { _ ! msg }
  }
  
}
