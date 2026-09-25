package com.example.model

enum class UserRole {
    SCANNER,
    ADMIN;

    companion object {
        fun fromString(role: String?): UserRole {
            return when (role?.uppercase()?.trim()) {
                "ADMIN" -> ADMIN
                else -> SCANNER
            }
        }
    }
}

data class AppUser(
    val id: String = "",
    val email: String = "",
    val name: String = "",
    val role: UserRole = UserRole.SCANNER,
    val inspectorId: String = "INSP-IND-8821",
    val badgeNumber: String = "BADGE-4409",
    val jurisdiction: String = "Central Enforcement Zone - New Delhi",
    val department: String = "National Consumer Quality & Standards Bureau"
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "email" to email,
            "name" to name,
            "role" to role.name,
            "inspectorId" to inspectorId,
            "badgeNumber" to badgeNumber,
            "jurisdiction" to jurisdiction,
            "department" to department
        )
    }

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): AppUser {
            return AppUser(
                id = id,
                email = map["email"] as? String ?: "",
                name = map["name"] as? String ?: "User",
                role = UserRole.fromString(map["role"] as? String),
                inspectorId = map["inspectorId"] as? String ?: "INSP-IND-8821",
                badgeNumber = map["badgeNumber"] as? String ?: "BADGE-4409",
                jurisdiction = map["jurisdiction"] as? String ?: "Central Enforcement Zone - New Delhi",
                department = map["department"] as? String ?: "National Consumer Quality & Standards Bureau"
            )
        }
    }
}
