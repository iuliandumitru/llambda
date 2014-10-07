package io.llambda.compiler.codegen
import io.llambda

import llambda.llvmir._
import llambda.llvmir.IrFunction._
import llambda.compiler.ProcedureSignature
import llambda.compiler.{valuetype => vt}
import llambda.compiler.{celltype => ct}

object RuntimeFunctions {
  val allocCells = IrFunctionDecl(
    result=Result(PointerType(UserDefinedType("cell"))),
    name="_lliby_alloc_cells",
    arguments=List(
      Argument(PointerType(WorldValue.irType)),
      Argument(IntegerType(64))
    ),
    attributes=Set(NoUnwind, Cold)
  )
    
  val signalError = IrFunctionDecl(
    result=IrFunction.Result(VoidType),
    name="_lliby_signal_error",
    arguments=List(
      IrFunction.Argument(PointerType(WorldValue.irType)),
      IrFunction.Argument(PointerType(IntegerType(8)), Set(IrFunction.NoCapture)),
      IrFunction.Argument(PointerType(ct.AnyCell.irType)),
      IrFunction.Argument(PointerType(IntegerType(8)), Set(IrFunction.NoCapture)),
      IrFunction.Argument(IntegerType(32))
    ),
    attributes=Set(NoReturn, Cold)
  )
  
  val dynamicenvPush = IrFunctionDecl(
    result=Result(VoidType),
    name="_lliby_dynamicenv_push",
    arguments=List(
      Argument(PointerType(WorldValue.irType))
    )
  )

  val dynamicenvSetValue = IrFunctionDecl(
    result=Result(VoidType),
    name="_lliby_dynamicenv_set_value",
    arguments=List(
      Argument(PointerType(WorldValue.irType)),
      Argument(PointerType(ct.ProcedureCell.irType)),
      Argument(PointerType(ct.AnyCell.irType))
    ),
    attributes=Set(NoUnwind)
  )
  
  val dynamicenvPop = IrFunctionDecl(
    result=Result(VoidType),
    name="_lliby_dynamicenv_pop",
    arguments=List(
      Argument(PointerType(WorldValue.irType))
    )
  )
  
  val launchWorld = IrFunctionDecl(
    result=IrFunction.Result(VoidType),
    name="_lliby_launch_world",
    arguments=List(IrFunction.Argument(
      // void (*entryPoint)(World *)
      PointerType(FunctionType(VoidType, List(PointerType(WorldValue.irType))))
    ))
  )

  val recordDataAlloc = IrFunctionDecl(
    result=IrFunction.Result(PointerType(IntegerType(8))),
    name="_lliby_record_data_alloc",
    arguments=List(
      IrFunction.Argument(IntegerType(64))
    ),
    attributes=Set(IrFunction.NoUnwind)
  )

  val vectorElementsAlloc = IrFunctionDecl(
    result=IrFunction.Result(PointerType(PointerType(ct.AnyCell.irType))),
    name="_lliby_vector_elements_alloc",
    arguments=List(
      IrFunction.Argument(IntegerType(32))
    ),
    attributes=Set(IrFunction.NoUnwind)
  )

  val makeParameter = IrFunctionDecl(
    result=Result(PointerType(ct.ProcedureCell.irType)),
    name="_lliby_make_parameter",
    arguments=List(
      Argument(PointerType(WorldValue.irType)),
      Argument(PointerType(ct.AnyCell.irType)),
      Argument(PointerType(ct.AnyCell.irType))
    )
  )
  
  val isEqvSymbol = "_lliby_is_eqv"
  val isEqualSymbol = "_lliby_is_equal"
  
  val equivalenceProcSignature = ProcedureSignature(
    hasWorldArg=false,
    hasSelfArg=false,
    fixedArgTypes=List(vt.AnySchemeType, vt.AnySchemeType),
    restArgMemberTypeOpt=None,
    returnType=vt.ReturnType.SingleValue(vt.Predicate),
    attributes=Set()
  )
  
  val valueForParameterSignature = ProcedureSignature(
    hasWorldArg=true,
    hasSelfArg=true,
    fixedArgTypes=Nil,
    restArgMemberTypeOpt=None,
    returnType=vt.ReturnType.SingleValue(vt.AnySchemeType),
    attributes=Set()
  )
  
  val valueForParameter = IrFunctionDecl(
    result=Result(PointerType(ct.AnyCell.irType)),
    name="_lliby_value_for_parameter",
    arguments=List(
      Argument(PointerType(WorldValue.irType)),
      Argument(PointerType(ct.ProcedureCell.irType))
    ),
    attributes=Set(IrFunction.NoUnwind, IrFunction.ReadOnly)
  )

  val init = IrFunctionDecl(
    result=IrFunction.Result(VoidType),
    name="lliby_init",
    arguments=Nil,
    attributes=Set(IrFunction.NoUnwind)
  )

  def hasSideEffects(symbol : String, arity : Int) : Boolean = (symbol, arity) match {
    case ("_lliby_stdin_port", 0) =>  false
    case ("_lliby_stdout_port", 0) => false
    case ("_lliby_stderr_port", 0) => false
    case _ => true
  }
}
