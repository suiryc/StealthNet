package perso.stealthnet.network

import org.jboss.netty.channel.{
  Channels,
  ChannelPipeline,
  ChannelPipelineFactory
}
import org.jboss.netty.channel.group.ChannelGroup

object StealthNetPipelineFactory {

  def apply(group: ChannelGroup): StealthNetPipelineFactory = new StealthNetPipelineFactory(group)

}

class StealthNetPipelineFactory(val group: ChannelGroup) extends ChannelPipelineFactory {

  def getPipeline(): ChannelPipeline = {
    val pipeline = Channels.pipeline()

    /* Reminder: upstream is when we are receiving data, downstream when we are
     * sending.
     * Channels listed in the pipeline are processed in given order for an
     * upstream event, and in reverse order for a downstream event.
     */

    /* upstream/downstream encryption handler */
    pipeline.addLast("decrypter", new EncryptionDecoder())
    pipeline.addLast("encrypter", new EncryptionEncoder())

    /* upstream command builder */
    pipeline.addLast("command builder", new CommandDecoder())

    /* upstream/downstream command handler */
    pipeline.addLast("command handler", new CommandHandler(group))

    pipeline
  }

}
