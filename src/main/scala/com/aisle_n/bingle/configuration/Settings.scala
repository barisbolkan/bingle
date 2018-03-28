package com.aisle_n.bingle.configuration

import com.typesafe.config.Config

/**
  * Holds all configurations in the *.conf files
  * Can be overridden by providing the configuration in *.conf file
  */
class Settings(config: Config) {

  config.checkValid(config, "mergen")

  // Providers for push services
  val apns: String = config.getString("mergen.providers.apns")
  val gcm:  String = config.getString("mergen.providers.gcm")

  // Persistance
  val snapshotCount: Int = config.getInt("mergen.snapshotcount")
}