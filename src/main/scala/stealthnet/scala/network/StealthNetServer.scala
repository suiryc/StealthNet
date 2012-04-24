package stealthnet.scala.network

import java.net.InetSocketAddress
import java.util.concurrent.Executors
import org.jboss.netty.bootstrap.ServerBootstrap
import org.jboss.netty.channel.{Channel, ChannelFactory}
import org.jboss.netty.channel.group.{
  ChannelGroup,
  ChannelGroupFuture,
  DefaultChannelGroup
}
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory
import stealthnet.scala.Config
import stealthnet.scala.core.Core
import stealthnet.scala.util.{EmptyLoggingContext, Logging}

/**
 * ''StealthNet'' server.
 *
 * Manages server-side actions.
 */
object StealthNetServer extends Logging with EmptyLoggingContext {

  /** Channel group. */
  val group: ChannelGroup = new DefaultChannelGroup("StealthNet server")
  /** Channel factory. */
  val factory: ChannelFactory = new NioServerSocketChannelFactory(
    Executors.newCachedThreadPool(),
    Executors.newCachedThreadPool()
  )

  /**
   * Starts server.
   *
   * Server channel is added to the channel group.
   */
  def start() {
    logger debug "Starting"
    val bootstrap: ServerBootstrap = new ServerBootstrap(factory)

    bootstrap.setPipelineFactory(StealthNetPipelineFactory(new StealthNetConnectionParameters(group = group)))

    var channel: Channel = bootstrap.bind(new InetSocketAddress(Config.serverPort))
    group.add(channel)
  }

  /**
   * Stops server.
   *
   * Closes channel group and cleans resources.
   */
  def stop() {
    logger debug "Stopping"
    Core.stopping = true
    val future: ChannelGroupFuture = group.close()
    future.awaitUninterruptibly()
    factory.releaseExternalResources()
    StealthNetPipelineFactory.releaseExternalResources()
  }

}
