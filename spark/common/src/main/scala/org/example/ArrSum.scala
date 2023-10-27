package org.example

import org.apache.spark.sql.catalyst.encoders.ExpressionEncoder
import org.apache.spark.sql.expressions.Aggregator
import org.apache.spark.sql.functions

object ArrSum extends Aggregator[Array[Double], Array[Double], Array[Double]] {

  def zero: Array[Double] = null

  def reduce(buffer: Array[Double], data: Array[Double]): Array[Double] = {
    if (buffer == null) {
      return data
    }
    if (data == null) {
      return buffer
    }

    var i = 0
    while (i < data.length) {
      buffer(i) += data(i)
      i += 1
    }
    buffer
  }

  def merge(b1: Array[Double], b2: Array[Double]): Array[Double] = {
    reduce(b1, b2)
  }

  def finish(reduction: Array[Double]): Array[Double] = reduction

  def bufferEncoder: ExpressionEncoder[Array[Double]] = ExpressionEncoder[Array[Double]]

  def outputEncoder: ExpressionEncoder[Array[Double]] = ExpressionEncoder[Array[Double]]
}
