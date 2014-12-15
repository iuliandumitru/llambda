package io.llambda.compiler.planner.reportproc
import io.llambda

import llambda.compiler.{valuetype => vt}
import llambda.compiler.ContextLocated
import llambda.compiler.planner.{step => ps}
import llambda.compiler.planner.{intermediatevalue => iv}
import llambda.compiler.planner._

object StringProcPlanner extends ReportProcPlanner {
  override def planWithValue(state : PlannerState)(
      reportName : String,
      operands : List[(ContextLocated, iv.IntermediateValue)]
  )(implicit plan : PlanWriter) : Option[iv.IntermediateValue] = (reportName, operands) match {
    case ("string-length", List((stringLocated, stringValue))) =>
      stringValue match {
        case iv.ConstantStringValue(stringValue) =>
          // This is easy
          val codePoints = stringValue.codePointCount(0, stringValue.length)

          Some(iv.ConstantExactIntegerValue(codePoints))

        case dynamicValue =>
          val stringCell = plan.withContextLocation(stringLocated) {
            dynamicValue.toTempValue(vt.StringType)
          }

          val resultTemp = ps.Temp(vt.UInt32)
          plan.steps += ps.LoadStringCharLength(resultTemp, stringCell)

          Some(new iv.NativeExactIntegerValue(resultTemp, vt.UInt32))
      }

    case _ =>
      None
  }
}
