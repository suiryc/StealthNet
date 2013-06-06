package stealthnet.scala.ui.web.beans

import javax.faces.bean.{ApplicationScoped, ManagedBean}
import javax.faces.event.ActionEvent
import akka.actor._
import org.primefaces.context.RequestContext
import stealthnet.scala.core.Core
import stealthnet.scala.ui.web.Server

object ServerBean {

  /* XXX: migrate to akka
   *  - use akka logging ?
   *  - refactor classes ?
   */

  case object Stop

  private class ServerBeanActor extends Actor {
    override def receive = {
      case Stop =>
        Server.stop()
        context.stop(self)
    }
  }

  private val actor = ActorDSL.actor(Core.actorSystem.system, "UI-ServerBean")(
    new ServerBeanActor
  )
  Core.actorSystem.watch(actor)

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
