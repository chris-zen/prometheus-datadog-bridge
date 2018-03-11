package com.github.chris_zen.prometheus.bridge.datadog

import com.timgroup.statsd.{NonBlockingStatsDClient, StatsDClient}
import io.prometheus.client.Collector.{MetricFamilySamples, Type}
import io.prometheus.client.CollectorRegistry
import org.slf4j.{Logger, LoggerFactory}


object DatadogPush {

  private val logger: Logger = LoggerFactory.getLogger(this.getClass.getName.replace("$", ""))

  def apply(config: DatadogBridge.Config): DatadogPush = {
    val client = new NonBlockingStatsDClient(config.prefix, config.host, config.port, config.tags: _*)
    val registry = config.registry.getOrElse(CollectorRegistry.defaultRegistry)
    new DatadogPush(client, registry)
  }
}

class DatadogPush private[datadog] (client: StatsDClient,
                                    private[datadog] val registry: CollectorRegistry) {

  import DatadogPush.logger

  import collection.JavaConverters._

  private case class UnsupportedMetricType(metricType: String, name: String)

  def close(): Unit = {
    client.close()
  }

  def push(): Unit = {

    val unsupportedMetricTypes = for {
      metricFamilySamples    <- registry.metricFamilySamples().asScala.toList
      metricName              = metricFamilySamples.name.replace(':', '.')
      sample                 <- metricFamilySamples.samples.asScala
      unsupportedMetricType  <- reportSample(metricFamilySamples.`type`, metricName, sample).left.toOption
    } yield unsupportedMetricType

    for (UnsupportedMetricType(metricType, name) <- unsupportedMetricTypes.toSet) {
      logger.error(s"Found samples for unsupported metric type for metric" +
        s" '$name' with type '$metricType'")
    }
  }

  private def reportSample(metricType: Type,
                           metricName: String,
                           sample: MetricFamilySamples.Sample): Either[UnsupportedMetricType, Unit] = {

    val tags = labelsAsTags(sample)

    if (logger.isTraceEnabled) {
      logger.trace("{}: [{}] {}={} ({}/{})",
        metricType.toString, metricName, sample.name, sample.value.toString,
        sample.labelNames.toString, sample.labelValues.toString)
    }

    metricType match {
      case Type.GAUGE | Type.SUMMARY | Type.HISTOGRAM =>
        Right(client.gauge(sample.name, sample.value, tags: _*))

      case Type.COUNTER =>
        Right(client.count(sample.name, sample.value, tags: _*))

      case _ =>
        Left(UnsupportedMetricType(metricType.toString, metricName))
    }
  }

  private def labelsAsTags(sample: MetricFamilySamples.Sample): Seq[String] = {
    sample.labelNames.asScala.zip(sample.labelValues.asScala).map {
      case (name, value) if value == null => name
      case (name, value) if value != null => s"$name:$value"
    }
  }
}
