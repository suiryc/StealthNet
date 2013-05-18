package stealthnet.scala.network.connection

/**
 * Connection listener companion object.
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

/* XXX - use a type alias of ActorRef ? */
/**
 * Connection listener.
 *
 * Basically, a listener is an actor. But there actually are many kind (that is
 * implementations) of actors. The common trait being the `!` method used to
 * send a message.
 */
trait ConnectionListener {

  // scalastyle:off method.name
  def !(msg: Any): Unit
  // scalastyle:on method.name

}
