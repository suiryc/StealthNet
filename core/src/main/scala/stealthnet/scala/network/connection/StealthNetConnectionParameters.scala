package stealthnet.scala.network.connection

import io.netty.channel.group.ChannelGroup

/**
 * Bare ''StealthNet'' connection parameters.
 */
class StealthNetConnectionParameters(
  /** Channel group to which register an opened channel. */
  var group: Option[ChannelGroup] = None,
  /** Whether we are the client of this connection. */
  var isClient: Boolean = false
)
