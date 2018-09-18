package com.github.chris_zen.prometheus.bridge.datadog

import com.timgroup.statsd.{NonBlockingStatsDClient, StatsDClient}
import io.prometheus.client.Collector.{MetricFamilySamples, Type}
import io.prometheus.client.CollectorRegistry
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable


object DatadogPush {

  private val logger: Logger = LoggerFactory.getLogger(this.getClass.getName.replace("$", ""))

  def apply(config: DatadogBridgeConfig): DatadogPush = {
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

  private val counterPreviousValues: mutable.Map[Seq[(String, String)], Double] = mutable.Map.empty

  def close(): Unit = {
    client.close()
  }

  def push(): DatadogPush = {

    val unsupportedMetricTypes = for {
      metricFamilySamples    <- registry.metricFamilySamples().asScala.toList
      metricName              = metricFamilySamples.name.replace(':', '.')
      sample                 <- metricFamilySamples.samples.asScala
      sampleName              = sample.name.replace(':', '.')
      sampleValue             = sample.value
      sampleLabels            = sample.labelNames.asScala.zip(sample.labelValues.asScala)
      unsupportedMetricType  <- reportSample(metricFamilySamples.`type`, metricName,
                                             sampleName, sampleValue, sampleLabels).left.toOption
    } yield unsupportedMetricType

    for (UnsupportedMetricType(metricType, name) <- unsupportedMetricTypes.toSet) {
      logger.error(s"Found samples for unsupported metric type for metric" +
        s" '$name' with type '$metricType'")
    }

    this
  }

  private def reportSample(metricType: Type,
                           metricName: String,
                           sampleName: String,
                           sampleValue: Double,
                           sampleLabels: Seq[(String, String)]): Either[UnsupportedMetricType, Unit] = {

    val tags = labelsAsTags(sampleLabels)

    if (logger.isTraceEnabled) {
      logger.trace("{}: [{}] {}={} ({})",
        metricType.toString, metricName, sampleName, sampleValue.toString, tags.mkString(","))
    }

    metricType match {
      case Type.GAUGE | Type.SUMMARY | Type.HISTOGRAM =>
        Right(client.gauge(sampleName, sampleValue, tags: _*))

      case Type.COUNTER =>
        val key = (metricName, sampleName) +: sampleLabels
        val prevValue = counterPreviousValues.getOrElse(key, 0.0)
        counterPreviousValues += key -> sampleValue
        val delta = sampleValue - prevValue
        Right(client.count(sampleName, delta, tags: _*))

      case _ =>
        Left(UnsupportedMetricType(metricType.toString, metricName))
    }
  }

  private def labelsAsTags(sampleLabels: Seq[(String, String)]): Seq[String] = {
    sampleLabels.map {
      case (name, value) if value == null => name
      case (name, value) if value != null => s"$name:$value"
    }
  }
}
