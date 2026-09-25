package com.example.data

import android.content.Context
import android.util.Log
import com.example.model.AppUser
import com.example.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class AuthRepository(private val context: Context) {
    private val TAG = "AuthRepository"

    private val _currentUser = MutableStateFlow<AppUser?>(null)
    val currentUser: StateFlow<AppUser?> = _currentUser.asStateFlow()

    // Pre-configured Demo Accounts for Hackathon presentation
    val demoAccounts = listOf(
        DemoAccount(
            email = "scanner@sih.gov.in",
            password = "password123",
            name = "Arjun Verma",
            role = UserRole.SCANNER,
            description = "Field Inspector / Scanner",
            inspectorId = "INSP-IND-8821",
            badgeNumber = "BADGE-4409",
            jurisdiction = "Central Enforcement Zone - New Delhi",
            department = "National Consumer Quality & Standards Bureau"
        ),
        DemoAccount(
            email = "agent2@sih.gov.in",
            password = "password123",
            name = "Rohan Patel",
            role = UserRole.SCANNER,
            description = "Secondary Field Agent",
            inspectorId = "INSP-MUM-3120",
            badgeNumber = "BADGE-1192",
            jurisdiction = "Western Industrial Division - Mumbai",
            department = "National Consumer Quality & Standards Bureau"
        ),
        DemoAccount(
            email = "admin@sih.gov.in",
            password = "password123",
            name = "Dr. Priya Sharma",
            role = UserRole.ADMIN,
            description = "Chief Verification Officer (Admin)",
            inspectorId = "ADMIN-HQ-001",
            badgeNumber = "BADGE-0001",
            jurisdiction = "National Directorate - HQ",
            department = "Standards & Anti-Counterfeiting Command"
        )
    )

    init {
        // Check if there is an active Firebase Auth user
        FirebaseManager.initialize(context)
        val firebaseAuth = FirebaseManager.auth
        val currentFirebaseUser = firebaseAuth?.currentUser
        if (currentFirebaseUser != null) {
            val email = currentFirebaseUser.email ?: "scanner@sih.gov.in"
            val demoMatch = demoAccounts.find { it.email.equals(email, ignoreCase = true) }
            val role = demoMatch?.role ?: if (email.contains("admin", ignoreCase = true)) UserRole.ADMIN else UserRole.SCANNER
            val name = currentFirebaseUser.displayName ?: demoMatch?.name ?: email.substringBefore("@")
            val inspectorId = demoMatch?.inspectorId ?: "INSP-IND-8821"
            val badgeNumber = demoMatch?.badgeNumber ?: "BADGE-4409"
            val jurisdiction = demoMatch?.jurisdiction ?: "Central Enforcement Zone - New Delhi"
            val department = demoMatch?.department ?: "National Consumer Quality & Standards Bureau"
            _currentUser.value = AppUser(
                id = currentFirebaseUser.uid,
                email = email,
                name = name,
                role = role,
                inspectorId = inspectorId,
                badgeNumber = badgeNumber,
                jurisdiction = jurisdiction,
                department = department
            )
        }
    }

    suspend fun login(email: String, pass: String): Result<AppUser> {
        val trimmedEmail = email.trim()
        val trimmedPass = pass.trim()

        if (trimmedEmail.isEmpty() || trimmedPass.isEmpty()) {
            return Result.failure(IllegalArgumentException("Email and password cannot be empty."))
        }

        // Check if matching demo credentials
        val demoMatch = demoAccounts.find { it.email.equals(trimmedEmail, ignoreCase = true) }

        // Attempt Firebase Auth if available
        val firebaseAuth = FirebaseManager.auth
        if (firebaseAuth != null) {
            try {
                val authResult = try {
                    firebaseAuth.signInWithEmailAndPassword(trimmedEmail, trimmedPass).await()
                } catch (e: Exception) {
                    Log.w(TAG, "signInWithEmailAndPassword failed, attempting create: ${e.message}")
                    // Attempt create user if not found
                    firebaseAuth.createUserWithEmailAndPassword(trimmedEmail, trimmedPass).await()
                }

                val uid = authResult.user?.uid ?: ("user_" + System.currentTimeMillis())
                val role = demoMatch?.role ?: if (trimmedEmail.contains("admin", ignoreCase = true)) {
                    UserRole.ADMIN
                } else {
                    UserRole.SCANNER
                }
                val name = demoMatch?.name ?: trimmedEmail.substringBefore("@").replace(".", " ").capitalizeWords()
                val inspectorId = demoMatch?.inspectorId ?: "INSP-IND-8821"
                val badgeNumber = demoMatch?.badgeNumber ?: "BADGE-4409"
                val jurisdiction = demoMatch?.jurisdiction ?: "Central Enforcement Zone - New Delhi"
                val department = demoMatch?.department ?: "National Consumer Quality & Standards Bureau"

                val appUser = AppUser(
                    id = uid,
                    email = trimmedEmail,
                    name = name,
                    role = role,
                    inspectorId = inspectorId,
                    badgeNumber = badgeNumber,
                    jurisdiction = jurisdiction,
                    department = department
                )

                // Sync user to Firestore if possible
                try {
                    val firestore = FirebaseManager.firestore
                    firestore?.collection("users")?.document(uid)?.set(appUser.toMap())?.await()
                } catch (fe: Exception) {
                    Log.w(TAG, "Firestore sync user failed: ${fe.message}")
                }

                _currentUser.value = appUser
                return Result.success(appUser)
            } catch (e: Exception) {
                Log.w(TAG, "Firebase Auth error, evaluating demo fallback: ${e.message}")
            }
        }

        // Demo account fallback (works seamlessly when Firebase is offline / without google-services.json)
        if (demoMatch != null) {
            if (trimmedPass == demoMatch.password || trimmedPass.length >= 6) {
                val appUser = AppUser(
                    id = "demo_uid_" + demoMatch.email.hashCode(),
                    email = demoMatch.email,
                    name = demoMatch.name,
                    role = demoMatch.role,
                    inspectorId = demoMatch.inspectorId,
                    badgeNumber = demoMatch.badgeNumber,
                    jurisdiction = demoMatch.jurisdiction,
                    department = demoMatch.department
                )
                _currentUser.value = appUser
                return Result.success(appUser)
            } else {
                return Result.failure(IllegalArgumentException("Incorrect password for ${demoMatch.email}."))
            }
        }

        // Any arbitrary user login during prototype
        val role = if (trimmedEmail.contains("admin", ignoreCase = true)) UserRole.ADMIN else UserRole.SCANNER
        val user = AppUser(
            id = "user_" + System.currentTimeMillis(),
            email = trimmedEmail,
            name = trimmedEmail.substringBefore("@").replace(".", " ").capitalizeWords(),
            role = role,
            inspectorId = "INSP-FIELD-" + (System.currentTimeMillis() % 10000),
            badgeNumber = "BADGE-9999",
            jurisdiction = "Field Operations",
            department = "Consumer Quality Bureau"
        )
        _currentUser.value = user
        return Result.success(user)
    }

    fun logout() {
        try {
            FirebaseManager.auth?.signOut()
        } catch (e: Exception) {
            Log.w(TAG, "Firebase signOut error: ${e.message}")
        }
        _currentUser.value = null
    }

    private fun String.capitalizeWords(): String = split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}

data class DemoAccount(
    val email: String,
    val password: String,
    val name: String,
    val role: UserRole,
    val description: String,
    val inspectorId: String,
    val badgeNumber: String,
    val jurisdiction: String,
    val department: String
)
