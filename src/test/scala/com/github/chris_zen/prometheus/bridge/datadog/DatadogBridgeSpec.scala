package com.github.chris_zen.prometheus.bridge.datadog

import java.time.Duration
import java.util.concurrent.{ScheduledExecutorService, TimeUnit}

import io.prometheus.client.CollectorRegistry
import org.mockito.ArgumentCaptor
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}
import org.mockito.Mockito.{times, verify, verifyNoMoreInteractions, when}
import org.mockito.ArgumentMatchers.{any, eq => mockitoEq}


class DatadogBridgeSpec extends FlatSpec with Matchers with DatadogBridgeFixtures {

  "A DatadogBridge" should "setup the ScheduledExecutorService" in {
    withDatadogBridge { (scheduledThreadPool, _, _) =>
      verify(scheduledThreadPool)
        .scheduleAtFixedRate(any(), mockitoEq(0L), mockitoEq(period.toMillis), mockitoEq(TimeUnit.MILLISECONDS))
      verifyNoMoreInteractions(scheduledThreadPool)
    }
  }

  it should "shutdown the ScheduledExecutorService" in {
    withDatadogBridge { (scheduledThreadPool, _, bridge) =>
      when(scheduledThreadPool.isShutdown).thenReturn(false)
      bridge.stop()
      verify(scheduledThreadPool).shutdown()
    }
  }

  it should "not shutdown the ScheduledExecutorService" in {
    withDatadogBridge { (scheduledThreadPool, _, bridge) =>
      when(scheduledThreadPool.isShutdown).thenReturn(true)
      bridge.stop()
      verify(scheduledThreadPool, times(0)).shutdown()
    }
  }

  it should "push the metrics when scheduled" in {
    withDatadogBridgeAndRegistry { (scheduledThreadPool, pusher, _, registry) =>
      val taskCaptor = ArgumentCaptor.forClass[Runnable, Runnable](classOf[Runnable])

      verify(scheduledThreadPool).scheduleAtFixedRate(taskCaptor.capture(),
        mockitoEq(0L), mockitoEq(period.toMillis), mockitoEq(TimeUnit.MILLISECONDS))

      taskCaptor.getValue.run()

      verify(pusher, times(1)).push()
    }
  }
}

trait DatadogBridgeFixtures extends MockitoSugar {

  val period: Duration = Duration.ofMinutes(1)

  def withDatadogBridge(test: (ScheduledExecutorService, DatadogPush, DatadogBridge) => Any): Any = {
    withDatadogBridgeAndRegistry { (scheduledThreadPool, pusher, bridge, _) =>
      test(scheduledThreadPool, pusher, bridge)
    }
  }

  def withDatadogBridgeAndRegistry(test: (ScheduledExecutorService, DatadogPush, DatadogBridge, CollectorRegistry) => Any): Any = {
    val scheduledThreadPool = mock[ScheduledExecutorService]
    val registry = new CollectorRegistry()
    val pusher = mock[DatadogPush]
    val bridge = new DatadogBridge(scheduledThreadPool, period, registry, pusher)
    test(scheduledThreadPool, pusher, bridge, registry)
  }
}