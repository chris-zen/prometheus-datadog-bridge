package com.github.chris_zen.prometheus.bridge.datadog

import java.util

import com.timgroup.statsd.StatsDClient
import io.prometheus.client.Collector.MetricFamilySamples.Sample
import io.prometheus.client.Collector.{MetricFamilySamples, Type}
import io.prometheus.client._
import org.mockito.Mockito.{verify, verifyNoMoreInteractions}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}

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
      pusher.push()
      verify(client).gauge("metric1", 1.0)
      verify(client).gauge("metric2", 2.0, "l1:v1")
      verifyNoMoreInteractions(client)
    }
  }

  it should "push metrics from a counter" in {
    withDatadogPush { (registry, pusher, client) =>
      Counter.build("metric1", "help2").register(registry).inc(1.0)
      Counter.build("metric2", "help2").labelNames("l1").register(registry).labels("v1").inc(2.0)
      pusher.push()
      verify(client).count("metric1", 1.0)
      verify(client).count("metric2", 2.0, "l1:v1")
      verifyNoMoreInteractions(client)
    }
  }

  it should "push metrics from a counter as deltas" in {
    withDatadogPush { (registry, pusher, client) =>
      val metric1 = Counter.build("metric1", "help2").register(registry)
      metric1.inc(1.0)
      pusher.push()
      verify(client).count("metric1", 1.0)
      verifyNoMoreInteractions(client)
      metric1.inc(4.0)
      pusher.push()
      verify(client).count("metric1", 4.0)
      verifyNoMoreInteractions(client)
    }
  }

  it should "push metrics from a summary" in {
    withDatadogPush { (registry, pusher, client) =>
      val summary = Summary.build("metric1", "help1").quantile(0.50, 0.05).labelNames("l1").register(registry)
      summary.labels("v1").observe(1.0)
      summary.labels("v1").observe(2.0)

      pusher.push()
      verify(client).gauge("metric1", 1.0, "l1:v1", "quantile:0.5")
      verify(client).gauge("metric1_count", 2.0, "l1:v1")
      verify(client).gauge("metric1_sum", 3.0, "l1:v1")
      verifyNoMoreInteractions(client)
    }
  }

  it should "push metrics from a histogram" in {
    withDatadogPush { (registry, pusher, client) =>
      val histogram = Histogram.build("metric1", "help1").buckets(1.0, 2.0).labelNames("l1").register(registry)
      histogram.labels("v1").observe(0.5)
      histogram.labels("v1").observe(1.5)
      histogram.labels("v1").observe(2.2)

      pusher.push()
      verify(client).gauge("metric1_bucket", 1.0, "l1:v1", "le:1.0")
      verify(client).gauge("metric1_bucket", 2.0, "l1:v1", "le:2.0")
      verify(client).gauge("metric1_bucket", 3.0, "l1:v1", "le:+Inf")
      verify(client).gauge("metric1_count", 3.0, "l1:v1")
      verify(client).gauge("metric1_sum", 4.2, "l1:v1")
      verifyNoMoreInteractions(client)
    }
  }

  it should "ignore unsupported metric types" in {
    withDatadogPush { (registry, pusher, client) =>
      new UnsupportedCollector().register(registry)
      pusher.push()
      verifyNoMoreInteractions(client)
    }
  }

  it should "translate colons in names into dots" in {
    withDatadogPush { (registry, pusher, client) =>
      Gauge.build("prefix:metric1", "help1").register(registry).set(1.0)
      pusher.push()
      verify(client).gauge("prefix.metric1", 1.0)
      verifyNoMoreInteractions(client)
    }
  }

  it should "ignore metrics with a NaN value" in {
    withDatadogPush { (registry, pusher, client) =>
      Gauge.build("prefix:metric1", "help1").register(registry).set(Double.NaN)
      pusher.push()
      verifyNoMoreInteractions(client)
    }
  }
}

trait DatadogPushFixtures extends MockitoSugar {

  class UnsupportedCollector extends Collector {
    override def collect(): util.List[MetricFamilySamples] = {
      val mfs = new util.ArrayList[MetricFamilySamples]()
      val samples = new util.ArrayList[Sample]()
      samples.add(new Sample("test", util.Collections.emptyList(), util.Collections.emptyList(), 0.0, 0L))
      mfs.add(new MetricFamilySamples("test", Type.UNTYPED, "help", samples))
      mfs
    }
  }

  def withDatadogPush(test: (CollectorRegistry, DatadogPush, StatsDClient) => Any): Any = {
    val registry = new CollectorRegistry()
    val client = mock[StatsDClient]
    val pusher = new DatadogPush(client, registry)
    test(registry, pusher, client)
  }
}
