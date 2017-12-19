package org.wq.mykudu.clientdemo

import org.apache.kudu.ColumnSchema.ColumnSchemaBuilder
import org.apache.kudu.client.{AsyncKuduClient, CreateTableOptions, KuduClient}
import org.apache.kudu.{ColumnSchema, Schema, Type}

import scala.collection.JavaConverters._

object KuduClientDemo2 {

  def main(args: Array[String]): Unit = {
    val kuduMaster = "quickstart.cloudera:7051"
    val tableName = "kankan"
    val numOfColumns = 6
    val numOfRows = 20

    //val kuduClient = new KuduClient.KuduClientBuilder(kuduMaster).build()
    val kuduClient = new AsyncKuduClient.AsyncKuduClientBuilder(kuduMaster).build()

    try {
      //Delete table if exist
      if (kuduClient.tableExists(tableName).join()) {
        kuduClient.deleteTable(tableName).join()
      }

      //Create Schema
      val columnList = new java.util.ArrayList[ColumnSchema]()
      columnList.add(new ColumnSchemaBuilder("key_id", Type.STRING).key(true).build())
      for (c <- 0 until numOfColumns) {
        columnList.add(new ColumnSchemaBuilder("col_" + c, Type.INT32).key(false).build())
      }
      val schema = new Schema(columnList)

      val tableOptions = new CreateTableOptions().setRangePartitionColumns(List("key_id").asJava)
        .setNumReplicas(1)

      //Create table
      kuduClient.createTable(tableName, schema,tableOptions).join()

      //Populate table
      val random = new java.util.Random
      val table = kuduClient.openTable(tableName).join()
      val asyncSession = kuduClient.newSession()

      for (r <- 0 until numOfRows) {
        val insert = table.newInsert()
        val row = insert.getRow()
        row.addString(0, "hehe")
        val columns = table.getSchema.getColumns
        for (c <- 1 until columns.size()) {
          row.addInt(columns.get(c).getName, random.nextInt(100000))
        }
        asyncSession.apply(insert)

        if (r % 1000 == 0) {
          println("Inserted: " + r)
        }
      }
      asyncSession.flush()

      val scanner = kuduClient.newScannerBuilder(table).build()
      while (scanner.hasMoreRows) {
        val rows = scanner.nextRows().join()
        while (rows.hasNext) {
          val row = rows.next()
          println(" - " + row.rowToString())
        }
      }

      asyncSession.close()

    } finally {
      kuduClient.shutdown()
    }

  }

}
