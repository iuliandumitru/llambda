/************************************************************
 * This file is generated by typegen. Do not edit manually. *
 ************************************************************/

public:
	AbstractPort* port() const
	{
		return m_port;
	}

public:
	static bool typeIdIsTypeOrSubtype(CellTypeId typeId)
	{
		return typeId == CellTypeId::Port;
	}

	static bool isInstance(const AnyCell *cell)
	{
		return typeIdIsTypeOrSubtype(cell->typeId());
	}

private:
	AbstractPort* m_port;
