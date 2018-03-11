package com.github.chris_zen.prometheus.bridge.datadog

import java.net.{DatagramPacket, DatagramSocket, InetSocketAddress, SocketTimeoutException}

import scala.annotation.tailrec

trait StatsDFixtures {
  class LocalStatsDServer {
    private val BufferSize = 1024
    private val TimeoutMillis = 1500

    private val buf = new Array[Byte](BufferSize)

    private val socket = {
      val socket = new DatagramSocket(new InetSocketAddress("localhost", 0))
      socket.setSoTimeout(TimeoutMillis)
      socket
    }

    def port: Int = socket.getLocalPort

    def receive: String = {
      val packet = new DatagramPacket(buf, buf.length)
      try {
        socket.receive(packet)
      }
      catch {
        case _: SocketTimeoutException => packet.setLength(0)
      }
      new String(packet.getData, 0, packet.getLength)
    }

    def receiveAll: List[String] = {
      @tailrec def rec(list: List[String]): List[String] = receive match {
        case msg if msg.isEmpty => list
        case msg if msg.nonEmpty => rec(list ++ msg.split("\n"))
      }
      rec(List.empty)
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
