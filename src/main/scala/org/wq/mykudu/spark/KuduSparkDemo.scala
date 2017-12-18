package org.wq.mykudu.spark

import org.apache.kudu.client.CreateTableOptions

import scala.collection.JavaConverters._
import org.apache.kudu.spark.kudu._
import org.apache.kudu.spark.kudu.KuduContext
import org.apache.spark.sql.types._
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql.{DataFrame, Row, SQLContext}
import org.apache.spark.SparkContext._


object KuduSparkDemo {

  case class Customer(name: String, age: Int, city: String)

  def main(args: Array[String]): Unit = {

    val sparkConf = new SparkConf()
      .set("spark.driver.allowMultipleContexts","true")
      .setAppName("KuduSparkdemo")
      .setMaster("spark://honest:7077")
      //.setMaster("lcoal")
      .set("spark.executor.memory","1g")


    val sc = new SparkContext(sparkConf)

    val sqlContext = new org.apache.spark.sql.SQLContext(sc)

    import sqlContext.implicits._

    val km = "quickstart.cloudera:7051"
    val kuduMasters = List(km).mkString(",")

    val kuduContext = new KuduContext(kuduMasters,sc)

    // 1. Give your table a name
    val kuduTableName = "spark_kudu_tbl"

    // 2. Define a schema
    val kuduTableSchema = StructType(
      //         col name   type     nullable?
      StructField("name", StringType , false) ::
        StructField("age" , IntegerType, true ) ::
        StructField("city", StringType , true ) :: Nil)

    // 3. Define the primary key
    val kuduPrimaryKey = Seq("name")

    // 4. Specify any further options
    val kuduTableOptions = new CreateTableOptions()
    kuduTableOptions.
      setRangePartitionColumns(List("name").asJava).
      setNumReplicas(1)

    // Check if the table exists, and drop it if it does
    if (kuduContext.tableExists(kuduTableName)) {
      kuduContext.deleteTable(kuduTableName)
    }

    // 5. Call create table API
    kuduContext.createTable(kuduTableName, kuduTableSchema, kuduPrimaryKey, kuduTableOptions)

    val coutomers = List(
      Customer("jane",30,"new york"),
      Customer("denise" , 19, "winnipeg"),
      Customer("jordan",18,"toronto")
    )

    val customerRDD = sc.parallelize(coutomers)

    val kuduOptions = Map(
      "kudu.table" -> kuduTableName,
      "kudu.master" -> kuduMasters
    )

    //insert
    val customersDF = customerRDD.toDF()

    kuduContext.insertRows(customersDF,kuduTableName)

    sqlContext.read.options(kuduOptions).kudu.show


    //delete
    customersDF.createTempView("customers")

    val deleteKeysDF = sqlContext.sql("select name from customers where age > 20")

    kuduContext.deleteRows(deleteKeysDF, kuduTableName)

    sqlContext.read.options(kuduOptions).kudu.show

    //update
    val modifiedCustomers = Array(
      Customer("jordan", 20, "toronto"))
    val modifiedCustomersRDD = sc.parallelize(modifiedCustomers)
    val modifiedCustomersDF  = modifiedCustomersRDD.toDF()

    kuduContext.updateRows(modifiedCustomersDF, kuduTableName)

    sqlContext.read.options(kuduOptions).kudu.show


    //RDD
    val kuduTableProjColumns = Seq("name", "age")

    val custRDD = kuduContext.kuduRDD(sc, kuduTableName, kuduTableProjColumns)

    val custTuple = custRDD.map { case Row(name: String, age: Int) => (name, age) }

    custTuple.collect().foreach(println(_))


    //DataFrame write
    val customersAppend = Array(
      Customer("bob", 30, "boston"),
      Customer("charlie", 23, "san francisco"))

    val customersAppendDF = sc.parallelize(customersAppend).toDF()

    customersAppendDF.write.options(kuduOptions).mode("append").kudu

    sqlContext.read.options(kuduOptions).kudu.show()


    //Predicate pushdown
    sqlContext.read.options(kuduOptions).kudu.createTempView(kuduTableName)

    val customerNameAgeDF = sqlContext.sql(s"""SELECT name, age FROM $kuduTableName WHERE age >= 20""")

    customerNameAgeDF.show()


    //Spark SQL INSERT
    val srcTableData = Array(
      Customer("enzo", 43, "oakland"),
      Customer("laura", 27, "vancouver"))

    val srcTableDF = sc.parallelize(srcTableData).toDF()

    //srcTableDF.registerTempTable("source_table")
    srcTableDF.createTempView("source_table")

    //sqlContext.read.options(kuduOptions).kudu.createTempView(kuduTableName)

    sqlContext.sql(s"INSERT INTO TABLE $kuduTableName SELECT * FROM source_table")

    sqlContext.read.options(kuduOptions).kudu.show()

  }

}
