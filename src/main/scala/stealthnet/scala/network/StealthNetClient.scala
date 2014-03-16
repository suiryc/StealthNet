package stealthnet.scala.network

import io.netty.bootstrap.{Bootstrap, ChannelFactory}
import io.netty.channel.{Channel, ChannelFuture, ChannelOption, EventLoopGroup}
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.util.concurrent.GenericFutureListener
import java.net.InetSocketAddress
import java.util.concurrent.Executors
import stealthnet.scala.Settings
import stealthnet.scala.network.connection.{
  StealthNetConnectionParameters,
  StealthNetConnectionsManager
}
import stealthnet.scala.util.Peer
import stealthnet.scala.util.log.Logging

/**
 * ''StealthNet'' client companion object.
 *
 * Manages client-side shared resources.
 */
object StealthNetClient {

  /** Worker EventLoopGroup. */
  private val workerGroup: EventLoopGroup = new NioEventLoopGroup()

  /**
   * Cleans shared resources.
   * @todo Caller may need to wait for returned future completion ?
   */
  def stop() =
    workerGroup.shutdownGracefully().awaitUninterruptibly()

}

/**
 * ''StealthNet'' client.
 *
 * Manages client-side actions.
 *
 * Each created client first registers the remote peer as a to-be
 * [[stealthnet.scala.network.connection.StealthNetConnection]].
 */
class StealthNetClient(
  /** Remote peer to connect to. */
  val peer: Peer
) extends Logging
{

  import StealthNetClient._

  protected def loggerContext = List("peer" -> peer)

  /**
   * Starts client.
   *
   * If connection to the peer is not accepted (e.g. limit reached) nothing is
   * done, otherwise connection to remote peer is asynchronuously attempted.
   *
   * @see [[stealthnet.scala.network.connection.StealthNetConnectionsManager]].`addPeer`
   */
  def start() {
    if (StealthNetConnectionsManager.addPeer(peer))
      connect()
  }

  /**
   * Attempts asynchronous connection.
   */
  protected def connect() {
    logger trace("Starting new client connection")

    val bootstrap: Bootstrap = new Bootstrap()

    bootstrap.group(workerGroup)
    bootstrap.channel(classOf[NioSocketChannel])
    bootstrap.option[Integer](ChannelOption.CONNECT_TIMEOUT_MILLIS, Settings.core.connectTimeout.toInt)

    /* Add the channel to the (server) group: will be closed with server. */
    bootstrap.handler(StealthNetChannelInitializer(
      new StealthNetConnectionParameters(
        group = Some(StealthNetServer.group),
        client = Some(this),
        peer = Some(peer)
      )
    ))

    val future = bootstrap.connect(new InetSocketAddress(peer.host, peer.port))
    future.addListener(new GenericFutureListener[ChannelFuture]() {
      override def operationComplete(future: ChannelFuture) {
        if (!future.isSuccess) {
          logger debug("Failed to connect", future.cause)
          /* Since connection failed, we need to notify the manager */
          StealthNetConnectionsManager.removePeer(peer)
        }
      }
    })
  }

}
