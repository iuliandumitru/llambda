package io.llambda.compiler
import io.llambda

import llambda.compiler.{valuetype => vt}

object ReturnType {
  /** Represents the return type of a procedure */
  sealed abstract trait ReturnType {
    /** Returns the representation used internally to contain this return type
      *
      * If no representation is required then the result is not defined
      */
    def representationTypeOpt : Option[vt.ValueType]
  }

  /** Represents a single value returned from a procedure 
    *
    * This is the normal case. Single values are represented by the their native type without any wrapping
    */
  case class SingleValue(valueType : vt.ValueType) extends ReturnType {
    def representationTypeOpt = valueType match {
      case vt.UnitType =>
        None

      case otherType =>
        Some(otherType)
    }
  }

  /** Represents multiple values returned from a procedure
    *
    * This is internally represented as a proper list of values. This makes the implementation of (values) and 
    * (call-by-values) simpler
    */
  sealed abstract trait MultipleValues extends ReturnType {
    def representationType : vt.SchemeType

    def representationTypeOpt : Option[vt.ValueType] = 
      Some(representationType)
  }

  /** Represents a fixed number of return values of specific types */
  case class SpecificValues(valueTypes : List[vt.SchemeType]) extends MultipleValues {
    def representationType : vt.SchemeType =
      vt.SpecificProperListType(valueTypes.map(vt.DirectSchemeTypeRef)) 
  }

  /** Represents an arbitrary number of return values of any type */
  object ArbitraryValues extends MultipleValues {
    def representationType : vt.SchemeType =
      vt.UniformProperListType(vt.DirectSchemeTypeRef(vt.AnySchemeType))
  }
}


