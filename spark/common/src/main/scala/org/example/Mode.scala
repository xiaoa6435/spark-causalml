package org.example

import org.apache.spark.sql.Row
import org.apache.spark.sql.expressions.{MutableAggregationBuffer, UserDefinedAggregateFunction}
import org.apache.spark.sql.types._

/**
 * Spark User Defined Aggregate Function to calculate the most frequent value in a column. This is similar to
 * Statistical Mode. When there are two random values, this function selects any one. When calculating mode, both
 * these values together is considered as mode.
 *
 * Usage:
 *
 * DataFrame / DataSet DSL
 * val mostCommonValue = new MostCommonValue
 * df.groupBy("group_id").agg(mostCommonValue(col("mode_column")), mostCommonValue(col("city")))
 *
 * Spark SQL:
 * sqlContext.udf.register("mode", new MostCommonValue)
 * %sql
 * -- Use a group_by statement and call the UDAF.
 * select group_id, mode(id) from table group by group_id
 *
 * Reference: https://docs.databricks.com/spark/latest/spark-sql/udaf-scala.html
 *
 * This version doesn't use the ScalaZ library
 *
 * Created by anish on 26/05/17.
 *
 * create table df as select explode(Array(1, 2, 1, 3)) as a;
 * CREATE TEMPORARY FUNCTION mode AS 'Mode';
 * select mode(a) from df;
 *
 */
class Mode extends UserDefinedAggregateFunction {

  // This is the input fields for your aggregate function.
  // We use StringType, because Mode can also be meaningfully applied on nominal data
  override def inputSchema: StructType =
    StructType(StructField("value", StringType) :: Nil)

  // This is the internal fields you keep for computing your aggregate.
  // We store the frequency of all the distinct element we encounter for the given attribute in this HashMap
  override def bufferSchema: StructType = StructType(
    StructField("frequencyMap", DataTypes.createMapType(StringType, LongType)) :: Nil
  )

  // This is the output type of your aggregation function.
  override def dataType: DataType = StringType

  override def deterministic: Boolean = true

  // This is the initial value for the buffer schema.
  override def initialize(buffer: MutableAggregationBuffer): Unit = {
    buffer(0) = Map[String, Long]()
  }

  // This is how to update your buffer schema given an input.
  override def update(buffer: MutableAggregationBuffer, input: Row): Unit = {
    val inpString = input.getAs[String](0)
    val existingMap = buffer.getAs[Map[String, Long]](0)
    buffer(0) = existingMap + (if (existingMap.contains(inpString)) inpString -> (existingMap(inpString) + 1) else inpString -> 1L)
  }

  // This is how you merge two objects with the bufferSchema type.
  override def merge(buffer1: MutableAggregationBuffer, buffer2: Row): Unit = {
    val map1 = buffer1.getAs[Map[String, Long]](0)
    val map2 = buffer2.getAs[Map[String, Long]](0)
    buffer1(0) =  map1 ++ map2.map{ case (k,v) => k -> (v + map1.getOrElse(k,0L)) }
  }

  // This is where you output the final value, given the final value of your bufferSchema.
  override def evaluate(buffer: Row): String = {
    buffer.getAs[Map[String, Long]](0).maxBy(_._2)._1
  }
}