package perso.stealthnet.network

import java.net.InetSocketAddress
import java.util.concurrent.Executors
import com.weiglewilczek.slf4s.Logging
import org.jboss.netty.bootstrap.ClientBootstrap
import org.jboss.netty.channel.{
  Channels,
  ChannelFactory,
  ChannelFuture,
  ChannelPipeline,
  ChannelPipelineFactory
}
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory

class StealthNetClient(val host: String, val port: Int) extends Logging {

  val factory: ChannelFactory = new NioClientSocketChannelFactory(
    Executors.newCachedThreadPool(),
    Executors.newCachedThreadPool()
  )

  val bootstrap: ClientBootstrap = new ClientBootstrap(factory)

  bootstrap.setPipelineFactory(StealthNetPipelineFactory(null))

  bootstrap.setOption("tcpNoDelay", true)
  bootstrap.setOption("keepAlive", true)

  val future: ChannelFuture = bootstrap.connect(new InetSocketAddress(host, port))
  future.awaitUninterruptibly()
  if (!future.isSuccess) {
    logger error("Failed to connect to host[" + host + "] port[" + port + "]", future.getCause)
    stop()
    false
  }
  else {
    logger debug("Connected to host[" + host + "] port[" + port + "]")
    true
  }

  def write() = {
    future.getChannel.write(1)
  }

  def stop() = {
    logger debug("Closing connection to host[" + host + "] port[" + port + "]")
    future.getChannel.close()
    future.getChannel.getCloseFuture.awaitUninterruptibly()
    factory.releaseExternalResources()
  }

}
