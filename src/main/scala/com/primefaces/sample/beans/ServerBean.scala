package com.primefaces.sample.beans

import javax.faces.bean.{ApplicationScoped, ManagedBean}
import javax.faces.event.ActionEvent
import scala.actors.Actor
import com.primefaces.sample.Server

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

  def shutdown(actionEvent: ActionEvent) =
    /* We need to delegate server closing since we are processing a request */
    ServerBean ! ServerBean.Stop()

}
