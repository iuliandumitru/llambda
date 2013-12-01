package llambda.planner.intermediatevalue

import llambda.nfi
import llambda.{valuetype => vt}
import llambda.{boxedtype => bt}
import llambda.planner.{step => ps}
import llambda.planner.{PlanWriter, UnlocatedImpossibleTypeConversionException, InvokableProcedure}

abstract class IntermediateValue {
  val possibleTypes : Set[bt.ConcreteBoxedType]

  case class PlanPhiResult(
    ourTempValue : ps.TempValue,
    theirTempValue : ps.TempValue,
    resultTemp : ps.TempValue,
    resultIntermediate : IntermediateValue
  )

  protected def toBoxedTempValue(boxedType : bt.BoxedType)(implicit plan : PlanWriter) : Option[ps.TempValue]
  protected def toScalarTempValue(nativeType : nfi.NativeType)(implicit plan : PlanWriter) : Option[ps.TempValue]
  protected def toBoxedRecordTempValue(boxedRecordType : vt.BoxedRecordType)(implicit plan : PlanWriter) : Option[ps.TempValue]

  def toTruthyPredicate()(implicit plan : PlanWriter) : ps.TempValue = {
    val trueTemp = new ps.TempValue
    plan.steps += ps.StoreNativeInteger(trueTemp, 1, 1) 

    trueTemp
  }
  
  def toInvokableProcedure()(implicit plan : PlanWriter) : Option[InvokableProcedure]

  protected def toTempValue(targetType : vt.ValueType)(implicit plan : PlanWriter) : Option[ps.TempValue] = targetType match {
    case vt.ScalarType(nfi.CBool) =>
      val truthyPredTemp = toTruthyPredicate()

      val intConvTemp = new ps.TempValue
      plan.steps += ps.ConvertNativeInteger(intConvTemp, truthyPredTemp, nfi.CBool.bits, false)

      Some(intConvTemp)

    case vt.ScalarType(nativeType : nfi.NativeType) =>
      toScalarTempValue(nativeType)

    case vt.BoxedIntrinsicType(boxedType) =>
      toBoxedTempValue(boxedType)

    case boxedRecordType : vt.BoxedRecordType =>
      toBoxedRecordTempValue(boxedRecordType)
  }
  
  def toRequiredTempValue(targetType : vt.ValueType)(implicit plan : PlanWriter) =
    toTempValue(targetType) getOrElse {
      throw new UnlocatedImpossibleTypeConversionException(s"Unable to convert ${this.toString} to ${targetType}")
    }

  def planPhiWith(theirValue : IntermediateValue)(ourPlan : PlanWriter, theirPlan : PlanWriter) : PlanPhiResult = {
    // This is extremely inefficient for compatible unboxed types
    // This should be overridden where possible
    val ourTempValue = this.toRequiredTempValue(vt.BoxedIntrinsicType(bt.BoxedDatum))(ourPlan)
    val theirTempValue = theirValue.toRequiredTempValue(vt.BoxedIntrinsicType(bt.BoxedDatum))(theirPlan)

    val phiResultTemp = new ps.TempValue
    val phiPossibleTypes = possibleTypes ++ theirValue.possibleTypes

    PlanPhiResult(
      ourTempValue=ourTempValue,
      theirTempValue=theirTempValue,
      resultTemp=phiResultTemp,
      resultIntermediate=new BoxedIntrinsicValue(phiPossibleTypes, bt.BoxedDatum, phiResultTemp)
    )
  }
}

