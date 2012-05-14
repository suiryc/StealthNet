package com.primefaces.sample.cometd

import org.cometd.server.DefaultSecurityPolicy
import org.cometd.bayeux.server.{BayeuxServer, ServerMessage, ServerSession}

class SecurityPolicy extends DefaultSecurityPolicy {

  override def canHandshake(server: BayeuxServer, session: ServerSession,
      message: ServerMessage): Boolean =
  {
    val authenticated = true

    if (!authenticated) {
      val reply = message.getAssociated
      // Here you can customize the reply
    }

    authenticated
  }

}
