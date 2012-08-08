package stealthnet.scala.ui.web.comet

import javax.servlet.ServletConfig
import javax.servlet.http.{HttpSession, HttpSessionEvent, HttpSessionListener}
import scala.actors.Actor
import scala.collection.mutable

class SessionListener extends HttpSessionListener {

  def init(config: ServletConfig) {
  }

  def sessionCreated(event: HttpSessionEvent) =
    SessionManager.addSession(event.getSession)

  def sessionDestroyed(event: HttpSessionEvent) =
    SessionManager.removeSession(event.getSession)

}

object SessionManager extends Actor {

  private val sessions = mutable.Map[String, HttpSession]()

  case class AddSession(session: HttpSession)
  case class RemoveSession(session: HttpSession)
  case class GetSession(id: String)
  case class Stop()

  def act() {
    loop {
      react {
        case AddSession(session) =>
          /* XXX - log */
          sessions += session.getId -> session

        case RemoveSession(session) =>
          sessions -= session.getId

        case GetSession(id) =>
          reply(sessions.get(id))

        case Stop() =>
          exit()
      }
    }
  }

  def addSession(session: HttpSession) = this ! AddSession(session)

  def removeSession(session: HttpSession) = this ! RemoveSession(session)

  def getSession(id: String) =
    (this !? GetSession(id)).asInstanceOf[Option[HttpSession]]

  def stop() = this ! Stop()

}
