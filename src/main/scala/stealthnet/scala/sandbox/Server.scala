package stealthnet.scala.sandbox

import java.net.{ServerSocket, SocketTimeoutException}
import com.weiglewilczek.slf4s.Logging
import scala.actors._
import scala.collection.mutable.MutableList

object Server extends Actor with Logging {

  object Action extends Enumeration {
    val WaitConnection, Stop = Value
  }

  /* XXX - get from configuration */
  /* XXX - but has to be known for AddPeer */
  val port = 6666

  protected var server: ServerSocket = null
  protected var sockets: MutableList[Connection] = MutableList.empty

  def act() {
    /* XXX - get from configuration */
    val acceptTimeout = 5000

    logger info "Server starting"

    /* Note: setSoTimeout impacts accept() */
    server = new ServerSocket(port)
    server.setSoTimeout(acceptTimeout)

    /* prime the pump */
    this ! Action.WaitConnection

    acceptConnections()
  }

  protected def acceptConnections() {
    loop {
      react {
        case Action.WaitConnection =>
          try {
        	val socket = server.accept()
        	logger debug("Accepted remote connection[" + socket.getInetAddress.getHostAddress + ":" + socket.getPort + "]")
        	val connection = new Connection(socket)
        	sockets += connection
        	connection.start()
          }
          catch {
            case e: SocketTimeoutException =>
              /* loop */
              logger debug "Server timeout on accept"
              this ! Action.WaitConnection
            
            case e: Exception =>
              logger error ("Server failed!", e)
              cleanup()
          }

        case Action.Stop =>
          /* time to leave */
          cleanup()
      }
    }
  }

  protected def cleanup() {
    logger info "Server stopping"
    server.close()

    for (connection <- sockets)
      connection ! Connection.Action.Stop
    sockets = MutableList.empty

    exit()
  }

}
