package com.github.chris_zen.prometheus.bridge.datadog

import java.time.Duration

import io.prometheus.client.{CollectorRegistry, Counter}
import org.scalatest.{FlatSpec, Matchers}


class DatadogBridgeItSpec extends FlatSpec with Matchers with StatsDFixtures {

  import DatadogBridgeItSpec._

  "A DatadogBridge" should "push metrics to Datadog" in {
    withStatsDServer { statsD =>

      val registry = new CollectorRegistry()
      val counter = Counter.build("metric2", "help2").labelNames("l1", "l2").register(registry)

      counter.labels("v1", "v2").inc(1.0)

      val config = DatadogBridge.Config(host = "localhost", port = statsD.port,
                                        prefix = "prefix", tags = List("l0:v0"),
                                        period = PushPeriod, registry = Some(registry))
      val bridge = DatadogBridge(config)

      Thread.sleep(PeriodMarginMillis)

      statsD.receive shouldBe "prefix.metric2:1|c|#l0:v0,l2:v2,l1:v1"

      counter.labels("v1", "v2").inc(1.0)

      Thread.sleep(PeriodMillis)

      statsD.receive shouldBe "prefix.metric2:2|c|#l0:v0,l2:v2,l1:v1"

      counter.labels("v1", "v2").inc(1.0)

      Thread.sleep(PeriodMillis)

      statsD.receive shouldBe "prefix.metric2:3|c|#l0:v0,l2:v2,l1:v1"

      bridge.stop()
    }
  }

  it should "use the default collector" in {
    val config = DatadogBridge.Config()
    val bridge = DatadogBridge(config)
    bridge.registry shouldBe CollectorRegistry.defaultRegistry
  }
}

object DatadogBridgeItSpec {
  private val PeriodMillis = 500
  private val PeriodMarginMillis = 100
  private val PushPeriod = Duration.ofMillis(PeriodMillis)
}
