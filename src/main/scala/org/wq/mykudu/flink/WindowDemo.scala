package org.wq.mykudu.flink

import org.apache.flink.streaming.api.scala._

object WindowDemo {

  def main(args: Array[String]): Unit = {
    if (args.length != 2) {
      System.err.println("USAGE:\nKuduFlinkDemo <hostname> <port>")
      return
    }

    val hostName = args(0)
    val port = args(1).toInt

    val env = StreamExecutionEnvironment.getExecutionEnvironment

    val text = env.socketTextStream(hostName, port)

    val counts = text.flatMap{x => x.split("\\s")}
      .map { (_, 1) }
      .keyBy(0)
      .countWindow(5)
      .sum(1)
      .setParallelism(4);
    counts.print()
    env.execute()
  }

}
