;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; This file is generated by typegen. Do not edit manually. ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

; {supertype, typeId, gcState}
%datum = type {i8, i8}
!0 = metadata !{ metadata !"Datum::typeId" }
!1 = metadata !{ metadata !"Datum::gcState" }

; {supertype}
%unspecific = type {%datum}
!2 = metadata !{ metadata !"Datum::typeId->Unspecific", metadata !0 }
!3 = metadata !{ metadata !"Datum::gcState->Unspecific", metadata !1 }

; {supertype}
%listElement = type {%datum}
!4 = metadata !{ metadata !"Datum::typeId->ListElement", metadata !0 }
!5 = metadata !{ metadata !"Datum::gcState->ListElement", metadata !1 }

; {supertype, car, cdr}
%pair = type {%listElement, %datum*, %datum*}
!8 = metadata !{ metadata !"Pair::car" }
!9 = metadata !{ metadata !"Pair::cdr" }
!6 = metadata !{ metadata !"Datum::typeId->ListElement->Pair", metadata !4 }
!7 = metadata !{ metadata !"Datum::gcState->ListElement->Pair", metadata !5 }

; {supertype}
%emptyList = type {%listElement}
!10 = metadata !{ metadata !"Datum::typeId->ListElement->EmptyList", metadata !4 }
!11 = metadata !{ metadata !"Datum::gcState->ListElement->EmptyList", metadata !5 }

; {supertype, unsigned charLength, unsigned byteLength, utf8Data}
%stringLike = type {%datum, i32, i32, i8*}
!13 = metadata !{ metadata !"Datum::gcState->StringLike", metadata !1 }
!12 = metadata !{ metadata !"Datum::typeId->StringLike", metadata !0 }
!15 = metadata !{ metadata !"StringLike::byteLength" }
!14 = metadata !{ metadata !"StringLike::charLength" }
!16 = metadata !{ metadata !"StringLike::utf8Data" }

; {supertype}
%string = type {%stringLike}
!17 = metadata !{ metadata !"Datum::gcState->StringLike->String", metadata !13 }
!18 = metadata !{ metadata !"Datum::typeId->StringLike->String", metadata !12 }
!19 = metadata !{ metadata !"StringLike::byteLength->String", metadata !15 }
!20 = metadata !{ metadata !"StringLike::charLength->String", metadata !14 }
!21 = metadata !{ metadata !"StringLike::utf8Data->String", metadata !16 }

; {supertype}
%symbol = type {%stringLike}
!22 = metadata !{ metadata !"Datum::gcState->StringLike->Symbol", metadata !13 }
!23 = metadata !{ metadata !"Datum::typeId->StringLike->Symbol", metadata !12 }
!24 = metadata !{ metadata !"StringLike::byteLength->Symbol", metadata !15 }
!25 = metadata !{ metadata !"StringLike::charLength->Symbol", metadata !14 }
!26 = metadata !{ metadata !"StringLike::utf8Data->Symbol", metadata !16 }

; {supertype, bool value}
%boolean = type {%datum, i8}
!29 = metadata !{ metadata !"Boolean::value" }
!27 = metadata !{ metadata !"Datum::typeId->Boolean", metadata !0 }
!28 = metadata !{ metadata !"Datum::gcState->Boolean", metadata !1 }

; {supertype}
%numeric = type {%datum}
!30 = metadata !{ metadata !"Datum::typeId->Numeric", metadata !0 }
!31 = metadata !{ metadata !"Datum::gcState->Numeric", metadata !1 }

; {supertype, signed value}
%exactInteger = type {%numeric, i64}
!34 = metadata !{ metadata !"ExactInteger::value" }
!32 = metadata !{ metadata !"Datum::typeId->Numeric->ExactInteger", metadata !30 }
!33 = metadata !{ metadata !"Datum::gcState->Numeric->ExactInteger", metadata !31 }

; {supertype, value}
%inexactRational = type {%numeric, double}
!37 = metadata !{ metadata !"InexactRational::value" }
!35 = metadata !{ metadata !"Datum::typeId->Numeric->InexactRational", metadata !30 }
!36 = metadata !{ metadata !"Datum::gcState->Numeric->InexactRational", metadata !31 }

; {supertype, unicodeChar}
%character = type {%datum, i32}
!40 = metadata !{ metadata !"Character::unicodeChar" }
!38 = metadata !{ metadata !"Datum::typeId->Character", metadata !0 }
!39 = metadata !{ metadata !"Datum::gcState->Character", metadata !1 }

; {supertype, unsigned length, elements}
%vector = type {%datum, i32, %datum**}
!43 = metadata !{ metadata !"Vector::length" }
!44 = metadata !{ metadata !"Vector::elements" }
!41 = metadata !{ metadata !"Datum::typeId->Vector", metadata !0 }
!42 = metadata !{ metadata !"Datum::gcState->Vector", metadata !1 }

; {supertype, unsigned length, data}
%bytevector = type {%datum, i32, i8*}
!47 = metadata !{ metadata !"Bytevector::length" }
!48 = metadata !{ metadata !"Bytevector::data" }
!45 = metadata !{ metadata !"Datum::typeId->Bytevector", metadata !0 }
!46 = metadata !{ metadata !"Datum::gcState->Bytevector", metadata !1 }

; {supertype, unsigned recordClassId, recordData}
%recordLike = type {%datum, i32, i8*}
!51 = metadata !{ metadata !"RecordLike::recordClassId" }
!52 = metadata !{ metadata !"RecordLike::recordData" }
!49 = metadata !{ metadata !"Datum::typeId->RecordLike", metadata !0 }
!50 = metadata !{ metadata !"Datum::gcState->RecordLike", metadata !1 }

; {supertype, entryPoint}
%procedure = type {%recordLike, %datum* (%procedure*, %listElement*) *}
!56 = metadata !{ metadata !"Datum::gcState->RecordLike->Procedure", metadata !50 }
!55 = metadata !{ metadata !"Datum::typeId->RecordLike->Procedure", metadata !49 }
!53 = metadata !{ metadata !"RecordLike::recordClassId->Procedure", metadata !51 }
!57 = metadata !{ metadata !"Procedure::entryPoint" }
!54 = metadata !{ metadata !"RecordLike::recordData->Procedure", metadata !52 }

; {supertype, extraData}
%record = type {%recordLike, i8*}
!61 = metadata !{ metadata !"Datum::gcState->RecordLike->Record", metadata !50 }
!60 = metadata !{ metadata !"Datum::typeId->RecordLike->Record", metadata !49 }
!58 = metadata !{ metadata !"RecordLike::recordClassId->Record", metadata !51 }
!62 = metadata !{ metadata !"Record::extraData" }
!59 = metadata !{ metadata !"RecordLike::recordData->Record", metadata !52 }
