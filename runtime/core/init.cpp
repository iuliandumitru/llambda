#include <clocale>

#include "core/init.h"
#include "dynamic/init.h"

#include "alloc/allocator.h"
#include "alloc/DynamicMemoryBlock.h"

extern "C"
{
using namespace lliby;

void lliby_init()
{
	alloc::DynamicMemoryBlock::init();

	// Use the user preferred locale
	// We assume a UTF-8 locale but don't explicitly set "UTF-8" so we still
	// get user-defined string sorting etc.
	std::setlocale(LC_ALL, "");
	dynamic::init();

	// Start the allocator
	alloc::initGlobal();
}

}
