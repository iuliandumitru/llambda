package io.llambda.compiler.planner.typecheck
import io.llambda

import llambda.compiler.{ProcedureSignature, ProcedureAttribute, ReturnType}
import llambda.compiler.{valuetype => vt}
  
object TypePredicateProcSignature extends ProcedureSignature(
    hasWorldArg=false,
    hasSelfArg=false,
    restArgOpt=None,
    // We must be able to take any data type without erroring out
    fixedArgs=List(vt.AnySchemeType),
    returnType=ReturnType.SingleValue(vt.Predicate),
    attributes=Set(ProcedureAttribute.FastCC)
)
  
