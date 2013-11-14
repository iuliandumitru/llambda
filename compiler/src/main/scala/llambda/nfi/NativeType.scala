package llambda.nfi

import llambda.{boxedtype => bt}

sealed abstract class NativeType

sealed abstract class UnboxedType extends NativeType

sealed abstract class IntLikeType(val bits : Int, val signed : Boolean) extends UnboxedType

sealed abstract class CBool extends IntLikeType(8, false)
case object CStrictBool extends CBool
case object CTruthyBool extends CBool

sealed abstract class IntType(bits : Int, signed : Boolean) extends IntLikeType(bits, signed)

case object Int8 extends IntType(8, true)
case object Int16 extends IntType(16, true)
case object Int32 extends IntType(32, true)
case object Int64 extends IntType(64, true)

case object UInt8 extends IntType(8, false)
case object UInt16 extends IntType(16, false)
case object UInt32 extends IntType(32, false)
// UInt64 is outside the range we can represent

sealed abstract class FpType extends UnboxedType

case object Float extends FpType
case object Double extends FpType 

case object Utf8CString extends UnboxedType

case object UnicodeChar extends IntLikeType(32, true)

case class BoxedValue(boxedType : bt.BoxedType) extends NativeType
