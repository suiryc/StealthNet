package com.primefaces.sample

import javax.faces.bean.{ApplicationScoped, ManagedBean}
import scala.actors.Actor

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

  def shutdown() =
    /* We need to delegate server closing since we are processing a request */
    ServerBean ! ServerBean.Stop()

}
 