package com.primefaces.sample.cometd

import org.cometd.bayeux.Message
import org.cometd.bayeux.server.{BayeuxServer, ServerSession}
import org.cometd.server.AbstractService
import scala.collection.JavaConversions._

class HelloService(bayeux: BayeuxServer)
  extends AbstractService(bayeux, "hello")
{

  addService("/service/hello", "processHello")

  def processHello(remote: ServerSession, message: Message) {
    val input = message.getDataAsMap()
    val name = input.get("name").asInstanceOf[String]

    val output = Map[String, Object]("greeting" -> ("Hello, " + name))
    remote.deliver(getServerSession(), "/hello", output:java.util.Map[String, Object], null)
  }

}
