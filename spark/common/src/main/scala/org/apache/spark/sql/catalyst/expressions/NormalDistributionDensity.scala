package org.apache.spark.sql.catalyst.expressions

import org.apache.commons.math3.distribution.NormalDistribution
import org.apache.spark.sql.catalyst.FunctionIdentifier
import org.apache.spark.sql.catalyst.expressions.codegen.{CodegenContext, ExprCode}
import org.apache.spark.sql.extra.{ExpressionUtils, FunctionDescription}
import org.apache.spark.sql.types.{AbstractDataType, BooleanType, DataType, DoubleType}

//class NormalDistribution {
//
//}
//

@ExpressionDescription(
  usage = "_FUNC_([days[, hours[, mins[, secs]]]]) - Make DayTimeIntervalType duration from days, hours, mins and secs.",
  arguments = """
    Arguments:
      * days - the number of days, positive or negative
      * hours - the number of hours, positive or negative
      * mins - the number of minutes, positive or negative
      * secs - the number of seconds with the fractional part in microsecond precision.
  """,
  examples = """
    Examples:
      > SELECT _FUNC_(1, 12, 30, 01.001001);
       1 12:30:01.001001000
      > SELECT _FUNC_(2);
       2 00:00:00.000000000
      > SELECT _FUNC_(100, null, 3);
       NULL
  """,
  since = "3.2.0",
  group = "datetime_funcs")
// scalastyle:on line.size.limit
case class NormalDistributionDensity(
  x: Expression,
  mean: Expression,
  sd: Expression,
  logp: Expression)
  extends QuaternaryExpression with ImplicitCastInputTypes with NullIntolerant {

  def this(
    x: Expression,
    mean: Expression,
    sd: Expression) = {
    this(x, mean, sd, Literal(false))
  }
  def this(x: Expression, mean: Expression) = this(x, mean, Literal(1.0))
  def this(x: Expression) = this(x, Literal(0.0))
//  def this() = this(Literal(0))

  override def first: Expression = x
  override def second: Expression = mean
  override def third: Expression = sd
  override def fourth: Expression = logp

  // Accept `secs` as DecimalType to avoid loosing precision of microseconds when converting
  // them to the fractional part of `secs`.
  override def inputTypes: Seq[AbstractDataType] = Seq(DoubleType, DoubleType, DoubleType, BooleanType)
  override def dataType: DataType = DoubleType

  override def nullSafeEval(
    x: Any,
    mean: Any,
    sd: Any,
    logp: Any): Any = {
//    IntervalUtils.makeDayTimeInterval(
//      day.asInstanceOf[Int],
//      hour.asInstanceOf[Int],
//      min.asInstanceOf[Int],
//      sec.asInstanceOf[Decimal])
//
//    if (logp.asInstanceOf[Boolean]) {
//      new NormalDistribution(
//        mean.asInstanceOf[Double], sd.asInstanceOf[Double]
//      ).logDensity(x.asInstanceOf[Double])
//    } else {
//      new NormalDistribution(
//        mean.asInstanceOf[Double], sd.asInstanceOf[Double]
//      ).density(x.asInstanceOf[Double])
//    }
    NormalDistributionDensity.dnorm(
      x.asInstanceOf[Double],
      mean.asInstanceOf[Double],
      sd.asInstanceOf[Double],
      logp.asInstanceOf[Boolean]
    )
  }

  override def doGenCode(ctx: CodegenContext, ev: ExprCode): ExprCode = {
    defineCodeGen(ctx, ev, (x, mean, sd, logp) => {
      val norm = NormalDistributionDensity.getClass.getName.stripSuffix("$")
      s"$norm.dnorm($x, $mean, $sd, $logp)"
    })
  }

  override def prettyName: String = "dnorm"

  override protected def withNewChildrenInternal(
    x: Expression,
    mean: Expression,
    sd: Expression,
    logp: Expression): NormalDistributionDensity =
    copy(x, mean, sd, logp)
}

object NormalDistributionDensity {
  def dnorm(x: Double, mean: Double, sd: Double, logp: Boolean): Double = {
    if (logp) {
      new NormalDistribution(mean, sd).logDensity(x)
    } else {
      new NormalDistribution(mean, sd).density(x)
    }
  }

  val fd: FunctionDescription = (
    new FunctionIdentifier("dnorm"),
    ExpressionUtils.getExpressionInfo(classOf[NormalDistributionDensity], "dnorm"),
    (children: Seq[Expression]) => children.size match {
      case 1 => NormalDistributionDensity(children.head, Literal(0.0), Literal(1.0), Literal(false))
      case 2 => NormalDistributionDensity(children.head, children(1), Literal(1.0), Literal(false))
      case 3 => NormalDistributionDensity(children.head, children(1), children(2), Literal(false))
      case _ => NormalDistributionDensity(children.head, children(1), children(2), children.last)
    })

//      if ( children.size == 1) {
//      Age(CurrentDate(), children.head)
//    } else {
//      NormalDistributionDensity(children.head, children(1), children(2), children.last)
//    })
//    (children: Seq[Expression]) => NormalDistributionDensity(children.head, children(1), children(2), children.last))
}