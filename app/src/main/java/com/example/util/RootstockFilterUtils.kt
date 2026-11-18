package com.example.util

import com.example.data.CropRecord
import com.example.data.GardenPlanningEntry
import com.example.data.parseVarietyLines

data class RootstockFamilyRule(
    val label: String,
    val matchesNormalized: (String) -> Boolean
)

/**
 * Single list of rootstock family rules evaluated in order against normalized text
 * (uppercase A-Z and 0-9 only). Add a new family by adding an entry here.
 */
val ROOTSTOCK_FAMILY_RULES: List<RootstockFamilyRule> = listOf(
    RootstockFamilyRule(
        label = "M9",
        matchesNormalized = { norm ->
            norm.startsWith("M9") || norm.startsWith("M09") || norm.contains("T337")
        }
    ),
    RootstockFamilyRule(
        label = "MM",
        matchesNormalized = { norm ->
            norm.startsWith("MM")
        }
    )
)

sealed class RootstockFilter {
    data object All : RootstockFilter()

    data class Family(val familyLabel: String) : RootstockFilter()

    data class Other(
        val normalizedValue: String,
        val displayLabel: String = normalizedValue
    ) : RootstockFilter() {
        override fun equals(other: Any?): Boolean =
            other is Other && normalizedValue == other.normalizedValue

        override fun hashCode(): Int = normalizedValue.hashCode()
    }

    data object NotSpecified : RootstockFilter()

    fun displayName(options: List<RootstockFilterOption> = emptyList()): String = when (this) {
        All -> "All Rootstocks"
        is Family -> familyLabel
        is Other -> options.firstOrNull { it.filter == this }?.shortLabel ?: displayLabel
        NotSpecified -> "Not Specified"
    }

    fun buttonLabel(options: List<RootstockFilterOption> = emptyList()): String = when (this) {
        All -> "Rootstock: All"
        is Family -> "Rootstock: $familyLabel"
        is Other -> "Rootstock: ${options.firstOrNull { it.filter == this }?.shortLabel ?: displayLabel}"
        NotSpecified -> "Rootstock: Not Specified"
    }
}

data class RootstockFilterOption(
    val filter: RootstockFilter,
    val dropdownLabel: String,
    val shortLabel: String,
    val count: Int
)

private val NON_ALPHANUMERIC_REGEX = Regex("[^A-Z0-9]")
private val NOTES_ROOTSTOCK_REGEX = Regex("""Rootstock:\s*([^|\]\n]+)""", RegexOption.IGNORE_CASE)
private val JSON_ROOTSTOCK_FIELD_REGEX = Regex(""""rootstock"\s*:\s*"([^"]*)"""", RegexOption.IGNORE_CASE)

fun normalizeRootstockToken(raw: String): String {
    return raw.uppercase().replace(NON_ALPHANUMERIC_REGEX, "")
}

fun classifyRootstockValue(raw: String): RootstockFilter? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return null
    val normalized = normalizeRootstockToken(trimmed)
    if (normalized.isEmpty()) return null

    val matchedRule = ROOTSTOCK_FAMILY_RULES.firstOrNull { it.matchesNormalized(normalized) }
    return if (matchedRule != null) {
        RootstockFilter.Family(matchedRule.label)
    } else {
        RootstockFilter.Other(normalizedValue = normalized, displayLabel = trimmed)
    }
}

fun splitCommaSeparatedRootstocks(raw: String): List<String> {
    if (raw.isBlank()) return emptyList()
    return raw.split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
}

private fun extractRootstocksFromVarietyLinesJson(varietyLinesJson: String): List<String> {
    if (varietyLinesJson.isBlank()) return emptyList()
    val parsedLines = parseVarietyLines(varietyLinesJson)
    if (parsedLines.isNotEmpty()) {
        return parsedLines.flatMap { splitCommaSeparatedRootstocks(it.rootstock) }
    }
    return JSON_ROOTSTOCK_FIELD_REGEX.findAll(varietyLinesJson)
        .flatMap { match -> splitCommaSeparatedRootstocks(match.groupValues[1]).asSequence() }
        .toList()
}

fun classifyCropRecord(record: CropRecord): Set<RootstockFilter> {
    val classifications = LinkedHashSet<RootstockFilter>()

    for (token in splitCommaSeparatedRootstocks(record.rootstock)) {
        classifyRootstockValue(token)?.let { classifications.add(it) }
    }

    for (token in extractRootstocksFromVarietyLinesJson(record.varietyLinesJson)) {
        classifyRootstockValue(token)?.let { classifications.add(it) }
    }

    if (classifications.isEmpty() && record.notes.isNotBlank()) {
        val notesMatch = NOTES_ROOTSTOCK_REGEX.find(record.notes)
        val notesValue = notesMatch?.groupValues?.getOrNull(1)?.trim().orEmpty()
        for (token in splitCommaSeparatedRootstocks(notesValue)) {
            classifyRootstockValue(token)?.let { classifications.add(it) }
        }
    }

    if (classifications.isEmpty()) {
        classifications.add(RootstockFilter.NotSpecified)
    }

    return classifications
}

