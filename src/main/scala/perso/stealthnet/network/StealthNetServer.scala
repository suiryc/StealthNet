package perso.stealthnet.network

import java.net.InetSocketAddress
import java.util.concurrent.Executors
import com.weiglewilczek.slf4s.Logging
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

object StealthNetServer extends Logging {

  val group: ChannelGroup = new DefaultChannelGroup("StealthNet")
  val factory: ChannelFactory = new NioServerSocketChannelFactory(
    Executors.newCachedThreadPool(),
    Executors.newCachedThreadPool()
  )

  def start {
    logger debug "Starting StealthNet server"
    val bootstrap: ServerBootstrap = new ServerBootstrap(factory)

    bootstrap.setPipelineFactory(StealthNetPipelineFactory(group))

    bootstrap.setOption("child.tcpNoDelay", true);
    bootstrap.setOption("child.keepAlive", true);
 
    val channel: Channel = bootstrap.bind(new InetSocketAddress(8080));
    group.add(channel)
  }

  def stop() = {
    logger debug "Stopping StealthNet server"
    val future: ChannelGroupFuture = group.close()
    future.awaitUninterruptibly()
    factory.releaseExternalResources()
  }

}
