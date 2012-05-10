package com.primefaces.sample

object TestServer {

  /* XXX - http://wiki.eclipse.org/Jetty/Howto/Configure_JSP
   */
  def main(args: Array[String]): Unit = {
    println("Started")

    try {
      Server.start()

      Thread.sleep(60000)
    }
    finally {
      Server.stop()
    }
  }

}
