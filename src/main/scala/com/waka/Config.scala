package com.waka

import com.typesafe.config.ConfigFactory

/**
  * Created by canoztokmak on 27/05/2017.
  */
object Config {
  private lazy val config = ConfigFactory.load()

  def getHttpInterface: String = config.getString("http.interface")
  def getHttpPort: Int = config.getInt("http.port")
  def getOmdbApiKey: String = config.getString("omdb.apikey")
  def getOmdbHost: String = config.getString("omdb.host")
  def getMongoDBHost: String = config.getString("mongodb.host")
  def getMongoDBPort: String = config.getString("mongodb.port")
}
