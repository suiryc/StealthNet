package perso.stealthnet.network

import java.util.concurrent.TimeUnit
import org.jboss.netty.channel.{
  Channels,
  ChannelPipeline,
  ChannelPipelineFactory
}
import org.jboss.netty.handler.timeout.ReadTimeoutHandler
import org.jboss.netty.util.{
  ExternalResourceReleasable,
  HashedWheelTimer,
  Timer
}

object StealthNetPipelineFactory extends ExternalResourceReleasable {

  val timer: Timer = new HashedWheelTimer()

  def apply(parameters: StealthNetConnectionParameters) =
    new StealthNetPipelineFactory(parameters)

  def releaseExternalResources() {
    timer.stop()
  }

}

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

    /* upstream timeout handler */
    /* XXX - possible to change timeout dynamically */
    pipeline.addLast("timeout handler", new ReadTimeoutHandler(StealthNetPipelineFactory.timer, 30000, TimeUnit.MILLISECONDS))

    /* upstream command decoder */
    pipeline.addLast("command decoder", new CommandDecoder())

    /* upstream/downstream command handler */
    pipeline.addLast("command handler", new CommandHandler(parameters.group))

    pipeline
  }

}
