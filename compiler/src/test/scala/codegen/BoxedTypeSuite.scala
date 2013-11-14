package llambda.codegen

import llambda.{boxedtype => bt}
import llambda.InternalCompilerErrorException

import org.scalatest.FunSuite

class BoxedTypeSuite extends llvmir.IrTestSuite {
  test("create constant boxed inexact rational") {
    val innerValue = llvmir.DoubleConstant(31.5)

    val constant = bt.BoxedInexactRational.createConstant(innerValue)
    val expectedTypeId = bt.BoxedInexactRational.typeId.toString

    assert(constant.toIr === 
      s"{%numeric {%datum {i16 ${expectedTypeId}, i16 0}}, double 31.5}"
    )
  }
  
  test("create constant with incorrect type") {
    val innerValue = llvmir.StringConstant("Hello, world!")
    
    intercept[InternalCompilerErrorException] {
      bt.BoxedInexactRational.createConstant(innerValue)
    }
  }

  test("isTypeOrSubtypeOf") {
    assert(bt.BoxedInexactRational.isTypeOrSubtypeOf(bt.BoxedInexactRational) === true)
    assert(bt.BoxedInexactRational.isTypeOrSubtypeOf(bt.BoxedNumeric) === true)
    assert(bt.BoxedInexactRational.isTypeOrSubtypeOf(bt.BoxedDatum) === true)
    
    assert(bt.BoxedInexactRational.isTypeOrSubtypeOf(bt.BoxedString) === false)
    assert(bt.BoxedNumeric.isTypeOrSubtypeOf(bt.BoxedInexactRational) === false)
    assert(bt.BoxedDatum.isTypeOrSubtypeOf(bt.BoxedInexactRational) === false)
  }
  
  test("isTypeOrSupertypeOf") {
    assert(bt.BoxedInexactRational.isTypeOrSupertypeOf(bt.BoxedInexactRational) === true)
    assert(bt.BoxedNumeric.isTypeOrSupertypeOf(bt.BoxedInexactRational) === true)
    assert(bt.BoxedDatum.isTypeOrSupertypeOf(bt.BoxedInexactRational) === true)
    
    assert(bt.BoxedString.isTypeOrSupertypeOf(bt.BoxedInexactRational) === false)
    assert(bt.BoxedInexactRational.isTypeOrSupertypeOf(bt.BoxedNumeric) === false)
    assert(bt.BoxedInexactRational.isTypeOrSupertypeOf(bt.BoxedDatum) === false)
  }

  test("concreteTypes") {
    assert(bt.BoxedListElement.concreteTypes === Set(bt.BoxedPair, bt.BoxedEmptyList))
    assert(bt.BoxedNumeric.concreteTypes === Set(bt.BoxedExactInteger, bt.BoxedInexactRational))
    assert(bt.BoxedString.concreteTypes === Set(bt.BoxedString))
  }
  
  test("noop bitcast") {
    val nullNumeric = llvmir.NullPointerConstant(llvmir.PointerType(bt.BoxedNumeric.irType))

    val block = createTestBlock()
    val resultValue = bt.BoxedNumeric.genPointerBitcast(block)(nullNumeric)

    assert(resultValue === nullNumeric)
  }
  
  test("simple bitcast") {
    val nullNumeric = llvmir.NullPointerConstant(llvmir.PointerType(bt.BoxedNumeric.irType))

    val block = createTestBlock()
    val resultValue = bt.BoxedDatum.genPointerBitcast(block)(nullNumeric)

    assertInstr(block, "%datumCast1 = bitcast %numeric* null to %datum*") 

    assert(resultValue != nullNumeric)
    assert(resultValue.irType === llvmir.PointerType(bt.BoxedDatum.irType))
  }
}
