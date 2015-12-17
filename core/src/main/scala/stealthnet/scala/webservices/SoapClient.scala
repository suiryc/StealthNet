package stealthnet.scala.webservices

import java.net.URL

import scala.io.Source
import scala.xml.{Elem, XML}

/**
 * Minimal ''SOAP'' client.
 */
object SoapClient {

  /**
   * Wraps ''XML'' node inside ''SOAP'' envelope/body.
   *
   * @param elem ''XML'' node to wrap
   * @return ''XML'' string
   */
  def wrap(elem: Elem): String = {
    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
    <soap12:Envelope
      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      xmlns:xsd="http://www.w3.org/2001/XMLSchema"
      xmlns:soap12="http://www.w3.org/2003/05/soap-envelope"
    >
      <soap12:Body>
        { elem }
      </soap12:Body>
    </soap12:Envelope>
  }

  /**
   * Invokes ''SOAP'' request.
   *
   * @param url ''URL'' to call
   * @param req ''SOAP'' request
   * @return either error message or response document
   */
  def doRequest(url: String, req: Elem): Either[String, Elem] = {
    val outs = wrap(req).getBytes("UTF-8")
    val conn = new URL(url).openConnection.asInstanceOf[java.net.HttpURLConnection]
    try {
      conn.setRequestMethod("POST")
      conn.setDoOutput(true)
      conn.setRequestProperty("Content-Length", outs.length.toString)
      conn.setRequestProperty("Content-Type", "text/xml; charset=utf-8")
      conn.getOutputStream.write(outs)
      conn.getOutputStream.close()
      Right(XML.load(conn.getInputStream))
    }
    catch {
      case e: Throwable =>
        try {
          val response = Source.fromInputStream(conn.getErrorStream).mkString
          val details = try {
            val doc = XML.loadString(response)
            "Fault code[" + (doc \\ "faultcode").text +
                "] actor[" + (doc \\ "faultactor").text +
                "]: " + (doc \\ "faultstring").text
          }
          catch {
            case e: Throwable => response
          }

          Left(s"Response code[${conn.getResponseCode}] message[${conn.getResponseMessage}] details[$details]")
        }
        catch {
          case _: Throwable =>
          /* Usually means we could not even connect to the server, let alone
           * get a response (code / message).
           */
          Left(e.toString)
        }
    }
  }

}
