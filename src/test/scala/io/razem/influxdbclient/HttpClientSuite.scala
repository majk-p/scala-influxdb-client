package io.razem.influxdbclient

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._

import org.scalatest.{BeforeAndAfterAll, BeforeAndAfter}


import cats.implicits._
import sttp.client._
import sttp.client.okhttp.OkHttpFutureBackend
import scala.concurrent.Future


class HttpClientSuite extends CustomTestSuite with BeforeAndAfter {
  
  implicit val sttpBackend = OkHttpFutureBackend()
  var host = "localhost"
  var port = 64011
  var httpsPort = 64012
  var mockServer: WireMockServer = new WireMockServer(
    wireMockConfig().port(port).containerThreads(10).jettyAcceptors(1).httpsPort(httpsPort)
  )
  mockServer.start()
  WireMock.configureFor(host, port)

  before {
    WireMock.reset()
  }

  override def afterAll = {
    mockServer.shutdown()
    super.afterAll
  }

  test("Basic requests are received") {
    val url = "/query"
    stubFor(get(urlEqualTo(url)).willReturn(aResponse().withStatus(200).withBody("")))
    val client = HttpClient.sttpInstance[Future](host, port)
    val future = client.get(url)
    val result = await(future)
    assert(result.code == 200)
    client.close()
  }

  test("Https requests are received") {
    val url = "/query"
    stubFor(get(urlEqualTo(url)).willReturn(aResponse().withStatus(200).withBody("")))
    val config = new HttpConfig().setAcceptAnyCertificate(true)
    val client = HttpClient.sttpInstance[Future](host, httpsPort, true, null, null)//, config)
    val future = client.get(url)
    val result = await(future)
    assert(result.code == 200)
    client.close()
  }

  test("Error responses are handled correctly") {
    val url = "/query"
    stubFor(get(urlEqualTo(url))
      .willReturn(
        aResponse()
          .withStatus(500)
          .withBody("")))

    val client = HttpClient.sttpInstance[Future](host, port)
    try {
      await(client.get(url))
      fail("Did not throw exception")
    } catch {
      case e: HttpException =>
        assert(e.code == 500)
    } finally {
      client.close()
    }
  }

  test("Future fails on connection refused") {
    val config = new HttpConfig()
    val client = HttpClient.sttpInstance[Future](host, port - 1, false, null, null)//, config)

    try {
      await(client.get("/query"))
      fail("Did not throw exception")
    } catch {
      case e: HttpException =>
    } finally {
      client.close()
    }
  }

  test("Future fails if request takes too long") {
    val url = "/query"
    stubFor(get(urlEqualTo(url))
      .willReturn(
        aResponse()
          .withFixedDelay(200)
          .withStatus(200)
          .withBody("a")))

    val config = new HttpConfig().setRequestTimeout(50)
    val client = HttpClient.sttpInstance[Future](host, port, false, null, null)//, config)

    try {
      await(client.get(url))
      fail("Did not throw exception")
    } catch {
      case e: HttpException =>
    } finally {
      client.close()
    }
  }

  test("Future fails if the connection takes too long to establish") {
    val url = "/query"

    val config = new HttpConfig().setConnectTimeout(50)
    val client = HttpClient.sttpInstance[Future]("192.0.2.1", port, false, null, null)//, config)

    try {
      await(client.get(url))
      fail("Did not throw exception")
    } catch {
      case e: HttpException =>
    } finally {
      client.close()
    }
  }

  test("Closing a connection more than once throws an exception") {
    val client = HttpClient.sttpInstance[Future](host, port)
    client.close()
    try {
      client.close()
      fail("Did not throw exception")
    } catch {
      case e: HttpException =>
    }
  }

  test("Using a closed connection to send a query returns an exception") {
    val client = HttpClient.sttpInstance[Future](host, port)
    client.close()
    try {
      await(client.get("/query"))
      fail("Did not throw exception")
    } catch {
      case e: HttpException =>
    }
  }
}