fun classifyGardenEntry(entry: GardenPlanningEntry): Set<RootstockFilter> {
    val classifications = LinkedHashSet<RootstockFilter>()

    for (token in splitCommaSeparatedRootstocks(entry.rootStock)) {
        classifyRootstockValue(token)?.let { classifications.add(it) }
    }

    for (token in extractRootstocksFromVarietyLinesJson(entry.varietyLinesJson)) {
        classifyRootstockValue(token)?.let { classifications.add(it) }
    }

    if (classifications.isEmpty()) {
        classifications.add(RootstockFilter.NotSpecified)
    }

    return classifications
}

fun matchesRootstockFilter(
    classifications: Set<RootstockFilter>,
    selectedFilter: RootstockFilter
): Boolean {
    return when (selectedFilter) {
        RootstockFilter.All -> true
        else -> selectedFilter in classifications
    }
}

fun matchesRootstockFilter(record: CropRecord, selectedFilter: RootstockFilter): Boolean =
    matchesRootstockFilter(classifyCropRecord(record), selectedFilter)

fun matchesRootstockFilter(entry: GardenPlanningEntry, selectedFilter: RootstockFilter): Boolean =
    matchesRootstockFilter(classifyGardenEntry(entry), selectedFilter)

fun buildRootstockFilterOptions(
    classificationSets: List<Set<RootstockFilter>>
): List<RootstockFilterOption> {
    val options = mutableListOf<RootstockFilterOption>()

    options.add(
        RootstockFilterOption(
            filter = RootstockFilter.All,
            dropdownLabel = "All Rootstocks",
            shortLabel = "All",
            count = classificationSets.size
        )
    )

    for (rule in ROOTSTOCK_FAMILY_RULES) {
        val familyFilter = RootstockFilter.Family(rule.label)
        val count = classificationSets.count { familyFilter in it }
        options.add(
            RootstockFilterOption(
                filter = familyFilter,
                dropdownLabel = "${rule.label} ($count)",
                shortLabel = rule.label,
                count = count
            )
        )
    }

    val otherFirstLabel = LinkedHashMap<String, String>()
    val otherCounts = LinkedHashMap<String, Int>()
    for (set in classificationSets) {
        for (item in set) {
            if (item is RootstockFilter.Other) {
                if (!otherFirstLabel.containsKey(item.normalizedValue)) {
                    otherFirstLabel[item.normalizedValue] = item.displayLabel
                }
                otherCounts[item.normalizedValue] = (otherCounts[item.normalizedValue] ?: 0) + 1
            }
        }
    }

    val sortedOtherKeys = otherFirstLabel.keys.sortedWith(
        compareBy<String> { otherFirstLabel[it].orEmpty().lowercase() }
            .thenBy { otherFirstLabel[it].orEmpty() }
    )
    for (normKey in sortedOtherKeys) {
        val displayLabel = otherFirstLabel[normKey] ?: normKey
        val count = otherCounts[normKey] ?: 0
        val otherFilter = RootstockFilter.Other(normalizedValue = normKey, displayLabel = displayLabel)
        options.add(
            RootstockFilterOption(
                filter = otherFilter,
                dropdownLabel = "$displayLabel ($count)",
                shortLabel = displayLabel,
                count = count
            )
        )
    }

    val notSpecifiedCount = classificationSets.count { RootstockFilter.NotSpecified in it }
    if (notSpecifiedCount > 0) {
        options.add(
            RootstockFilterOption(
                filter = RootstockFilter.NotSpecified,
                dropdownLabel = "Not Specified ($notSpecifiedCount)",
                shortLabel = "Not Specified",
                count = notSpecifiedCount
            )
        )
    }

    return options
}

fun buildRootstockFilterOptionsForCropRecords(
    records: Iterable<CropRecord>
): List<RootstockFilterOption> =
    buildRootstockFilterOptions(records.map { classifyCropRecord(it) })

fun buildRootstockFilterOptionsForGardenEntries(
    entries: Iterable<GardenPlanningEntry>
): List<RootstockFilterOption> =
    buildRootstockFilterOptions(entries.map { classifyGardenEntry(it) })
