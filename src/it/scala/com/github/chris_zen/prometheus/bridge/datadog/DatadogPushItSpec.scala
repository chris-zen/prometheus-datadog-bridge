package com.github.chris_zen.prometheus.bridge.datadog

import io.prometheus.client.{CollectorRegistry, Counter}
import org.scalatest.{FlatSpec, Matchers}

class DatadogPushItSpec extends FlatSpec with Matchers with StatsDFixtures {

  "A DatadogPush" should "push metrics to Datadog" in {
    withStatsDServer { statsD =>
      val registry = new CollectorRegistry()
      Counter.build("metric2", "help2").labelNames("l1", "l2").register(registry).labels("v1", "v2").inc(2.0)
      Counter.build("metric1", "help2").register(registry).inc(3.0)

      val config = DatadogBridge.Config(host = "localhost", port = statsD.port, registry = Some(registry))
      val pusher = DatadogPush(config)

      pusher.push()
      pusher.close()

      statsD.receiveAll.toSet shouldBe Set("metric2:2|c|#l2:v2,l1:v1", "metric1:3|c")
    }
  }
}
