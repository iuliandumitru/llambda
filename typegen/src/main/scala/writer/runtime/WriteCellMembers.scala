package io.llambda.typegen.writer.runtime

import io.llambda.typegen._

object WriteCellMembers extends writer.OutputWriter {
  private def cppTypePredicate(processedTypes : ProcessedTypes, cellClass : CellClass) : String = cellClass match {
    case _ : RootCellClass =>
      // We're always an instance of the root classes
      "true"

    case _ if cellClass.instanceType == CellClass.Abstract =>
      // Our type check is the union of the type checks for our child classes
      val allChildren = processedTypes.taggedCellClassesByParent(cellClass)

      val childTypeChecks = allChildren map { childCellClass =>
        "(" + cppTypePredicate(processedTypes, childCellClass) + ")"
      }

      childTypeChecks.mkString(" || ")

    case _ =>
      val typeTagField = processedTypes.rootCellClass.typeTagField
      val typeTagEnumName = FieldTypeToCpp(typeTagField.fieldType, None)

      s"datum->${typeTagField.name}() == ${typeTagEnumName}::${cellClass.name}" 
  }


  private def writeMemberFile(processedTypes : ProcessedTypes, cellClass : CellClass) : String = {
    val cppName = cellClass.names.cppClassName
    val rootCellCppName = processedTypes.rootCellClass.names.cppClassName

    val cppBuilder = new CppBuilder

    cppBuilder.appendRaw(writer.GeneratedClikeFileComment)

    // Make accessors for each field
    if (!cellClass.fields.isEmpty) {
      cppBuilder += "public:"
      cppBuilder.indented {
        for(field <- cellClass.fields) {
          val cppReturnType = FieldTypeToCpp(field.fieldType, None, true)
          // Are we returning a pointer to a member variable?
          val returningPointerToMemeber = field.fieldType.isInstanceOf[ArrayFieldType]

          if (returningPointerToMemeber) {
            // Build const version returning const pointer
            cppBuilder += s"const ${cppReturnType} ${field.name}() const"
            cppBuilder.blockSep {
              cppBuilder += s"return m_${field.name};"
            }
            
            // And non-const version
            cppBuilder += s"${cppReturnType} ${field.name}()"
            cppBuilder.blockSep {
              cppBuilder += s"return m_${field.name};"
            }
          }
          else {
            // We're returning by value; this can be const
            cppBuilder += s"${cppReturnType} ${field.name}() const"
            cppBuilder.blockSep {
              cppBuilder += s"return m_${field.name};"
            }
          }
        }
      }
    }

    cellClass match {
      case _ : VariantCellClass =>
        // Variants can only be distinguished in a variant-specific way at runtime

      case _ =>
        cppBuilder += "public:"
        cppBuilder.indented {
          // Make our type check
          cppBuilder += s"static bool isInstance(const ${rootCellCppName} *datum)"
          cppBuilder.blockSep {
            cppBuilder += "return " + cppTypePredicate(processedTypes, cellClass) + ";"
          }
      
          if (cellClass != processedTypes.rootCellClass) {
            // Make our type casters 
            for(constPrefix <- List("", "const ")) {
              cppBuilder += s"static ${constPrefix}${cppName}* fromDatum(${constPrefix}${rootCellCppName} *datum)"
              cppBuilder.blockSep {
                cppBuilder += "if (isInstance(datum))"
                cppBuilder.blockSep {
                  cppBuilder += s"return static_cast<${constPrefix}${cppName}*>(datum);"
                }
              
                cppBuilder += s"return nullptr;"
              }
            }
          }
        }
    }

    if (!cellClass.fields.isEmpty) {
      // Define each field member variable
      cppBuilder += "private:"
      cppBuilder.indented {
        for(field <- cellClass.fields) {
          cppBuilder += FieldTypeToCpp(field.fieldType, Some("m_" + field.name)) + ";"
        }
      }
    }

    cppBuilder.toString
  }

  def apply(processedTypes : ProcessedTypes) : Map[String, String] = {
    (processedTypes.cellClasses.values.map { case cellClass =>
      val cppName = cellClass.names.cppClassName
      val outputPath = s"runtime/binding/generated/${cppName}Members.h"

      (outputPath -> writeMemberFile(processedTypes, cellClass))
    }).toMap
  }
}
