package com.example.data

data class BusinessInfo(
    val businessName: String = "BAAGBAAN BOI",
    val tagline: String = "The Streets of Kashmir",
    val address: String = "Ramnagri 192303, Shopian, Jammu & Kashmir",
    val contactNumbers: List<String> = listOf("+916006143037", "+917006996169", "+917051826858", "+916005096439"),
    val accountNumber: String = "0018010100007537",
    val accountHolderName: String = "Aamir Manzoor Ganaie",
    val ifscCode: String = "JAKA0SHOPAN",
    val registrationNumber: String = "01EBWPG3946L1Z7"
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "businessName" to businessName,
            "tagline" to tagline,
            "address" to address,
            "contactNumbers" to contactNumbers,
            "accountNumber" to accountNumber,
            "accountHolderName" to accountHolderName,
            "ifscCode" to ifscCode,
            "registrationNumber" to registrationNumber
        )
    }

    companion object {
        val DEFAULT = BusinessInfo()

        fun fromMap(map: Map<String, Any?>): BusinessInfo {
            val businessName = (map["businessName"] as? String)?.takeIf { it.isNotBlank() } ?: DEFAULT.businessName
            val tagline = (map["tagline"] as? String)?.takeIf { it.isNotBlank() } ?: DEFAULT.tagline
            val address = (map["address"] as? String)?.takeIf { it.isNotBlank() } ?: DEFAULT.address
            val contactList = (map["contactNumbers"] as? List<*>)
                ?.mapNotNull { it?.toString()?.trim() }
                ?.filter { it.isNotBlank() }
            val contactNumbers = if (!contactList.isNullOrEmpty()) contactList else DEFAULT.contactNumbers
            val accountNumber = (map["accountNumber"] as? String)?.takeIf { it.isNotBlank() } ?: DEFAULT.accountNumber
            val accountHolderName = (map["accountHolderName"] as? String)?.takeIf { it.isNotBlank() } ?: DEFAULT.accountHolderName
            val ifscCode = (map["ifscCode"] as? String)?.takeIf { it.isNotBlank() } ?: DEFAULT.ifscCode
            val registrationNumber = (map["registrationNumber"] as? String)?.takeIf { it.isNotBlank() } ?: DEFAULT.registrationNumber

            return BusinessInfo(
                businessName = businessName,
                tagline = tagline,
                address = address,
                contactNumbers = contactNumbers,
                accountNumber = accountNumber,
                accountHolderName = accountHolderName,
                ifscCode = ifscCode,
                registrationNumber = registrationNumber
            )
        }
    }
}
