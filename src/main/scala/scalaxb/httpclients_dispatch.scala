package scalaxb

trait DispatchHttpClients extends HttpClients {
  val httpClient = new DispatchHttpClient {}

  trait DispatchHttpClient extends HttpClient {
    import dispatch._

    def request(in: String, address: java.net.URI, headers: Map[String, String]): String = {
      val http = new Http
      http x (url(address.toString) << (in) <:< headers as_str)
    }
  }
}
