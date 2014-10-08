package io.llambda.compiler.planner
import io.llambda

import llambda.compiler.ProcedureSignature
import llambda.compiler.{valuetype => vt}
import llambda.compiler.planner.{step => ps}

import org.scalatest.FunSuite

class PlanCellAllocationsSuite extends FunSuite {
  val testSignature = ProcedureSignature(
    hasWorldArg=true,
    hasSelfArg=false,
    fixedArgTypes=Nil,
    restArgMemberTypeOpt=None,
    returnType=vt.ReturnType.SingleValue(vt.UnitType),
    attributes=Set()
  )
  
  val worldPtrTemp = new ps.WorldPtrValue 

  def functionForSteps(steps : List[ps.Step]) : PlannedFunction =
    PlannedFunction(
      signature=testSignature,
      namedArguments=List(
        "world" -> worldPtrTemp
      ),
      steps=steps,
      worldPtrOpt=Some(worldPtrTemp),
      debugContextOpt=None
    )

  test("empty steps") {
    val testFunction = functionForSteps(Nil)
    
    val expectedSteps = Nil

    assert(PlanCellAllocations(testFunction).steps === expectedSteps)
  }
  
  test("single non-consuming step") {
    val nativeResult = ps.Temp(vt.Int64)
    val testSteps = List(
      ps.CreateNativeInteger(nativeResult, 25, 64)
    )

    val testFunction = functionForSteps(testSteps)
    
    val expectedSteps = List(
      ps.CreateNativeInteger(nativeResult, 25, 64)
    )

    assert(PlanCellAllocations(testFunction).steps === expectedSteps)
  }
  
  test("single consuming step") {
    val nativeResult = ps.Temp(vt.Int64)
    val boxedResult = ps.Temp(vt.ExactIntegerType)

    val testSteps = List(
      ps.CreateNativeInteger(nativeResult, 25, 64),
      ps.BoxExactInteger(boxedResult, nativeResult)
    )

    val testFunction = functionForSteps(testSteps)
    
    val expectedSteps = List(
      ps.AllocateCells(worldPtrTemp, 1),
      ps.CreateNativeInteger(nativeResult, 25, 64),
      ps.BoxExactInteger(boxedResult, nativeResult)
    )

    assert(PlanCellAllocations(testFunction).steps === expectedSteps)
  }

  test("allocations happen after dispose") {
    val disposingTemp = ps.Temp(vt.SymbolType)
    val nativeResult = ps.Temp(vt.Int64)
    val boxedResult = ps.Temp(vt.ExactIntegerType)

    val testSteps = List(
      ps.DisposeValues(Set(disposingTemp)),
      ps.CreateNativeInteger(nativeResult, 25, 64),
      ps.BoxExactInteger(boxedResult, nativeResult)
    )

    val testFunction = functionForSteps(testSteps)
    
    val expectedSteps = List(
      ps.DisposeValues(Set(disposingTemp)),
      ps.AllocateCells(worldPtrTemp, 1),
      ps.CreateNativeInteger(nativeResult, 25, 64),
      ps.BoxExactInteger(boxedResult, nativeResult)
    )

    assert(PlanCellAllocations(testFunction).steps === expectedSteps)
  }
  
  test("multiple consuming steps are merged") {
    val nativeResult = ps.Temp(vt.Int64)
    val boxedResult = ps.Temp(vt.ExactIntegerType)

    val testSteps = List(
      ps.CreateNativeInteger(nativeResult, 25, 64),
      ps.BoxExactInteger(boxedResult, nativeResult),
      ps.BoxExactInteger(boxedResult, nativeResult),
      ps.BoxExactInteger(boxedResult, nativeResult)
    )

    val testFunction = functionForSteps(testSteps)
    
    val expectedSteps = List(
      ps.AllocateCells(worldPtrTemp, 3),
      ps.CreateNativeInteger(nativeResult, 25, 64),
      ps.BoxExactInteger(boxedResult, nativeResult),
      ps.BoxExactInteger(boxedResult, nativeResult),
      ps.BoxExactInteger(boxedResult, nativeResult)
    )

    assert(PlanCellAllocations(testFunction).steps === expectedSteps)
  }

