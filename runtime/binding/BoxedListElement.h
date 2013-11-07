#ifndef _LLIBY_BINDING_BOXEDLISTELEMENT_H
#define _LLIBY_BINDING_BOXEDLISTELEMENT_H

#include "BoxedDatum.h"

#include <list>

namespace lliby
{

class BoxedListElement : public BoxedDatum
{
#include "generated/BoxedListElementMembers.h"
public:
	static BoxedListElement *createProperList(const std::list<BoxedDatum*> &elements);
	static BoxedDatum *createList(const std::list<BoxedDatum*> &elements, BoxedDatum *tail);

protected:
	explicit BoxedListElement(BoxedTypeId typeId) :
		BoxedDatum(typeId)
	{
	}

	BoxedListElement(BoxedTypeId typeId, GarbageState gcState) :
		BoxedDatum(typeId, gcState)
	{
	}
};

}

#endif
