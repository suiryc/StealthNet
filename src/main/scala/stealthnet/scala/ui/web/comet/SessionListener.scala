package stealthnet.scala.ui.web.comet

import javax.servlet.ServletConfig
import javax.servlet.http.{HttpSession, HttpSessionEvent, HttpSessionListener}
import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent._
import scala.concurrent.duration._
import scala.collection.mutable
import stealthnet.scala.core.Core

class SessionListener extends HttpSessionListener {

  def init(config: ServletConfig) {
  }

  def sessionCreated(event: HttpSessionEvent) =
    SessionManager.addSession(event.getSession)

  def sessionDestroyed(event: HttpSessionEvent) =
    SessionManager.removeSession(event.getSession)

}

object SessionManager {

  /* XXX: migrate to akka
   *  - use akka logging ?
   *  - refactor classes ?
   */
  implicit val timeout = Timeout(1.hour)

  private val sessions = mutable.Map[String, HttpSession]()

  case class AddSession(session: HttpSession)
  case class RemoveSession(session: HttpSession)
  case class GetSession(id: String)
  case object Stop

  private class SessionManagerActor extends Actor {
    override def receive = {
      case AddSession(session) =>
        /* XXX - log */
        sessions += session.getId -> session

      case RemoveSession(session) =>
        sessions -= session.getId

      case GetSession(id) =>
        sender ! sessions.get(id)

      case Stop =>
        context.stop(self)
    }
  }

  private val actor = Core.actorSystem.system.actorOf(Props[SessionManagerActor], "UI-SessionManager")
  Core.actorSystem.watch(actor)

  def addSession(session: HttpSession) = actor ! AddSession(session)

  def removeSession(session: HttpSession) = actor ! RemoveSession(session)

  def getSession(id: String) =
    Await.result(actor ? GetSession(id), Duration.Inf).asInstanceOf[Option[HttpSession]]

  def stop() = actor ! Stop

  /** Dummy method to start the manager. */
  def start() { }

}
