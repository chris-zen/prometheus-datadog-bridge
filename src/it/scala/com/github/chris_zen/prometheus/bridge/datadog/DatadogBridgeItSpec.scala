package com.github.chris_zen.prometheus.bridge.datadog

import java.net._

import io.prometheus.client.Counter
import org.scalatest.{FlatSpec, Matchers}


class DatadogBridgeItSpec extends FlatSpec with Matchers with DatadogBridgeItFixtures {

  "A DatadogBridge" should "push metrics to Datadog" in {
    withStatsDServer { statsD =>

      val counter = Counter.build("metric2", "help2").labelNames("l1", "l2").register()
      counter.labels("v1", "v2").inc(2.0)

      val config = DatadogBridge.Config(host = "localhost", port = statsD.port)
      val bridge = DatadogBridge(config)

      Thread.sleep(500)

      val received = statsD.receive

      bridge.stop()

      received shouldBe "metric2:2|c|#l2:v2,l1:v1"
    }
  }
}

trait DatadogBridgeItFixtures {
  class LocalStatsDServer {
    private val BufferSize = 1024

    private val buf = new Array[Byte](BufferSize)

    private val socket = new DatagramSocket(new InetSocketAddress("localhost", 0))

    def port: Int = socket.getLocalPort

    def receive: String = {
      val packet = new DatagramPacket(buf, buf.length)
      socket.receive(packet)
      new String(packet.getData, 0, packet.getLength)
    }

    def close(): Unit = {
      socket.close()
    }
  }

  def withStatsDServer(test: LocalStatsDServer => Any): Any = {
    val server = new LocalStatsDServer
    test(server)
    server.close()
  }
}