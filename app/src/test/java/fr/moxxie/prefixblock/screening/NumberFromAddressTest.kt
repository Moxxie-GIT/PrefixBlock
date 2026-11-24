package fr.moxxie.prefixblock.screening

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NumberFromAddressTest {

    @Test
    fun `tel address gives the number`() {
        assertEquals("+870773111632", numberFromAddress("tel", "+870773111632"))
        assertEquals("0690123456", numberFromAddress("TEL", " 0690123456 "))
    }

    @Test
    fun `sip address gives the user part without parameters`() {
        assertEquals("+33612345678", numberFromAddress("sip", "+33612345678@ims.example.org;user=phone"))
        assertEquals("+33612345678", numberFromAddress("sip", "+33612345678;user=phone"))
    }

    @Test
    fun `other schemes and empty addresses give no number`() {
        assertNull(numberFromAddress("voicemail", "123"))
        assertNull(numberFromAddress(null, "+33612345678"))
        assertNull(numberFromAddress("tel", ""))
        assertNull(numberFromAddress("tel", null))
        assertNull(numberFromAddress("sip", "@ims.example.org"))
    }
}
