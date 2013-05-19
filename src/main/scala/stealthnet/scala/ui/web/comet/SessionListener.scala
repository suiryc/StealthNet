package stealthnet.scala.ui.web.comet

import javax.servlet.ServletConfig
import javax.servlet.http.{HttpSession, HttpSessionEvent, HttpSessionListener}
import akka.actor._
import akka.actor.ActorDSL._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent._
import scala.concurrent.duration._
import scala.collection.mutable

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
   *  - create actor class inside object: DONE
   *  - instantiate actor inside object: DONE
   *  - update API methods to use instantiated actor: DONE
   *  - shutdown system: DONE
   *  - use less ambiguous Stop message
   *  - use same system for all actors
   *    - use actorOf to create actor ?
   *    - address messages inside object to prevent ambiguous names ?
   *  - use a shutdown pattern ? (http://letitcrash.com/post/30165507578/shutdown-patterns-in-akka-2)
   *  - use akka logging ?
   *  - cleanup
   */
  implicit val timeout = Timeout(36500.days)

  private val sessions = mutable.Map[String, HttpSession]()

  case class AddSession(session: HttpSession)
  case class RemoveSession(session: HttpSession)
  case class GetSession(id: String)
  case object Stop

  private class SessionManagerActor extends ActWithStash {
    override def postStop() = context.system.shutdown()
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

  private val system = ActorSystem("SessionManager")
  private lazy val actor = ActorDSL.actor(system)(new SessionManagerActor)

  def addSession(session: HttpSession) = actor ! AddSession(session)

  def removeSession(session: HttpSession) = actor ! RemoveSession(session)

  def getSession(id: String) =
    Await.result(actor ? GetSession(id), Duration.Inf).asInstanceOf[Option[HttpSession]]

  def stop() = actor ! Stop

  /** Dummy method to start the manager. */
  def start() {
    actor
  }

}
