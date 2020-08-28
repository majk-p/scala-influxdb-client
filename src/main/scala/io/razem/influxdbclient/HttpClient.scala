package io.razem.influxdbclient

import java.nio.charset.Charset

import cats.MonadError
import cats.implicits._
import org.asynchttpclient.Realm.{AuthScheme, Builder}
import org.asynchttpclient._

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future, Promise}

import sttp.client._
import sttp.model.Uri
import cats.data.EitherT

trait HttpClient[F[_]] {
  def get(url: String, params: Map[String, String] = Map()): F[HttpResponse]
  def post(url: String, params: Map[String, String] = Map(), content: String): F[HttpResponse]
  def close(): F[Unit]
}

object HttpClient {

  case class HttpClientError(msg: String) extends Exception(msg)

  def apply[F[_]](implicit ev: HttpClient[F]): HttpClient[F] = ev

  def sttpInstance[F[_]: MonadError[*[_], Throwable]](
      host: String,
      port: Int,
      https: Boolean = false,
      username: Option[String] = None,
      password: Option[String] = None
  )(
      implicit httpBackend: SttpBackend[F, Nothing, Nothing]
  ) = new HttpClient[F] {

    private val scheme = if (https) "https" else "http"

    private def responseToEither(
        response: sttp.client.Response[Either[String, String]]
    ): Either[HttpClientError, HttpResponse] =
      response.body.bimap(HttpClientError, HttpResponse(response.code.code, _))


    private def makeUri(url: String, params: Map[String, String]): Uri =
      uri"$scheme://$host:$port$url".params(params)

    override def get(url: String, params: Map[String, String]): F[HttpResponse] = 
      basicRequest
        .get(makeUri(url, params))
        .send()
        .map(responseToEither)
        .flatMap(MonadError[F, Throwable].fromEither)


    override def post(url: String, params: Map[String, String], content: String): F[HttpResponse] =
      basicRequest
        .post(makeUri(url, params))
        .body(content)
        .send()
        .map(responseToEither)
        .flatMap(MonadError[F, Throwable].fromEither)


    override def close(): F[Unit] = MonadError[F, Throwable].unit

  }
}

// protected class HttpClient(val host: String,
//                           val port: Int,
//                           val https: Boolean = false,
//                           val username: String = null,
//                           val password: String = null,
//                           val clientConfig: HttpConfig = null)(implicit ec: ExecutionContext)
// {

//   private val authenticationRealm = makeAuthenticationRealm()
//   private var connectionClosed = false

//   private val client: AsyncHttpClient = if (clientConfig == null)
//     new DefaultAsyncHttpClient()
//   else
//     new DefaultAsyncHttpClient(clientConfig.build())

//   private val protocol = if (https) "https" else "http"

//   def get(url: String, params: Map[String, String] = Map()): Future[HttpResponse] = {
//     val requestBuilder = client.prepareGet("%s://%s:%d%s".format(protocol, host, port, url))
//       .setRealm(authenticationRealm)
//     requestBuilder.setQueryParams(params.map(p => new Param(p._1, p._2)).toList.asJava)

//     makeRequest(requestBuilder)
//   }

//   def post(url: String, params: Map[String, String] = Map(), content: String): Future[HttpResponse] = {
//     val requestBuilder = client.preparePost("%s://%s:%d%s".format(protocol, host, port, url))
//       .setRealm(authenticationRealm)
//       .setBody(content)
//       .setCharset(Charset.forName("UTF-8"))
//     requestBuilder.setQueryParams(params.map(p => new Param(p._1, p._2)).toList.asJava)

//     makeRequest(requestBuilder)
//   }

//   private def makeRequest(requestBuilder: BoundRequestBuilder): Future[HttpResponse] = {
//     val resultPromise = Promise[HttpResponse]()
//     if (isClosed)
//       return resultPromise.failure(new HttpException("Connection is already closed")).future

//     requestBuilder.execute(new ResponseHandler(resultPromise))
//     resultPromise.future
//   }

//   def close(): Future[Unit] = Future {
//     if (isClosed)
//       throw new HttpException("Connection is already closed")

//     client.close()
//     connectionClosed = true
//   }

//   def isClosed: Boolean = connectionClosed

//   private def makeAuthenticationRealm(): Realm = username match {
//     case null => null
//     case _ => new Builder(username, password)
//       .setUsePreemptiveAuth(true)
//       .setScheme(AuthScheme.BASIC)
//       .build()
//   }

//   private class ResponseHandler(promise: Promise[HttpResponse]) extends AsyncCompletionHandler[Response] {

//     override def onCompleted(response: Response): Response = {
//       if (response.getStatusCode >= 400)
//         promise.failure(new HttpException(s"Server answered with error code ${response.getStatusCode}. Message: ${response.getResponseBody}", response.getStatusCode))
//       else
//         promise.success(new HttpResponse(response.getStatusCode, response.getResponseBody))
//       response
//     }

//     override def onThrowable(throwable: Throwable): Unit = {
//       promise.failure(new HttpException("An error occurred during the request", -1, throwable))
//     }
//   }

// }

class HttpException protected[influxdbclient] (
    val str: String,
    val code: Int = -1,
    val throwable: Throwable = null
) extends Exception(str, throwable) {}

case class HttpResponse(code: Int, content: String)

case class HttpJsonResponse(code: Int, content: Map[String, Object])
