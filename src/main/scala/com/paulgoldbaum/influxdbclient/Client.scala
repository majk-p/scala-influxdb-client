package com.paulgoldbaum.influxdbclient

import scala.concurrent.{Future, ExecutionContext}

class Client(val host: String, val port: Int, val username: String = null, val password: String = null) {

  val httpClient = new HttpClient(host, port, username, password)
  implicit val ec = ExecutionContext.global

  def selectDatabase(databaseName: String) =
    new Database(host, port, username, password, databaseName)

  def showDatabases(): Future[Seq[String]] = {
    query("SHOW DATABASES")
      .map(response => response.series.head.points("name").asInstanceOf[List[String]])
  }

  def query(query: String) = {
    val params = Map("q" -> query)
    httpClient.get("query", params)
      .map(response => QueryResponse.fromJson(response.content))
  }

  def queryWithoutResult(query: String) = {
    val params = Map("q" -> query)
    httpClient.get("query", params)
  }

}