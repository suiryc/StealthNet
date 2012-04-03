package perso.stealthnet.network

import java.net.Socket
import scala.actors._
import java.net.SocketTimeoutException

object Connection {
  
  object Action extends Enumeration {
    val WaitData, Stop = Value
  }

}

class Connection(val socket: Socket) extends Actor {

  import perso.stealthnet.network.Connection._

  def act() {
    /* XXX - get from configuration */
    val socketTimeout = 5000

    /* XXX - debug */
    println("Connection starting")

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
              /* XXX - debug */
              println("Socket timeout")
              this ! Action.WaitData
            
            case e: Exception =>
              /* XXX - log issue */
              println("Socket failed: " + e)
              exit()
          }

        case Action.Stop =>
          /* time to leave */
          /* XXX - debug */
          println("Socket stopping")
          socket.close()
          exit()
      }
    }
  }

}
