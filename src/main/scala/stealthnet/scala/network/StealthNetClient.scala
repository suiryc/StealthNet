package stealthnet.scala.network

import java.net.InetSocketAddress
import java.util.concurrent.Executors
import org.jboss.netty.bootstrap.ClientBootstrap
import org.jboss.netty.channel.{Channel, ChannelFactory}
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory
import stealthnet.scala.Config
import stealthnet.scala.util.{Logging, Peer}

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

  protected def loggerContext = List("peer" -> peer)

  /** Channel factory. */
  private var factory: ChannelFactory = null
  /** Client connection channel. */
  private var channel: Channel = null

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

    factory = new NioClientSocketChannelFactory(
      Executors.newCachedThreadPool(),
      Executors.newCachedThreadPool()
    )

    val bootstrap: ClientBootstrap = new ClientBootstrap(factory)

    bootstrap.setPipelineFactory(StealthNetPipelineFactory(
      new StealthNetConnectionParameters(client = this, peer = peer)))

    /* XXX - useful ? */
    bootstrap.setOption("tcpNoDelay", true)
    bootstrap.setOption("keepAlive", true)
    bootstrap.setOption("connectTimeoutMillis", Config.connectTimeout)

    val future = bootstrap.connect(new InetSocketAddress(peer.host, peer.port))
    future.awaitUninterruptibly()
    if (!future.isSuccess) {
      logger error("Failed to connect", future.getCause)
      stop()
      false
    }
    else {
      channel = future.getChannel
      /* Add the channel to the (server) group: will be closed with server. */
      /* XXX - so no need to do it in connections manager ? */
      StealthNetServer.group.add(channel)
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
    if ((channel != null) && channel.isOpen) {
      logger debug("Closing connection")

      StealthNetConnectionsManager.getConnection(channel) match {
        case Some(cnx) =>
          cnx.close()

        case None =>
          channel.close()
      }
      channel.getCloseFuture.awaitUninterruptibly()
    }

    if (factory != null)
      factory.releaseExternalResources()
    /* Note: pipeline factory resources are released globally */
  }

  /** Writes to the channel. */
  def write(obj: Any) = channel.write(obj)

}
