package stealthnet.scala.network

import io.netty.channel.{ChannelInitializer, ChannelPipeline}
import io.netty.channel.socket.SocketChannel
import io.netty.handler.timeout.{ReadTimeoutHandler, WriteTimeoutHandler}
import java.util.concurrent.TimeUnit
import stealthnet.scala.Settings
import stealthnet.scala.network.connection.StealthNetConnectionParameters

/**
 * ''StealthNet'' channel initializer companion object.
 */
object StealthNetChannelInitializer {

  /** Factory method. */
  def apply(parameters: StealthNetConnectionParameters) =
    new StealthNetChannelInitializer(parameters)

}

/**
 * ''StealthNet'' channel initializer.
 *
 * Initializes a channel with the following handlers (in order):
 *   - [[stealthnet.scala.network.ParametersHandler]]
 *   - [[stealthnet.scala.network.ConnectionLimitHandler]]
 *   - [[org.jboss.netty.handler.timeout.ReadTimeoutHandler]]
 *     - with configured read timeout
 *   - [[org.jboss.netty.handler.timeout.WriteTimeoutHandler]]
 *     - with configured write timeout
 *   - [[stealthnet.scala.network.CommandDecoder]]
 *   - [[stealthnet.scala.network.CommandEncoder]]
 *   - [[stealthnet.scala.network.CommandHandler]]
 *   - [[stealthnet.scala.network.ExceptionHandler]]
 */
class StealthNetChannelInitializer(val parameters: StealthNetConnectionParameters)
  extends ChannelInitializer[SocketChannel]
{

  def initChannel(ch: SocketChannel) {
    val pipeline = ch.pipeline()

    /* Reminder: inbound is when we are receiving data, outbound when we are
     * sending.
     * Channels listed in the pipeline are processed in given order for an
     * inbound event, and in reverse order for an outbound event.
     */

    /* inbound/outbound parameters handler */
    pipeline.addLast("parameters handler", new ParametersHandler(parameters))

    /* inbound connection limiter */
    pipeline.addLast("connection limiter", new ConnectionLimitHandler())

    /* inbound read timeout handler */
    pipeline.addLast("read timeout handler",
      new ReadTimeoutHandler(Settings.core.readTimeout, TimeUnit.MILLISECONDS))

    /* outbound write timeout handler */
    pipeline.addLast("write timeout handler",
      new WriteTimeoutHandler(Settings.core.writeTimeout, TimeUnit.MILLISECONDS))

    /* inbound command decoder */
    pipeline.addLast("command decoder", new CommandDecoder())

    /* outbound command handler */
    pipeline.addLast("command encoder", new CommandEncoder())

    /* inbound/outbound command handler */
    pipeline.addLast("command handler", new CommandHandler())

    /* inbound/outbound exception handler */
    pipeline.addLast("exception handler", new ExceptionHandler())
  }

}
