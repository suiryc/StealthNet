package perso.stealthnet.network

import java.net.InetSocketAddress
import java.util.concurrent.Executors
import org.jboss.netty.bootstrap.ServerBootstrap
import org.jboss.netty.channel.{
  Channel,
  ChannelFactory,
  ChannelPipeline,
  ChannelPipelineFactory,
  Channels
}
import org.jboss.netty.channel.group.{
  ChannelGroup,
  ChannelGroupFuture,
  DefaultChannelGroup
}
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory
import perso.stealthnet.core.util.{EmptyLoggingContext, Logging}

object StealthNetServer extends Logging with EmptyLoggingContext {

  val group: ChannelGroup = new DefaultChannelGroup("StealthNet server")
  val factory: ChannelFactory = new NioServerSocketChannelFactory(
    Executors.newCachedThreadPool(),
    Executors.newCachedThreadPool()
  )

  def start() {
    logger debug "Starting"
    val bootstrap: ServerBootstrap = new ServerBootstrap(factory)

    bootstrap.setPipelineFactory(StealthNetPipelineFactory(new StealthNetConnectionParameters(group = group)))

    /* XXX - useful ? */
    bootstrap.setOption("child.tcpNoDelay", true);
    bootstrap.setOption("child.keepAlive", true);
 
    var channel: Channel = bootstrap.bind(new InetSocketAddress(6097))
    group.add(channel)
    /* XXX - can be used to pause accept ? */
    //channel.close().awaitUninterruptibly()
    //channel = bootstrap.bind(new InetSocketAddress(8080))
  }

  def stop() {
    logger debug "Stopping"
    val future: ChannelGroupFuture = group.close()
    future.awaitUninterruptibly()
    factory.releaseExternalResources()
    StealthNetPipelineFactory.releaseExternalResources()
  }

}
