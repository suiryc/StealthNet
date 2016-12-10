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

  def sessionCreated(event: HttpSessionEvent): Unit =
    SessionManager.actor ! SessionManager.AddSession(event.getSession)

  def sessionDestroyed(event: HttpSessionEvent): Unit =
    SessionManager.actor ! SessionManager.RemoveSession(event.getSession)

}

object SessionManager {

  /* XXX: migrate to akka
   *  - use akka logging ?
   *  - refactor classes ?
   */
  implicit private val timeout = Timeout(1.hour)

  private val sessions = mutable.Map[String, HttpSession]()

  case class AddSession(session: HttpSession)
  case class RemoveSession(session: HttpSession)
  case class GetSession(id: String)
  case object Stop

  private class SessionManagerActor extends Actor {
    override def receive: Receive = {
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

  val actor: ActorRef = Core.actorSystem.system.actorOf(Props[SessionManagerActor], "UI-SessionManager")
  Core.actorSystem.watch(actor)

  /* XXX - alternative solution without blocking (here) ? */
  def getSession(id: String): Option[HttpSession] =
    Await.result(actor ? GetSession(id), Duration.Inf).asInstanceOf[Option[HttpSession]]

  def stop(): Unit = actor ! Stop

  /** Dummy method to start the manager. */
  def start() { }

}
