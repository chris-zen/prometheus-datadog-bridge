package com.github.chris_zen.prometheus.bridge.datadog

import java.time.Duration

import io.prometheus.client.CollectorRegistry
import DatadogBridgeConfig._

object DatadogBridgeConfig {
  private val DefaultDatadogHost = "localhost"
  private val DefaultDatadogPort = 8125
  private val DefaultDatadogPrefix = ""
  private val DefaultPeriod = Duration.ofMinutes(1)
}

case class DatadogBridgeConfig(host: String = DefaultDatadogHost,
                               port: Int = DefaultDatadogPort,
                               prefix: String = DefaultDatadogPrefix,
                               tags: Seq[String] = Seq.empty,
                               period: Duration = DefaultPeriod,
                               registry: Option[CollectorRegistry] = None) {

  def withAddress(host: String, port: Int): DatadogBridgeConfig = copy(host = host, port = port)
  def withPrefix(prefix: String): DatadogBridgeConfig = copy(prefix = prefix)
  def withTags(tags: String*): DatadogBridgeConfig = copy(tags = tags)
  def withPeriod(period: Duration): DatadogBridgeConfig = copy(period = period)
  def withRegistry(registry: CollectorRegistry): DatadogBridgeConfig = copy(registry = Some(registry))
}