  test("cell allocations can not be merged across GC barrier") {
    val nativeResult = ps.Temp(vt.Int64)
    val boxedResult = ps.Temp(vt.ExactIntegerType)

    val testSteps = List(
      ps.CreateNativeInteger(nativeResult, 25, 64),
      ps.BoxExactInteger(boxedResult, nativeResult),
      ps.BoxExactInteger(boxedResult, nativeResult),
      ps.PushDynamicState(worldPtrTemp, Nil),
      ps.BoxExactInteger(boxedResult, nativeResult)
    )

    val testFunction = functionForSteps(testSteps)
    
    val expectedSteps = List(
      ps.AllocateCells(worldPtrTemp, 2),
      ps.CreateNativeInteger(nativeResult, 25, 64),
      ps.BoxExactInteger(boxedResult, nativeResult),
      ps.BoxExactInteger(boxedResult, nativeResult),
      ps.PushDynamicState(worldPtrTemp, Nil),
      ps.AllocateCells(worldPtrTemp, 1),
      ps.BoxExactInteger(boxedResult, nativeResult)
    )

    assert(PlanCellAllocations(testFunction).steps === expectedSteps)
  }
  
  test("cell allocations are not affected by empty branch") {
    val nativeResult = ps.Temp(vt.Int64)
    val boxedResult = ps.Temp(vt.ExactIntegerType)
    val condResult = ps.Temp(vt.Int64)

    val testSteps = List(
      ps.CreateNativeInteger(nativeResult, 25, 64),
      ps.BoxExactInteger(boxedResult, nativeResult),
      ps.BoxExactInteger(boxedResult, nativeResult),
      ps.CondBranch(condResult, boxedResult, Nil, nativeResult, Nil, nativeResult),
      ps.BoxExactInteger(boxedResult, nativeResult)
    )

    val testFunction = functionForSteps(testSteps)
    
    val expectedSteps = List(
      ps.AllocateCells(worldPtrTemp, 3),
      ps.CreateNativeInteger(nativeResult, 25, 64),
      ps.BoxExactInteger(boxedResult, nativeResult),
      ps.BoxExactInteger(boxedResult, nativeResult),
      ps.CondBranch(condResult, boxedResult, Nil, nativeResult, Nil, nativeResult),
      ps.BoxExactInteger(boxedResult, nativeResult)
    )

    assert(PlanCellAllocations(testFunction).steps === expectedSteps)
  }
 
  test("cell allocations with allocating branch") {
    val nativeResult = ps.Temp(vt.Int64)
    val boxedResult = ps.Temp(vt.ExactIntegerType)
    val condResult = ps.Temp(vt.Int64)

    val trueSteps = List(
      ps.BoxExactInteger(boxedResult, nativeResult),
      ps.BoxExactInteger(boxedResult, nativeResult)
    )

    val falseSteps = List(
      ps.BoxExactInteger(boxedResult, nativeResult)
    )

    val testSteps = List(
      ps.CreateNativeInteger(nativeResult, 25, 64),
      ps.BoxExactInteger(boxedResult, nativeResult),
      ps.BoxExactInteger(boxedResult, nativeResult),
      ps.CondBranch(condResult, boxedResult, trueSteps, nativeResult, falseSteps, nativeResult),
      ps.BoxExactInteger(boxedResult, nativeResult)
    )

    val testFunction = functionForSteps(testSteps)
    
    val expectedTrueSteps = List(
      ps.AllocateCells(worldPtrTemp, 2),
      ps.BoxExactInteger(boxedResult, nativeResult),
      ps.BoxExactInteger(boxedResult, nativeResult)
    )

    val expectedFalseSteps = List(
      ps.AllocateCells(worldPtrTemp, 1),
      ps.BoxExactInteger(boxedResult, nativeResult)
    )
    
    val expectedSteps = List(
      ps.AllocateCells(worldPtrTemp, 2),
      ps.CreateNativeInteger(nativeResult, 25, 64),
      ps.BoxExactInteger(boxedResult, nativeResult),
      ps.BoxExactInteger(boxedResult, nativeResult),
      ps.CondBranch(condResult, boxedResult, expectedTrueSteps, nativeResult, expectedFalseSteps, nativeResult),
      ps.AllocateCells(worldPtrTemp, 1),
      ps.BoxExactInteger(boxedResult, nativeResult)
    )

    assert(PlanCellAllocations(testFunction).steps === expectedSteps)
  }
}
