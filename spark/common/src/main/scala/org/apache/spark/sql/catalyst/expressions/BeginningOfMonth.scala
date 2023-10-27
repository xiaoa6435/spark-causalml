package org.apache.spark.sql.catalyst.expressions

import org.apache.spark.sql.catalyst.FunctionIdentifier
import org.apache.spark.sql.catalyst.expressions.codegen.Block._
import org.apache.spark.sql.catalyst.expressions.codegen._
import org.apache.spark.sql.catalyst.util.DateTimeUtils
import org.apache.spark.sql.extra.{ExpressionUtils, FunctionDescription}
import org.apache.spark.sql.types.{AbstractDataType, DataType, DateType}
import org.apache.spark.unsafe.types.UTF8String


/** Returns the first day of the month which the date belongs to.
  */
@ExpressionDescription(
  usage =
    "_FUNC_(date) - Returns the first day of the month which the date belongs to.",
  examples = """
    Examples:
      > SELECT _FUNC_('2009-01-12');
       2009-01-01
  """,
  group = "datetime_funcs",
  since = "0.0.2"
)
case class BeginningOfMonth(child: Expression)
    extends UnaryExpression
    with ImplicitCastInputTypes
    with NullIntolerant {
  // override def child: Expression = startDate

  override def inputTypes: Seq[AbstractDataType] = Seq(DateType)

  override def dataType: DataType = DateType

  override def nullSafeEval(input: Any): Any = {
    val level = DateTimeUtils.parseTruncLevel(UTF8String.fromString("MONTH"))
    DateTimeUtils.truncDate(input.asInstanceOf[Int], level)
  }

  override protected def doGenCode(
      ctx: CodegenContext,
      ev: ExprCode
  ): ExprCode = {
    val level = DateTimeUtils.parseTruncLevel(UTF8String.fromString("MONTH"))
    val dtu = DateTimeUtils.getClass.getName.stripSuffix("$")
    defineCodeGen(ctx, ev, sd => s"$dtu.parseTruncLevel($sd, $level)")
  }

  override def prettyName: String = "beginning_of_month"

  override protected def withNewChildInternal(
      newChild: Expression
  ): BeginningOfMonth = copy(child = newChild)
}

object BeginningOfMonth {
//  def dnorm

  val fd: FunctionDescription = (
    new FunctionIdentifier("beginning_of_month"),
    ExpressionUtils.getExpressionInfo(classOf[BeginningOfMonth], "beginning_of_month"),
//    (children: Seq[Expression]) => if ( children.size == 1) {
//      Age(CurrentDate(), children.head)
//    } else {
//      Age(children.head, children.last)
//    })
    (children: Seq[Expression]) => BeginningOfMonth(children.head))
}