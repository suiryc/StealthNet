package stealthnet.scala.network

import java.util.concurrent.TimeUnit
import org.jboss.netty.channel.{
  Channels,
  ChannelPipeline,
  ChannelPipelineFactory
}
import org.jboss.netty.handler.timeout.{
  ReadTimeoutHandler,
  WriteTimeoutHandler
}
import org.jboss.netty.util.{
  ExternalResourceReleasable,
  HashedWheelTimer,
  Timer
}
import stealthnet.scala.Settings
import stealthnet.scala.network.connection.StealthNetConnectionParameters

/**
 * ''StealthNet'' pipeline factory companion object.
 *
 * Holds a shared timer, used by timeout handlers.
 */
object StealthNetPipelineFactory extends ExternalResourceReleasable {

  /** Shared timer. */
  val timer: Timer = new HashedWheelTimer()

  /** Factory method. */
  def apply(parameters: StealthNetConnectionParameters) =
    new StealthNetPipelineFactory(parameters)

  /**
   * Releases external resources.
   *
   * Stops shared timer.
   *
   * This method has to be called before closing the application.
   */
  def releaseExternalResources() {
    timer.stop()
  }

}

/**
 * ''StealthNet'' pipeline factory.
 *
 * Creates a pipeline with the following handlers (in order):
 *   - [[stealthnet.scala.network.ParametersHandler]]
 *   - [[stealthnet.scala.network.ConnectionLimitHandler]]
 *   - [[org.jboss.netty.handler.timeout.ReadTimeoutHandler]]
 *     - with configured read timeout
 *   - [[org.jboss.netty.handler.timeout.WriteTimeoutHandler]]
 *     - with configured write timeout
 *   - [[stealthnet.scala.network.CommandDecoder]]
 *   - [[stealthnet.scala.network.CommandHandler]]
 */
class StealthNetPipelineFactory(val parameters: StealthNetConnectionParameters)
  extends ChannelPipelineFactory
{

  def getPipeline(): ChannelPipeline = {
    val pipeline = Channels.pipeline()

    /* Reminder: upstream is when we are receiving data, downstream when we are
     * sending.
     * Channels listed in the pipeline are processed in given order for an
     * upstream event, and in reverse order for a downstream event.
     */

    /* upstream/downstream parameters handler */
    pipeline.addLast("parameters handler", new ParametersHandler(parameters))

    /* upstream connection limiter */
    pipeline.addLast("connection limiter", new ConnectionLimitHandler())

    /* upstream read timeout handler */
    pipeline.addLast("read timeout handler",
      new ReadTimeoutHandler(StealthNetPipelineFactory.timer,
        Settings.core.readTimeout, TimeUnit.MILLISECONDS))

    /* downstream write timeout handler */
    pipeline.addLast("write timeout handler",
      new WriteTimeoutHandler(StealthNetPipelineFactory.timer,
        Settings.core.writeTimeout, TimeUnit.MILLISECONDS))

    /* upstream command decoder */
    pipeline.addLast("command decoder", new CommandDecoder())

    /* upstream/downstream command handler */
    pipeline.addLast("command handler", new CommandHandler())

    pipeline
  }

}
