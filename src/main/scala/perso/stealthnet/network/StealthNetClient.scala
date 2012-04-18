package perso.stealthnet.network

import java.net.InetSocketAddress
import java.util.concurrent.Executors
import org.jboss.netty.bootstrap.ClientBootstrap
import org.jboss.netty.channel.{ChannelFactory, ChannelFuture}
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory
import perso.stealthnet.util.{Logging, Peer}

class StealthNetClient(val peer: Peer) extends Logging {

  protected def loggerContext = List("peer" -> peer)

  private var factory: ChannelFactory = null
  private var future: ChannelFuture = null

  def start(): Boolean = {
    if (!StealthNetConnectionsManager.addPeer(peer))
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

    future = bootstrap.connect(new InetSocketAddress(peer.host, peer.port))
    future.awaitUninterruptibly()
    if (!future.isSuccess) {
      logger error("Failed to connect", future.getCause)
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
        logger debug("Closing connection")

        val cnx = StealthNetConnectionsManager.getConnection(channel, false)
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

  def write(obj: Any) {
    future.getChannel.write(obj)
  }

}
