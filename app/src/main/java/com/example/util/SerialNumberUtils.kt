package com.example.util

import com.example.data.CropRecord
import com.example.data.GardenPlanningEntry
import com.example.data.GlobalSearchResult

object SerialNumberUtils {

    /**
     * Extracts the first contiguous sequence of digits from a serial number as a [Long].
     * If no digits are found or the string is empty/blank, returns -1L
     * so non-numbered items sort after numbered ones in descending order.
     */
    fun extractNumericValue(serialNumber: String?): Long {
        if (serialNumber == null) return -1L
        val trimmed = serialNumber.trim()
        if (trimmed.isBlank()) return -1L
        val match = Regex("""\d+""").find(trimmed)
        return match?.value?.toLongOrNull() ?: -1L
    }

    /**
     * Extracts the prefix before the numeric portion of the serial number (e.g. "LP-" from "LP-01").
     */
    fun extractPrefix(serialNumber: String?): String {
        if (serialNumber == null) return ""
        val trimmed = serialNumber.trim()
        val match = Regex("""^([^\d]+)""").find(trimmed)
        return match?.groupValues?.get(1)?.trim() ?: ""
    }

    /**
     * Compare two serial numbers based on their numerical integer value in descending order (highest first).
     * If numeric values are identical, compares prefix and full string as tie-breakers.
     */
    fun compareSerials(s1: String?, s2: String?): Int {
        val str1 = s1 ?: ""
        val str2 = s2 ?: ""
        val n1 = extractNumericValue(str1)
        val n2 = extractNumericValue(str2)
        if (n1 != n2) {
            return n2.compareTo(n1)
        }
        val prefixCompare = extractPrefix(str1).compareTo(extractPrefix(str2), ignoreCase = true)
        if (prefixCompare != 0) {
            return prefixCompare
        }
        return str2.compareTo(str1, ignoreCase = true)
    }

    /**
     * Comparator to sort CropRecord objects strictly in descending numerical order of their Serial Number.
     * Highest numeric serial appears first (e.g., LP-05, LP-04, LP-03, LP-02, LP-01).
     */
    val cropRecordComparator: Comparator<CropRecord> = Comparator { r1, r2 ->
        val n1 = extractNumericValue(r1.serialNumber)
        val n2 = extractNumericValue(r2.serialNumber)
        if (n1 != n2) {
            n2.compareTo(n1)
        } else {
            val prefixCompare = extractPrefix(r1.serialNumber).compareTo(extractPrefix(r2.serialNumber), ignoreCase = true)
            if (prefixCompare != 0) {
                prefixCompare
            } else {
                val fullCompare = r2.serialNumber.compareTo(r1.serialNumber, ignoreCase = true)
                if (fullCompare != 0) {
                    fullCompare
                } else {
                    r2.id.compareTo(r1.id)
                }
            }
        }
    }

    /**
     * Comparator to sort GardenPlanningEntry objects strictly in descending numerical order of their Serial Number.
     * Highest numeric serial appears first (e.g., GP-05, GP-04, GP-03, GP-02, GP-01).
     */
    val gardenEntryComparator: Comparator<GardenPlanningEntry> = Comparator { e1, e2 ->
        val n1 = extractNumericValue(e1.serialNumber)
        val n2 = extractNumericValue(e2.serialNumber)
        if (n1 != n2) {
            n2.compareTo(n1)
        } else {
            val prefixCompare = extractPrefix(e1.serialNumber).compareTo(extractPrefix(e2.serialNumber), ignoreCase = true)
            if (prefixCompare != 0) {
                prefixCompare
            } else {
                val fullCompare = e2.serialNumber.compareTo(e1.serialNumber, ignoreCase = true)
                if (fullCompare != 0) {
                    fullCompare
                } else {
                    e2.id.compareTo(e1.id)
                }
            }
        }
    }

    /**
     * Comparator to sort GlobalSearchResult objects strictly in descending numerical order of their Serial Number.
     * Highest numeric serial appears first.
     */
    val globalSearchResultComparator: Comparator<GlobalSearchResult> = Comparator { g1, g2 ->
        val n1 = extractNumericValue(g1.serialNumber)
        val n2 = extractNumericValue(g2.serialNumber)
        if (n1 != n2) {
            n2.compareTo(n1)
        } else {
            val prefixCompare = extractPrefix(g1.serialNumber).compareTo(extractPrefix(g2.serialNumber), ignoreCase = true)
            if (prefixCompare != 0) {
                prefixCompare
            } else {
                val fullCompare = g2.serialNumber.compareTo(g1.serialNumber, ignoreCase = true)
                if (fullCompare != 0) {
                    fullCompare
                } else {
                    g2.id.compareTo(g1.id)
                }
            }
        }
    }
}
