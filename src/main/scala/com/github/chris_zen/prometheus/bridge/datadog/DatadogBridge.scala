package com.github.chris_zen.prometheus.bridge.datadog

import java.time.Duration
import java.util.concurrent._

import com.timgroup.statsd.NonBlockingStatsDClient
import io.prometheus.client.CollectorRegistry
import org.slf4j.{Logger, LoggerFactory}


object DatadogBridge {

  private val logger: Logger = LoggerFactory.getLogger(getClass.getName.split("[$.]").last)

  private val DefaultDatadogHost = "localhost"
  private val DefaultDatadogPort = 8125
  private val DefaultDatadogPrefix = ""
  private val DefaultPeriod = Duration.ofMinutes(1)

  case class Config(host: String = DefaultDatadogHost,
                    port: Int = DefaultDatadogPort,
                    prefix: String = DefaultDatadogPrefix,
                    tags: Seq[String] = Seq.empty,
                    period: Duration = DefaultPeriod,
                    registry: Option[CollectorRegistry] = None)

  private val BridgeThreadFactory = new ThreadFactory {
    def newThread(r: Runnable): Thread = new Thread(r, "prometheus-datadog-bridge")
  }

  def apply(config: Config): DatadogBridge = {
    val client = new NonBlockingStatsDClient(config.prefix, config.host, config.port, config.tags: _*)
    val scheduledThreadPool = Executors.newScheduledThreadPool(1, BridgeThreadFactory)
    val registry = config.registry.getOrElse(CollectorRegistry.defaultRegistry)
    val pusher = new DatadogPush(client, registry)
    new DatadogBridge(scheduledThreadPool, config.period, registry, pusher)
  }
}

class DatadogBridge private[datadog] (scheduledThreadPool: ScheduledExecutorService,
                                      period: Duration,
                                      registry: CollectorRegistry,
                                      pusher: DatadogPush) {

  private val pushTask = new Runnable {
    def run(): Unit = {
      pusher.push()
    }
  }

  scheduledThreadPool.scheduleAtFixedRate(pushTask, 0, period.toMillis, TimeUnit.MILLISECONDS)

  def stop(): Unit = {
    this.synchronized {
      if (!scheduledThreadPool.isShutdown) {
        scheduledThreadPool.shutdown()
      }
      pusher.close()
    }
  }
}
