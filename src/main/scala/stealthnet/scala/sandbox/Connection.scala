package stealthnet.scala.sandbox

import java.net.{Socket, SocketTimeoutException}
import com.weiglewilczek.slf4s.Logging
import scala.actors._

object Connection {
  
  object Action extends Enumeration {
    val WaitData, Stop = Value
  }

}

class Connection(val socket: Socket) extends Actor with Logging {

  import stealthnet.scala.sandbox.Connection._

  def act() {
    /* XXX - get from configuration */
    val socketTimeout = 5000

    logger debug "Connection starting"

    socket.setSoTimeout(socketTimeout)

    /* prime the pump */
    this ! Action.WaitData

    processData()
  }

  protected def processData() {
    loop {
      react {
        case Action.WaitData =>
          try {
            /* XXX - TODO */
            /* XXX - use java.nio.channels.ServerSocketChannel, java.nio.channels.SocketChannel and java.nio.channels.Selector */
            Thread.sleep(5000)
            throw new SocketTimeoutException
          }
          catch {
            case e: SocketTimeoutException =>
              /* loop */
              logger error "Socket timeout"
              this ! Action.WaitData
            
            case e: Exception =>
              logger error ("Socket failed!",  e)
              exit()
          }

        case Action.Stop =>
          /* time to leave */
          logger debug "Socket stopping"
          socket.close()
          exit()
      }
    }
  }

}
