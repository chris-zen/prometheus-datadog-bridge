package com.github.chris_zen.prometheus.bridge.datadog

import com.timgroup.statsd.StatsDClient
import io.prometheus.client.{CollectorRegistry, Counter, Gauge, Histogram}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}
import org.mockito.Mockito.{verify, verifyNoMoreInteractions}

class DatadogPushSpec extends FlatSpec with Matchers with DatadogPushFixtures {

  "A DatadogBridge" should "close the statsd client" in {
    withDatadogPush { (registry, pusher, client) =>
      pusher.close()
      verify(client).close()
      verifyNoMoreInteractions(client)
    }
  }

  it should "push metrics from a gauge" in {
    withDatadogPush { (registry, pusher, client) =>
      Gauge.build("metric1", "help1").register(registry).set(1.0)
      Gauge.build("metric2", "help2").labelNames("l1").register(registry).labels("v1").set(2.0)
      pusher.push(registry)
      verify(client).gauge("metric1", 1.0)
      verify(client).gauge("metric2", 2.0, "l1:v1")
      verifyNoMoreInteractions(client)
    }
  }

  it should "push metrics from a counter" in {
    withDatadogPush { (registry, pusher, client) =>
      Counter.build("metric1", "help2").register(registry).inc(1.0)
      Counter.build("metric2", "help2").labelNames("l1").register(registry).labels("v1").inc(2.0)
      pusher.push(registry)
      verify(client).count("metric1", 1.0)
      verify(client).count("metric2", 2.0, "l1:v1")
      verifyNoMoreInteractions(client)
    }
  }

  it should "ignore unsupported metric types" in {
    val registry = new CollectorRegistry()
    val client = mock[StatsDClient]
    val bridge = new DatadogPush(client)

    val histogram = Histogram.build("metric1", "help1").register(registry)
    histogram.observe(1.0)

    val counter = Counter.build("metric2", "help2").labelNames("label1", "label2").register(registry)
    counter.labels("v1.2", "v2.2").inc(2.0)

    bridge.push(registry)

    verify(client).count("metric2", 2.0, "label1:v1.2", "label2:v2.2")
    verifyNoMoreInteractions(client)
  }
}

trait DatadogPushFixtures extends MockitoSugar {
  def withDatadogPush(test: (CollectorRegistry, DatadogPush, StatsDClient) => Any): Any = {
    val registry = new CollectorRegistry()
    val client = mock[StatsDClient]
    val pusher = new DatadogPush(client)

    test(registry, pusher, client)
  }
}