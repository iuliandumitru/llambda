/************************************************************
 * This file is generated by typegen. Do not edit manually. *
 ************************************************************/

#include "binding/UnspecificCell.h"
#include "binding/PairCell.h"
#include "binding/EmptyListCell.h"
#include "binding/StringCell.h"
#include "binding/SymbolCell.h"
#include "binding/BooleanCell.h"
#include "binding/ExactIntegerCell.h"
#include "binding/InexactRationalCell.h"
#include "binding/CharacterCell.h"
#include "binding/VectorCell.h"
#include "binding/BytevectorCell.h"
#include "binding/ProcedureCell.h"
#include "binding/RecordCell.h"

static_assert(sizeof(lliby::UnspecificCell) <= sizeof(lliby::alloc::Cell), "UnspecificCell does not fit in to a cell");
static_assert(sizeof(lliby::PairCell) <= sizeof(lliby::alloc::Cell), "PairCell does not fit in to a cell");
static_assert(sizeof(lliby::EmptyListCell) <= sizeof(lliby::alloc::Cell), "EmptyListCell does not fit in to a cell");
static_assert(sizeof(lliby::StringCell) <= sizeof(lliby::alloc::Cell), "StringCell does not fit in to a cell");
static_assert(sizeof(lliby::InlineStringCell) <= sizeof(lliby::alloc::Cell), "InlineStringCell does not fit in to a cell");
static_assert(sizeof(lliby::HeapStringCell) <= sizeof(lliby::alloc::Cell), "HeapStringCell does not fit in to a cell");
static_assert(sizeof(lliby::SymbolCell) <= sizeof(lliby::alloc::Cell), "SymbolCell does not fit in to a cell");
static_assert(sizeof(lliby::InlineSymbolCell) <= sizeof(lliby::alloc::Cell), "InlineSymbolCell does not fit in to a cell");
static_assert(sizeof(lliby::HeapSymbolCell) <= sizeof(lliby::alloc::Cell), "HeapSymbolCell does not fit in to a cell");
static_assert(sizeof(lliby::BooleanCell) <= sizeof(lliby::alloc::Cell), "BooleanCell does not fit in to a cell");
static_assert(sizeof(lliby::ExactIntegerCell) <= sizeof(lliby::alloc::Cell), "ExactIntegerCell does not fit in to a cell");
static_assert(sizeof(lliby::InexactRationalCell) <= sizeof(lliby::alloc::Cell), "InexactRationalCell does not fit in to a cell");
static_assert(sizeof(lliby::CharacterCell) <= sizeof(lliby::alloc::Cell), "CharacterCell does not fit in to a cell");
static_assert(sizeof(lliby::VectorCell) <= sizeof(lliby::alloc::Cell), "VectorCell does not fit in to a cell");
static_assert(sizeof(lliby::BytevectorCell) <= sizeof(lliby::alloc::Cell), "BytevectorCell does not fit in to a cell");
static_assert(sizeof(lliby::ProcedureCell) <= sizeof(lliby::alloc::Cell), "ProcedureCell does not fit in to a cell");
static_assert(sizeof(lliby::RecordCell) <= sizeof(lliby::alloc::Cell), "RecordCell does not fit in to a cell");
