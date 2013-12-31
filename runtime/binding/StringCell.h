#ifndef _LLIBY_BINDING_STRINGCELL_H
#define _LLIBY_BINDING_STRINGCELL_H

#include "DatumCell.h"
#include <list>

#include "unicode/UnicodeChar.h"

namespace lliby
{

class StringCell : public DatumCell
{
#include "generated/StringCellMembers.h"
public:
	static StringCell* fromUtf8CString(const char *str);
	static StringCell* fromUtf8Data(const std::uint8_t *data, std::uint32_t byteLength);
	static StringCell* fromFill(std::uint32_t length, UnicodeChar fill);
	static StringCell* fromAppended(const std::list<const StringCell*> &strings);
	static StringCell* fromUnicodeChars(const std::list<UnicodeChar> &unicodeChars);
	static StringCell* fromSymbol(const SymbolCell *symbol);

	StringCell* copy(std::int64_t start = 0, std::int64_t end = -1) const; 

	UnicodeChar charAt(std::uint32_t offset) const;
	bool setCharAt(std::uint32_t offset, UnicodeChar unicodeChar);

	bool fill(UnicodeChar unicodeChar, std::int64_t start = 0, std::int64_t end = -1);
	bool replace(std::uint32_t offset, const StringCell *from, std::int64_t fromStart = 0, std::int64_t fromEnd = -1);

	std::list<UnicodeChar> unicodeChars(std::int64_t start = 0, std::int64_t end = -1) const;
	
	bool operator==(const StringCell &other) const;
	
	bool operator!=(const StringCell &other) const
	{
		return !(*this == other);
	}

	// Returns and integer less than, equal to or greater than zero if the string
	// less than, equal to or greater than the other string
	int compare(const StringCell *other, CaseSensitivity cs = CaseSensitivity::Sensitive) const;

	bool asciiOnly() const
	{
		return byteLength() == charLength();
	}

	SymbolCell *toSymbol() const;
	BytevectorCell *toUtf8Bytevector(std::int64_t start = 0, std::int64_t end = -1) const;
	
	StringCell *toUppercaseString() const;
	StringCell *toLowercaseString() const;
	StringCell *toCaseFoldedString() const;
	
	std::uint8_t* utf8Data() const;

protected:
	StringCell(std::uint32_t byteLength, std::uint32_t charLength, std::int16_t allocSlackBytes) :
		DatumCell(CellTypeId::String),
		m_allocSlackBytes(allocSlackBytes),
		m_charLength(charLength),
		m_byteLength(byteLength)
	{
	}
	
	static const std::uint32_t InlineDataSize = 12;

	// Creates an uninitialized cell with the given size
	static StringCell* createUninitialized(std::uint32_t byteLength);

	std::uint8_t *charPointer(std::uint8_t *scanFrom, std::uint32_t bytesLeft, uint32_t charOffset) const;
	std::uint8_t *charPointer(std::uint32_t charOffset) const;

	struct CharRange
	{
		std::uint8_t *startPointer;
		std::uint8_t *endPointer;
		std::uint32_t charCount;

		bool isNull() const
		{
			return startPointer == nullptr;
		};

		unsigned int byteCount() const
		{
			return endPointer - startPointer;
		}
	};

	CharRange charRange(std::int64_t start, std::int64_t end = -1) const; 
	bool replaceBytes(const CharRange &range, std::uint8_t *pattern, unsigned int patternBytes, unsigned int count = 1);
	
	int compareCaseSensitive(const StringCell *other) const;
	int compareCaseInsensitive(const StringCell *other) const;

	StringCell *toConvertedString(UnicodeChar (UnicodeChar::* converter)() const) const;
	
	static size_t inlineDataSize();
	bool dataIsInline() const;
	
	void setByteLength(std::uint32_t newByteLength)
	{
		m_byteLength = newByteLength;
	}
	
	void setCharLength(std::uint32_t newCharLength)
	{
		m_charLength = newCharLength;
	}
	
	void setAllocSlackBytes(std::uint16_t newAllocSlackBytes)
	{
		m_allocSlackBytes = newAllocSlackBytes;
	}
};

class HeapStringCell : public StringCell
{
	friend class StringCell;
#include "generated/HeapStringCellMembers.h"
private:
	HeapStringCell(std::uint8_t *heapData, std::uint32_t byteLength, std::uint32_t charLength, std::uint16_t allocSlackBytes) :
		StringCell(byteLength, charLength, allocSlackBytes),
		m_heapData(heapData)
	{
	}
	
	void setHeapData(std::uint8_t* newHeapData)
	{
		m_heapData = newHeapData;
	}
};

class InlineStringCell : public StringCell
{
	friend class StringCell;
#include "generated/InlineStringCellMembers.h"
private:
	InlineStringCell(std::uint32_t byteLength, std::uint32_t charLength) :
		StringCell(byteLength, charLength, InlineDataSize - byteLength - 1)
	{
	}
};

}

#endif

