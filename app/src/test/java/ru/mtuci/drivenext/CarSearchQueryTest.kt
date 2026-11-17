package ru.mtuci.drivenext

import java.net.URLDecoder
import org.junit.Assert.*
import org.junit.Test
import ru.mtuci.drivenext.data.CarSearchQuery

class CarSearchQueryTest {
    private fun expression(query: String) = URLDecoder.decode(CarSearchQuery.filter(query).removePrefix("&or="), "UTF-8")

    @Test fun emptyQueryDoesNotRestrictCatalog() {
        assertEquals("", CarSearchQuery.filter("   "))
    }

    @Test fun searchesBrandAndModelWithTrimmedText() {
        assertEquals("(brand.ilike.\"%S 500%\",model.ilike.\"%S 500%\")", expression(" S 500 "))
    }

    @Test fun preservesUnicodeAndPlusWithoutBreakingUrlParameters() {
        val filter = CarSearchQuery.filter("Лада +")
        assertFalse(filter.contains(" "))
        assertTrue(filter.contains("%2B"))
        assertEquals("(brand.ilike.\"%Лада +%\",model.ilike.\"%Лада +%\")", expression("Лада +"))
    }

    @Test fun quotesReservedCharactersAndEscapesQuotesAndBackslashes() {
        assertEquals("(brand.ilike.\"%A,B(C):D%\",model.ilike.\"%A,B(C):D%\")", expression("A,B(C):D"))
        assertEquals("(brand.ilike.\"%A\\\"B\\\\C%\",model.ilike.\"%A\\\"B\\\\C%\")", expression("A\"B\\C"))
        assertFalse(CarSearchQuery.filter("x&is_available=eq.false").contains("&is_available"))
    }
}
