package io.llambda.typegen.writer

import io.llambda.typegen._

object WriteTypeid {
  def apply(processedTypes : ProcessedTypes) : Map[String, String] = {
    // Get information about our type tag
    val typeTagField = processedTypes.rootCellClass.typeTagField
    val typeTagFieldType = typeTagField.fieldType

    val typeTagAlias = typeTagFieldType match {
      case alias : FieldTypeAlias => 
        alias

      case _ =>
        // CheckTypeTagField should have caught this
        throw new Exception
    }

    val typeTagCppName = typeTagAlias.cppTypeName.get
    val typeTagSupertype = FieldTypeToCpp(typeTagAlias.aliasedType, None)

    val incBuilder = new CppIncludeBuilder("_LLIBY_BINDING_TYPEID_H")
    
    incBuilder.appendRaw(GeneratedFileComment)

    // Our C++ type names assume this is included
    incBuilder += "#include <cstdint>"
    incBuilder.sep()

    incBuilder += "namespace lliby"
    incBuilder += "{"
    incBuilder.sep()

    incBuilder += s"enum class ${typeTagCppName} : ${typeTagSupertype}"
    incBuilder += "{"
    incBuilder.indented {
      for(cellClass <- processedTypes.cellClasses.values; typeId <- cellClass.typeId) {
        incBuilder += s"${cellClass.name} = ${typeId},"
      }
    }
    incBuilder += "};"

    incBuilder.sep()
    incBuilder += "}"
    incBuilder.sep()

    Map("runtime/binding/generated/typeid.h" -> incBuilder.toString)
  }
}

