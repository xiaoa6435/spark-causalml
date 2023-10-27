

```sh
~/spark-3.5.0-bin-hadoop3/bin/spark-shell \
  --jars /Users/admin/spark-causalml/spark/common/target/spark-causalml-common-0.0.1.jar \
  --conf spark.driver.extraJavaOptions="-Dio.netty.tryReflectionSetAccessible=true" \
  --conf spark.executor.extraJavaOptions="-Dio.netty.tryReflectionSetAccessible=true" \
  --conf spark.sql.extensions=org.apache.spark.sql.extra.StatExtensions
```

```scala
val df = spark.sql("""
select *
  from values ("2020-01-15 08:01:32"), ("2020-01-15 08:01:32"), (null) as df(a)
""")
df.show()
spark.sql("""
select beginning_of_month(a)
  from values ("2020-01-15 08:01:32"), ("2020-01-15 08:01:32"), (null) as df(a)
""").show()
df.queryExecution

spark.sql("""
select dnorm(a)
  from values (-1.5), (0.0), (1.1) as df(a)
""").show()
spark.sql("""
select dnorm(a, 0.0, 1.0, false)
  from values (-1.5), (0.0), (1.1) as df(a)
""").show()

spark.sql("""
select dnorm(a, 0.0, 1.0, false)
  from values (-1.5), (0.0), (null) as df(a)
""").show()

import org.apache.commons.math3.distribution.NormalDistribution

val N = (1e7).toLong
val df = spark.sql(s"select randn() as x from range($N)")
df.cache()
df.count

val beginNs = System.nanoTime()
val df2 = df.withColumn("d", expr("dnorm(x)"))
df2.write.format("noop").mode("overwrite").save()
val elapsedNs = System.nanoTime() - beginNs
println(s"native udf elapse time: ${elapsedNs / 1e9}/s, N: ${N}")
val dnormUDF = udf(x => new NormalDistribution(0.0, 1.0).density(x))

val beginNs = System.nanoTime()
val df2 = df.withColumn("d", dnormUDF(col("x")))
df2.write.format("noop").mode("overwrite").save()
val elapsedNs = System.nanoTime() - beginNs
println(s"scala udf elapse time: ${elapsedNs / 1e9}/s, N: ${N}")

// native udf elapse time: 17.853027958/s, N: 10000000
// scala udf elapse time: 18.563835542/s, N: 10000000
```