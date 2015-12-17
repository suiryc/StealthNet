package stealthnet.scala.network.connection

/**
 * Connection listener object.
 *
 * Defines messages that can be notified to listeners.
 */
object ConnectionListener {

  /* XXX - manager connection updates (state, etc) and closing */
  /** Message: new connection initiated. */
  case class NewConnection(cnx: StealthNetConnection)
  /** Message: connection closed. */
  case class ClosedConnection(cnx: StealthNetConnection)

}
