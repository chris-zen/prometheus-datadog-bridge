# prometheus-datadog-bridge

[![Build](https://travis-ci.org/chris-zen/prometheus-datadog-bridge.svg?branch=master)](https://travis-ci.org/chris-zen/prometheus-datadog-bridge)
[![Coverage](https://codecov.io/gh/chris-zen/prometheus-datadog-bridge/branch/master/graph/badge.svg)](https://codecov.io/gh/chris-zen/prometheus-datadog-bridge)
[ ![Download](https://api.bintray.com/packages/chris-zen/maven/prometheus-datadog-bridge/images/download.svg) ](https://bintray.com/chris-zen/maven/prometheus-datadog-bridge/_latestVersion)

A [Prometheus](https://prometheus.io/) bridge to push metrics into [Datadog](https://www.datadoghq.com/)

## Examples of use

There are two cases where the Prometheus bridge to Datadog can be used.

### Services

A service can be seen as an application that it is running all the time
 and needs to report metrics periodically. The following example shows
 how easy is to setup the bridge to report at intervals of 30 seconds.

```scala
import java.time.Duration
import io.prometheus.client.Summary
import com.github.chris_zen.prometheus.bridge.datadog.{DatadogBridge, DatadogBridgeConfig}

object ServiceExample {

  private val execTime = Summary.build("exec_time_ms", "Execution time").register()
  
  private val ReportingSeconds = 30

  def main(args: Array[String]): Unit = {
    
    val bridgeConfig = DatadogBridgeConfig()
      .withPrefix("example")
      .withTags("name:example", "team:mazingerz")
      .withPeriod(Duration.ofSeconds(ReportingSeconds))
    
    val bridge = DatadogBridge(bridgeConfig)
    
    while (true) {
      instrumentedCode()
    }
    
    bridge.stop()
  }

  private def instrumentedCode(): Unit = {
    val timer = execTime.startTimer()

    // Do something ...

    timer.observeDuration()
  }
}
```

### Batch jobs

A batch job is an application that executes for an specific period of time and then finishes.
In such cases, rather than being interested in reporting metrics periodically, it is more interesting
to report the metrics at the end, when the logic has finished and the metrics have been computed.

```scala
import com.github.chris_zen.prometheus.bridge.datadog.{DatadogBridgeConfig, DatadogPush}
import io.prometheus.client.Summary

object BatchJobExample {

  private val execTime = Summary.build("exec_time_ms", "Execution time").register()

  def main(args: Array[String]): Unit = {

    instrumentedCode()

    val bridgeConfig = DatadogBridgeConfig()
      .withPrefix("example")
      .withTags("name:example", "team:mazingerz")

    DatadogPush(bridgeConfig)
      .push()
      .close()
  }

  private def instrumentedCode(): Unit = {
    val timer = execTime.startTimer()

    // Do something ...

    timer.observeDuration()
  }
}
```
