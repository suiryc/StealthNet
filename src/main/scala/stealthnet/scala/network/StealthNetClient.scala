package stealthnet.scala.network

import java.net.InetSocketAddress
import java.util.concurrent.Executors
import org.jboss.netty.bootstrap.ClientBootstrap
import org.jboss.netty.channel.{Channel, ChannelFactory}
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory
import stealthnet.scala.Config
import stealthnet.scala.util.Peer
import stealthnet.scala.util.log.Logging

/**
 * ''StealthNet'' client companion object.
 *
 * Manages client-side shared resources.
 */
object StealthNetClient {

  /** Channel factory. */
  private val factory: ChannelFactory = new NioClientSocketChannelFactory(
    Executors.newCachedThreadPool(),
    Executors.newCachedThreadPool()
  )

  /**
   * Cleans shared resources.
   */
  def stop() =
    factory.releaseExternalResources()

}

/**
 * ''StealthNet'' client.
 *
 * Manages client-side actions.
 */
class StealthNetClient(
  /** Remote peer to connect to. */
  val peer: Peer
) extends Logging
{

  import StealthNetClient._

  protected def loggerContext = List("peer" -> peer)

  /** Client connection channel. */
  private var channel: Option[Channel] = None

  /**
   * Starts client.
   *
   * First registers the remote peer as a to-be
   * [[stealthnet.scala.network.StealthNetConnection]]. If connection limit is
   * reached, nothing is done and this method returns `false`, otherwise
   * connection is attempted to remote peer.
   *
   * If connection was successful, `stop` should be called to stop the client.
   *
   * @return `true` if connection succeeded, `false` otherwise
   * @see [[stealthnet.scala.network.StealthNetConnectionsManager]].`addPeer`
   */
  def start(): Boolean = {
    if (!StealthNetConnectionsManager.addPeer(peer))
      return false

    val bootstrap: ClientBootstrap = new ClientBootstrap(factory)

    /* Add the channel to the (server) group: will be closed with server. */
    bootstrap.setPipelineFactory(StealthNetPipelineFactory(
      new StealthNetConnectionParameters(
        group = Some(StealthNetServer.group),
        client = Some(this),
        peer = Some(peer)
      )
    ))

    bootstrap.setOption("connectTimeoutMillis", Config.connectTimeout)

    val future = bootstrap.connect(new InetSocketAddress(peer.host, peer.port))
    future.awaitUninterruptibly()
    if (!future.isSuccess) {
      logger debug("Failed to connect", future.getCause)
      stop()
      false
    }
    else {
      channel = Some(future.getChannel)
      true
    }
  }

  /**
   * Stops client.
   *
   * Closes opened channel and cleans resources.
   * Should be called when connection was successfully started.
   */
  def stop() {
    if (channel map(_.isOpen) getOrElse(false)) {
      logger debug("Closing connection")

      val ch = channel.get
      StealthNetConnectionsManager getConnection(ch) map {
        _.close()
      } getOrElse {
        ch.close()
      }
      ch.getCloseFuture.awaitUninterruptibly()
    }
    /* Note: pipeline factory resources are released by server */
  }

  /** Writes to the channel. */
  def write(obj: Any) = channel.map(_.write(obj))
    .getOrElse(throw new Exception("Cannot write: channel not yet created"))

}
