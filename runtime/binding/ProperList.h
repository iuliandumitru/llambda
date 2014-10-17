#ifndef _LLIBY_BINDING_PROPERLIST_H
#define _LLIBY_BINDING_PROPERLIST_H

#include <iterator>
#include <cassert>

#include "ListElementCell.h"
#include "PairCell.h"
#include "EmptyListCell.h"
#include "RestArgument.h"

namespace lliby
{

template<class T>
class ProperList
{
public:
	typedef std::uint32_t size_type;

	class Iterator : public std::iterator<std::forward_iterator_tag, T*>
	{
		friend class ProperList;
	public:
		T* operator*() const
		{
			// ProperList verifies all the cars are of type T in its constructor
			auto pairHead = cell_unchecked_cast<const PairCell>(m_head);
			return cell_unchecked_cast<T>(pairHead->car());
		}

		bool operator==(const Iterator &other) const
		{
			return m_head == other.m_head;
		}
		
		bool operator!=(const Iterator &other) const
		{
			return m_head != other.m_head;
		}

		Iterator& operator++()
		{
			auto pairHead = cell_unchecked_cast<const PairCell>(m_head);
			m_head = cell_unchecked_cast<const ListElementCell>(pairHead->cdr());

			return *this;
		}
		
		Iterator operator++(int postfix)
		{
			Iterator originalValue(*this);
			++(*this);
			return originalValue;
		}

	private:
		explicit Iterator(const ListElementCell *head) :
			m_head(head)
		{
		}
		
		const ListElementCell *m_head;
	};
	
	
	explicit ProperList(const RestArgument<T> *head) :
		m_head(head),
		m_valid(true),
		m_length(0)
	{
		// This list has already been verified by Scheme; we just need to find its length
		const AnyCell *cell = head;
			
		while(auto pair = cell_cast<PairCell>(cell))
		{
			if (pair->listLength() != 0)
			{
				// We have a list length hint
				m_length += pair->listLength();
				break;
			}

			// No length hint, keep checking 
			cell = pair->cdr();
			m_length++;
		}
	}

	explicit ProperList(const ListElementCell *head) :
		m_head(EmptyListCell::instance()),
		m_valid(false),
		m_length(0)
	{
		// Manually verify the list
		const AnyCell *cell = head;
		size_type length = 0;
			
		while(auto pair = cell_cast<PairCell>(cell))
		{
			length++;
			
			if (cell_cast<T>(pair->car()) == nullptr)
			{
				// Wrong element type
				return;
			}

			cell = pair->cdr();
		}

		if (cell != EmptyListCell::instance())
		{
			// Not a proper list
			return;
		}

		m_head = head;
		m_valid = true;
		m_length = length;
	}

	bool isValid() const
	{
		return m_valid;
	}

	bool isEmpty() const
	{
		return m_length == 0;
	}

	size_type length() const
	{
		return m_length;
	}

	Iterator begin() const
	{
		return Iterator(m_head);
	}

	Iterator end() const
	{
		return Iterator(EmptyListCell::instance());
	}

private:
	const ListElementCell *m_head;
	bool m_valid;
	size_type m_length;
};


}

#endif
