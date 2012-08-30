package stealthnet.scala.ui.web.beans

import javax.faces.bean.{ApplicationScoped, ManagedBean}
import javax.faces.event.ActionEvent
import scala.actors.Actor
import org.primefaces.context.RequestContext
import stealthnet.scala.ui.web.Server

object ServerBean extends Actor {

  case class Stop()

  start()

  def act() {
    loop {
      react {
        case Stop() =>
          Server.stop()
          exit()
      }
    }
  }

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
    ServerBean ! ServerBean.Stop()
  }

}
