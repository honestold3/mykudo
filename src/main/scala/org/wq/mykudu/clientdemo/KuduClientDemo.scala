package org.wq.mykudu.clientdemo

import org.apache.kudu.ColumnSchema.ColumnSchemaBuilder
import org.apache.kudu.client._
import org.apache.kudu.{ColumnSchema, Schema, Type}

import scala.collection.JavaConverters._

object KuduClientDemo {

  def main(args: Array[String]): Unit = {
    val kuduMaster = "quickstart.cloudera:7051"

    val kuduClient = new KuduClient.KuduClientBuilder(kuduMaster).build()

    val columnList = new java.util.ArrayList[ColumnSchema]()
    columnList.add(new ColumnSchemaBuilder("KEY_ID", Type.STRING).key(true).build())
    columnList.add(new ColumnSchemaBuilder("COL_A", Type.INT32).key(false).build())
    columnList.add(new ColumnSchemaBuilder("COL_B", Type.STRING).key(false).build())
    columnList.add(new ColumnSchemaBuilder("COL_C", Type.STRING).key(false).build())
    val schema = new Schema(columnList)

    if (kuduClient.tableExists("foobar")) {
      kuduClient.deleteTable("foobar")
    }

    val tableOptions = new CreateTableOptions().setRangePartitionColumns(List("KEY_ID").asJava)
      .setNumReplicas(1)

    kuduClient.createTable("foobar", schema,tableOptions)

    val session = kuduClient.newSession()
    val table = kuduClient.openTable("foobar")

    try {
      val random = new java.util.Random()
      for (i <- 0 until 10) {
        val insert = table.newInsert()
        val row = insert.getRow()
        row.addString(0, i.toString)
        row.addInt(1,  i)
        row.addString(2, "42:" + i)
        row.addString(3, "Cat" + random.nextGaussian())
        session.apply(insert)
      }
      session.flush()

      //scanner
      val columnsList: java.util.ArrayList[String] = new java.util.ArrayList()
      columnsList.add("COL_A")
      columnsList.add("COL_B")
      columnsList.add("COL_C")

      val kp1 = KuduPredicate.newComparisonPredicate(schema.getColumn("COL_A"),
        KuduPredicate.ComparisonOp.GREATER, 5)
      val kp2 = KuduPredicate.newComparisonPredicate(schema.getColumn("COL_B"),
        KuduPredicate.ComparisonOp.EQUAL, "42:8")

      val scanner = kuduClient.newScannerBuilder(table)
        .setProjectedColumnNames(columnsList)
        .addPredicate(kp1)
        .addPredicate(kp2)
        .build()

      while (scanner.hasMoreRows) {
        val rows = scanner.nextRows()
        while (rows.hasNext) {
          val row = rows.next()
          println(" - " + row.rowToString())
        }
      }

    } finally {
      session.close()
    }

    println("---------------------------------------------")
    val tableList = kuduClient.getTablesList.getTablesList
    for (i <- 0 until tableList.size()) {
      println("Table " + i + ":" + tableList.get(i))
    }





  }

}
