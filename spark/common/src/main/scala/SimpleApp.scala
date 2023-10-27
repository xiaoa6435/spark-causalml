import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.extraFunctions._
import org.apache.spark.sql.functions.col

object SimpleApp {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder.appName("Simple Application").getOrCreate()
    println(s"\nhello ${spark.version}, args: {args.toList()}")

    import spark.implicits._
    val df = Seq(("2020-01-15 08:01:32"), ("2020-01-15 08:01:32"), (null)).toDF(
      "start_date"
    )
    println("df")
    println(df.show())
    println(df.collect().toList)
    val df2 = df.withColumn("actual", beginningOfMonth(col("start_date")))
    println("df2")
    println(df2.show())
    println(df2.collect().toList)
    spark.stop()
  }
}