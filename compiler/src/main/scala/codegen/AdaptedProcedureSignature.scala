package io.llambda.compiler.codegen
import io.llambda

import llambda.compiler.ProcedureSignature
import llambda.compiler.{valuetype => vt}
import llambda.compiler.ReturnType

/** Signature for all boxed procedure values
  *
  * If the underlying procedure has a different signature then a trampoline with this signature will be generated at
  * compile time
  */
object AdaptedProcedureSignature extends ProcedureSignature(
  hasWorldArg=true,
  hasSelfArg=true,
  fixedArgs=Nil,
  restArgOpt=Some(vt.AnySchemeType),
  returnType=ReturnType.ArbitraryValues,
  attributes=Set()
)
