package com.example

import com.example.util.MapHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MapLocationHelperTest {

    @Test
    fun testValidGoogleMapsUrls() {
        // Standard full web URLs
        assertTrue(MapHelper.isGoogleMapsUrl("https://maps.google.com/?q=34.0837,74.7973"))
        assertTrue(MapHelper.isGoogleMapsUrl("https://www.google.com/maps/place/Srinagar"))
        assertTrue(MapHelper.isGoogleMapsUrl("https://google.com/maps/@34.0837,74.7973,15z"))
        assertTrue(MapHelper.isGoogleMapsUrl("http://maps.google.com/maps?daddr=34.0837,74.7973"))
        assertTrue(MapHelper.isGoogleMapsUrl("https://www.google.co.in/maps/search/orchard"))

        // Short URLs
        assertTrue(MapHelper.isGoogleMapsUrl("https://maps.app.goo.gl/abcdef123456"))
        assertTrue(MapHelper.isGoogleMapsUrl("http://maps.app.goo.gl/xyz"))
        assertTrue(MapHelper.isGoogleMapsUrl("https://goo.gl/maps/abcdef123456"))
        assertTrue(MapHelper.isGoogleMapsUrl("http://goo.gl/maps/xyz"))

        // Without protocol
        assertTrue(MapHelper.isGoogleMapsUrl("maps.google.com/?q=34.0837,74.7973"))
        assertTrue(MapHelper.isGoogleMapsUrl("www.google.com/maps/place/Srinagar"))
        assertTrue(MapHelper.isGoogleMapsUrl("maps.app.goo.gl/abcdef123456"))
        assertTrue(MapHelper.isGoogleMapsUrl("goo.gl/maps/abcdef123456"))
    }

    @Test
    fun testNormalTextLocationIsNotMapsUrl() {
        assertFalse(MapHelper.isGoogleMapsUrl("Block A, North Field, Village Green Valley"))
        assertFalse(MapHelper.isGoogleMapsUrl("Srinagar Highway near Petrol Pump"))
        assertFalse(MapHelper.isGoogleMapsUrl("Shop No. 12, Fruit Mandi, Sopore"))
        assertFalse(MapHelper.isGoogleMapsUrl(""))
        assertFalse(MapHelper.isGoogleMapsUrl("   "))
        assertFalse(MapHelper.isGoogleMapsUrl(null))
    }

    @Test
    fun testUnrelatedUrlsAreNotMapsUrls() {
        assertFalse(MapHelper.isGoogleMapsUrl("https://facebook.com/maps"))
        assertFalse(MapHelper.isGoogleMapsUrl("https://example.com"))
        assertFalse(MapHelper.isGoogleMapsUrl("https://google.com/search?q=apple"))
        assertFalse(MapHelper.isGoogleMapsUrl("https://instagram.com/p/12345"))
    }

    @Test
    fun testSanitizeMapsUrl() {
        assertEquals("https://maps.google.com/?q=1,2", MapHelper.sanitizeMapsUrl("maps.google.com/?q=1,2"))
        assertEquals("https://maps.app.goo.gl/xyz", MapHelper.sanitizeMapsUrl("maps.app.goo.gl/xyz"))
        assertEquals("https://maps.google.com/?q=1,2", MapHelper.sanitizeMapsUrl("https://maps.google.com/?q=1,2"))
        assertEquals("http://maps.google.com/?q=1,2", MapHelper.sanitizeMapsUrl("http://maps.google.com/?q=1,2"))
    }
}
