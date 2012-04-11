package perso.stealthnet.network

import java.net.{InetAddress, InetSocketAddress}
import java.util.concurrent.Executors
import com.weiglewilczek.slf4s.Logging
import org.jboss.netty.bootstrap.ClientBootstrap
import org.jboss.netty.channel.{
  Channels,
  ChannelFactory,
  ChannelFuture,
  ChannelFutureListener,
  ChannelPipeline,
  ChannelPipelineFactory
}
import org.jboss.netty.channel.group.{ChannelGroup, DefaultChannelGroup}
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory
import org.jboss.netty.channel.group.ChannelGroupFuture

class StealthNetClient(val host: String, val port: Int) extends Logging {

  private var factory: ChannelFactory = null
  private var future: ChannelFuture = null

  def start(): Boolean = {
    if (!StealthNetConnections.add(host))
      return false

    factory = new NioClientSocketChannelFactory(
      Executors.newCachedThreadPool(),
      Executors.newCachedThreadPool()
    )

    val bootstrap: ClientBootstrap = new ClientBootstrap(factory)

    bootstrap.setPipelineFactory(StealthNetPipelineFactory(new StealthNetConnectionParameters(isClient = true)))

    /* XXX - useful ? */
    bootstrap.setOption("tcpNoDelay", true)
    bootstrap.setOption("keepAlive", true)
    /* XXX - configuration */
    bootstrap.setOption("connectTimeoutMillis", 5000)

    future = bootstrap.connect(new InetSocketAddress(host, port))
    future.awaitUninterruptibly()
    if (!future.isSuccess) {
      logger error("Failed to connect to host[" + host + "] port[" + port + "]", future.getCause)
      stop()
      false
    }
    else
      true
  }

  def stop() {
    if (future != null) {
      val channel = future.getChannel

      if (channel.isOpen) {
        logger debug("Closing connection to host[" + host + "] port[" + port + "]")

        val cnx = StealthNetConnections.get(channel, create = false)
        if (cnx != null)
          cnx.closing = true
        channel.close()
        channel.getCloseFuture.awaitUninterruptibly()
      }
    }

    if (factory != null) {
      factory.releaseExternalResources()
      StealthNetPipelineFactory.releaseExternalResources()
    }
  }

  def write() {
    future.getChannel.write(1)
  }

}
