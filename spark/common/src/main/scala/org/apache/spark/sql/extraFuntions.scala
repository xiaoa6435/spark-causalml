package org.apache.spark.sql

import org.apache.spark.sql.catalyst.expressions._
import org.apache.spark.sql.catalyst.expressions.aggregate._
import org.apache.spark.sql.types.StringType
import org.apache.spark.unsafe.types.UTF8String
import org.apache.spark.sql.functions.{expr, lit, when}

/** @groupname string_funcs String Functions
  * @groupname agg_funcs Aggregate Functions
  * @groupname math_funcs Math Functions
  */
object extraFunctions {

  private def withExpr(expr: Expression): Column = Column(expr)
  
  def beginningOfMonth(col: Column): Column =
    withExpr {
      BeginningOfMonth(col.expr)
    }
}
