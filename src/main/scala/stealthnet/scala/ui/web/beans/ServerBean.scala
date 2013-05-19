package stealthnet.scala.ui.web.beans

import javax.faces.bean.{ApplicationScoped, ManagedBean}
import javax.faces.event.ActionEvent
import akka.actor._
import akka.actor.ActorDSL._
import org.primefaces.context.RequestContext
import stealthnet.scala.ui.web.Server

object ServerBean {

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

  case object Stop

  private class ServerBeanActor extends ActWithStash {
    override def postStop() = context.system.shutdown()
    override def receive = {
      case Stop =>
        Server.stop()
        context.stop(self)
    }
  }

  private val system = ActorSystem("ServerBean")
  private lazy val actor = ActorDSL.actor(system)(new ServerBeanActor)

}

@ManagedBean
@ApplicationScoped
class ServerBean extends Serializable {

  def shutdown(actionEvent: ActionEvent) = {
    /* Let the client know we acknowledged the request */
    RequestContext.getCurrentInstance().addCallbackParam("acknowledged", true)
    /* Note we could also execute ourself the remote JavaScript function (would
     * require to catch issues with 'onerror' Ajax callback):
    RequestContext.getCurrentInstance().execute("Ext.functions.shutdown()")
     */

    /* We need to delegate server closing since we are processing a request */
    ServerBean.actor ! ServerBean.Stop
  }

}
