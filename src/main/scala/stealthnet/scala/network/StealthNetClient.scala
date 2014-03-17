package stealthnet.scala.network

import akka.pattern.ask
import akka.util.Timeout
import io.netty.bootstrap.Bootstrap
import io.netty.channel.{ChannelFuture, ChannelOption, EventLoopGroup}
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.util.concurrent.GenericFutureListener
import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import stealthnet.scala.Settings
import stealthnet.scala.network.connection.{
  StealthNetConnectionParameters,
  StealthNetConnectionsManager
}
import stealthnet.scala.util.Peer
import stealthnet.scala.util.log.{Logging, EmptyLoggingContext}

/**
 * ''StealthNet'' client companion object.
 *
 * Manages client-side shared resources.
 */
object StealthNetClient
  extends Logging
  with EmptyLoggingContext
{

  /** Worker EventLoopGroup. */
  private val workerGroup: EventLoopGroup = new NioEventLoopGroup()

  private val bootstrap: Bootstrap = new Bootstrap()

  bootstrap.group(workerGroup)
  bootstrap.channel(classOf[NioSocketChannel])
  bootstrap.option[Integer](ChannelOption.CONNECT_TIMEOUT_MILLIS, Settings.core.connectTimeout.toInt)

  /* Add the channel to the (server) group: will be closed with server. */
  bootstrap.handler(StealthNetChannelInitializer(
    new StealthNetConnectionParameters(isClient = true)))

  /**
   * Cleans shared resources.
   * @todo Caller may need to wait for returned future completion ?
   */
  def stop() = {
    logger debug "Stopping"

    val f = workerGroup.shutdownGracefully(Settings.core.shutdownQuietPeriod,
      Settings.core.shutdownTimeout, TimeUnit.MILLISECONDS)

    import stealthnet.scala.util.netty.NettyFuture._
    import scala.concurrent.ExecutionContext.Implicits.global
    val r = for {
      _ <- f
    } yield {
      logger debug "Stopped"
    }

    r
  }

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

  implicit private val timeout = Timeout(1.hour)

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
    import scala.concurrent.ExecutionContext.Implicits.global

    (StealthNetConnectionsManager.actor ? StealthNetConnectionsManager.AddPeer(peer)).mapTo[Boolean] onComplete {
      case Failure(e) =>
        logger error("Failed to start", e)

      case Success(allowed) =>
        if (allowed)
          connect()
    }
  }

  /**
   * Attempts asynchronous connection.
   */
  protected def connect() {
    logger trace("Starting new client connection")

    val future = bootstrap.connect(new InetSocketAddress(peer.host, peer.port))
    StealthNetServer.group.add(future.channel)
    future.addListener(new GenericFutureListener[ChannelFuture]() {
      override def operationComplete(future: ChannelFuture) {
        if (!future.isSuccess) {
          logger debug("Failed to connect", future.cause)
          /* Since connection failed, we need to notify the manager */
          StealthNetConnectionsManager.actor ! StealthNetConnectionsManager.RemovePeer(peer)
        }
      }
    })
  }

}
