package org.wq.mykudu.spark

import com.google.common.collect.ImmutableList
import org.apache.kudu.ColumnSchema.ColumnSchemaBuilder
import org.apache.kudu.client.KuduClient.KuduClientBuilder
import org.apache.kudu.spark.kudu.KuduContext
import org.apache.spark.SparkConf
import org.apache.spark.sql.{Row, SparkSession}
import org.apache.spark.sql.SQLContext
import org.apache.spark.SparkContext._
import org.apache.spark.sql.types._

import scala.collection.JavaConverters._
import org.apache.kudu.spark.kudu._
import org.apache.kudu.client._
import org.apache.kudu.{Schema, Type}



object KuduSparkDemo2 {

  case class Customer(name: String, age: Int, city: String)

  def main(args: Array[String]): Unit = {

    val sparkConf = new SparkConf()
      .setAppName("KuduSparkdemo")
      .setMaster("spark://honest:7077")
      .set("spark.executor.memory","1g")
      .set("spark.driver.allowMultipleContexts","true")

    val spark = SparkSession.builder
      .config(sparkConf)
      .appName("example")
      .getOrCreate()

    import spark.implicits._

    val sc = spark.sparkContext
    val sqlContext = spark.sqlContext



    val km = "quickstart.cloudera:7051"
    val kuduMasters = List(km).mkString(",")


    val kuduClient = new KuduClientBuilder(kuduMasters).build()

    val kuduSession = kuduClient.newSession()

    val tableName: String = "testkudu"

    lazy val schema: Schema = {
      val columns = ImmutableList.of(
        new ColumnSchemaBuilder("key", Type.INT32).key(true).build(),
        new ColumnSchemaBuilder("c1_i", Type.INT32).build(),
        new ColumnSchemaBuilder("c2_s", Type.STRING).nullable(true).build(),
        new ColumnSchemaBuilder("c3_double", Type.DOUBLE).build(),
        new ColumnSchemaBuilder("c4_long", Type.INT64).build(),
        new ColumnSchemaBuilder("c5_bool", Type.BOOL).build(),
        new ColumnSchemaBuilder("c6_short", Type.INT16).build(),
        new ColumnSchemaBuilder("c7_float", Type.FLOAT).build(),
        new ColumnSchemaBuilder("c8_binary", Type.BINARY).build(),
        new ColumnSchemaBuilder("c9_unixtime_micros", Type.UNIXTIME_MICROS).build(),
        new ColumnSchemaBuilder("c10_byte", Type.INT8).build())
      new Schema(columns)
    }

    val tableOptions = new CreateTableOptions().setRangePartitionColumns(List("key").asJava)
      .setNumReplicas(1)

    //val table = kuduClient.createTable(tableName, schema, tableOptions)



    val kuduTableName = "spark_kudu_tbl"

    val coutomers = Array(
      Customer("jane",30,"new york"),
      Customer("jordan",18,"toronto")
    )

    val customerRDD = sc.parallelize(coutomers)

    val kuduOptions = Map(
      "kudu.table" -> kuduTableName,
      "kudu.master" -> kuduMasters
    )


    val customersDF = customerRDD.toDF()

    val kuduContext = new KuduContext(kuduMasters,sc)

    sqlContext.read.options(kuduOptions).kudu.createTempView(kuduTableName)

    //val customerNameAgeDF = sqlContext.sql(s"""SELECT name, age FROM $kuduTableName WHERE age >= 20""")

    val customerNameAgeDF = sqlContext.sql(s"""SELECT name, age,city FROM $kuduTableName""")

    customerNameAgeDF.show()


  }

}
