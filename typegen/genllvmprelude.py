from typegen.exceptions import SemanticException
from typegen.constants import BASE_TYPE

def _complex_type_to_llvm(complex_type):
    if complex_type == "bool":
        return "i8"
    elif complex_type == "entryPoint":
        return "%" + BASE_TYPE + "* (%closure*, %" + BASE_TYPE + "*)*"
    elif complex_type == "unicodeChar":
        return "i32"
    else:
        raise SemanticException('Unknown complex type "' + complex_type + '"')

def generate_llvm_prelude(boxed_types):
    output  = ";;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;\n"
    output += ";; This file is generated by gen-types.py. Do not edit manually. ;;\n"
    output += ";;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;\n\n"

    for type_name, boxed_type in boxed_types.items():
        field_comments = []
        field_irs = []

        if not boxed_type.inherits is None:
            field_comments.append("supertype")
            field_irs.append("%" + boxed_type.inherits)

        for field_name, field in boxed_type.fields.items():
            # LLVM types don't include signedness
            if field.signed is True:
                description = "signed "
            elif field.signed is False:
                description = "unsigned "
            elif field.complex_type == "bool":
                description = "bool "
            else:
                description = ""

            # Add our comment for the field
            description += field_name
            field_comments.append(description)

            # And the actual LLVM type
            if field.llvm_type:
                field_irs.append(field.llvm_type)
            else:
                field_irs.append(_complex_type_to_llvm(field.complex_type))

        # Join everything together
        output += "; {" + ', '.join(field_comments) + '}\n'

        output += "%" + type_name + ' = type {'
        output += ', '.join(field_irs)
        output += '}\n\n'

    # Remove the extra \n
    return {"compiler/src/main/resources/generated/boxedTypes.ll": output[:-1]}
