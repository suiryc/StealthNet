package perso.stealthnet.network

import java.net.ServerSocket
import java.net.SocketTimeoutException
import scala.actors._
import scala.collection.mutable.MutableList

object Server extends Actor {

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

    /* XXX - debug */
    println("Server starting")

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
        	/* XXX - log */
        	println("Accepted remote connection[" + socket.getInetAddress.getHostAddress + ":" + socket.getPort + "]")
        	val connection = new Connection(socket)
        	sockets += connection
        	connection.start()
          }
          catch {
            case e: SocketTimeoutException =>
              /* loop */
              /* XXX - debug */
              println("Server timeout on accept")
              this ! Action.WaitConnection
            
            case e: Exception =>
              /* XXX - log issue */
              println("Server failed: " + e)
              cleanup()
          }

        case Action.Stop =>
          /* time to leave */
          cleanup()
      }
    }
  }

  protected def cleanup() {
    /* XXX - debug */
    println("Server stopping")
    server.close()

    for (connection <- sockets)
      connection ! Connection.Action.Stop
    sockets = MutableList.empty

    exit()
  }

}
