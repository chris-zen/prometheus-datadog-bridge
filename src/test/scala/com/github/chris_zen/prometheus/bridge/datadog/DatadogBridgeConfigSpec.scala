package com.github.chris_zen.prometheus.bridge.datadog

import java.time.Duration

import io.prometheus.client.CollectorRegistry
import org.scalatest.{FlatSpec, Matchers}

class DatadogBridgeConfigSpec extends FlatSpec with Matchers {

  "A DatadogBridgeConfig" should "build with an address" in {
    val port = 1234
    DatadogBridgeConfig().withAddress("host", port) shouldBe DatadogBridgeConfig(host = "host", port = port)
  }

  it should "build with a prefix" in {
    DatadogBridgeConfig().withPrefix("prefix") shouldBe DatadogBridgeConfig(prefix = "prefix")
  }

  it should "build with tags" in {
    val tags = Seq("t1:v1", "t2:v2")
    DatadogBridgeConfig().withTags(tags: _*) shouldBe DatadogBridgeConfig(tags = tags)
  }

  it should "build with a period" in {
    val period = Duration.ofMinutes(1)
    DatadogBridgeConfig().withPeriod(period) shouldBe DatadogBridgeConfig(period = period)
  }

  it should "build with a registry" in {
    val registry = new CollectorRegistry()
    DatadogBridgeConfig().withRegistry(registry) shouldBe DatadogBridgeConfig(registry = Some(registry))
  }
}
