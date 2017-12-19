package org.wq.mykudu.flink

import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.triggers.ContinuousProcessingTimeTrigger
import org.apache.flink.streaming.connectors.fs._
import org.apache.flink.streaming.connectors.fs.bucketing.{BucketingSink, DateTimeBucketer}
import es.accenture.flink.Sink.KuduSink
import es.accenture.flink.Utils.RowSerializable


object KuduFlinkDemo {

  def main(args: Array[String]): Unit = {
    if (args.length != 2) {
      System.err.println("USAGE:\nKuduFlinkDemo <hostname> <port>")
      return
    }

    val hostName = args(0)
    val port = args(1).toInt

    val env = StreamExecutionEnvironment.getExecutionEnvironment

    val text = env.socketTextStream(hostName, port)

    val data = text.map{ x=>
      val ss= x.toString.split(",")
      val row = new RowSerializable(3)
      row.setField(0,ss(0))
      row.setField(1,ss(1).toInt)
      row.setField(2,ss(2))
      row
    }

    val KuduMaster = "quickstart.cloudera:7051"
    val KuduTableName = "spark_kudu_tbl"
    val Columns = Array(
      "name",
      "age",
      "city"
    )

    val kuduSink: KuduSink = new KuduSink(
      KuduMaster,
      KuduTableName,
      Columns)

    data.addSink(kuduSink)

    env.execute("Kudu Example")

  }

}
