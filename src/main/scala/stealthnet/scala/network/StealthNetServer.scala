package stealthnet.scala.network

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.{Channel, EventLoopGroup}
import io.netty.channel.group.{
  ChannelGroup,
  ChannelGroupFuture,
  DefaultChannelGroup
}
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.util.concurrent.GlobalEventExecutor
import java.net.InetSocketAddress
import java.util.concurrent.Executors
import stealthnet.scala.Settings
import stealthnet.scala.core.Core
import stealthnet.scala.network.connection.StealthNetConnectionParameters
import stealthnet.scala.util.log.{EmptyLoggingContext, Logging}

/**
 * ''StealthNet'' server.
 *
 * Manages server-side actions.
 */
object StealthNetServer extends Logging with EmptyLoggingContext {

  /** Channel group. */
  val group: ChannelGroup = new DefaultChannelGroup("StealthNet server",
    GlobalEventExecutor.INSTANCE)
  /** Boss EventLoopGroup. */
  val bossGroup: EventLoopGroup = new NioEventLoopGroup()
  /** Worker EventLoopGroup. */
  val workerGroup: EventLoopGroup = new NioEventLoopGroup()

  /**
   * Starts server.
   *
   * Server channel is added to the channel group.
   */
  def start() {
    logger debug "Starting"

    val bootstrap: ServerBootstrap = new ServerBootstrap()

    bootstrap.group(bossGroup, workerGroup)
    bootstrap.channel(classOf[NioServerSocketChannel])

    bootstrap.childHandler(StealthNetChannelInitializer(
      new StealthNetConnectionParameters(group = Some(group))))

    val channel: Channel = bootstrap.bind(Settings.core.serverPort).sync().channel()
    group.add(channel)
  }

  /**
   * Stops server.
   *
   * Cleans resources. Connections are supposed to be closed beforehand.
   */
  def stop() {
    logger debug "Stopping"

    bossGroup.shutdownGracefully().awaitUninterruptibly()
    workerGroup.shutdownGracefully().awaitUninterruptibly()

    logger debug "Stopped"
  }

  /**
   * Closes connections.
   *
   * Closes channel group.
   */
  def closeConnections() {
    logger debug s"Closing connections in $group"

    /* Note: we *MUST NOT* await since this would block the caller thread which
     * is also needed when channel closing event is fired.
     */
    group.close()
  }

}
