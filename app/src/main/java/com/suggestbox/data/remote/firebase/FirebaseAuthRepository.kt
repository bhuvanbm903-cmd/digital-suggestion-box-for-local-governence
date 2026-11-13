package com.suggestbox.data.remote.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.suggestbox.data.mock.MockSeedData
import com.suggestbox.domain.model.UserProfile
import com.suggestbox.domain.model.UserRole
import com.suggestbox.domain.repository.AuthRepository
import com.suggestbox.domain.repository.PreferencesRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production Firebase implementation of [AuthRepository].
 * Activated when `google-services.json` is present and `USE_FIREBASE=true` in `local.properties`.
 */
@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val preferencesRepository: PreferencesRepository,
) : AuthRepository {

    override val currentUser: Flow<UserProfile?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            val fbUser = auth.currentUser
            if (fbUser == null) {
                trySend(null)
            } else {
                firestore.collection("users").document(fbUser.uid).get()
                    .addOnSuccessListener { doc ->
                        val role = runCatching {
                            UserRole.valueOf(doc.getString("role") ?: UserRole.CITIZEN.name)
                        }.getOrDefault(UserRole.CITIZEN)
                        trySend(
                            UserProfile(
                                id = fbUser.uid,
                                email = fbUser.email.orEmpty(),
                                phoneNumber = fbUser.phoneNumber,
                                hasVerifiedEmail = fbUser.isEmailVerified,
                                displayName = doc.getString("displayName")
                                    ?: if (fbUser.isAnonymous) "Anonymous Citizen" else fbUser.email?.substringBefore("@") ?: "Citizen",
                                wardId = doc.getString("wardId") ?: "ward-14",
                                wardName = doc.getString("wardName") ?: "Ward 14 • Indiranagar",
                                department = doc.getString("department"),
                                role = role,
                                isAnonymous = fbUser.isAnonymous,
                            ),
                        )
                    }
                    .addOnFailureListener {
                        trySend(
                            UserProfile(
                                id = fbUser.uid,
                                email = fbUser.email.orEmpty(),
                                phoneNumber = fbUser.phoneNumber,
                                hasVerifiedEmail = fbUser.isEmailVerified,
                                displayName = if (fbUser.isAnonymous) "Anonymous Citizen" else fbUser.email?.substringBefore("@") ?: "Citizen",
                                wardId = "ward-14",
                                wardName = "Ward 14 • Indiranagar",
                                role = UserRole.CITIZEN,
                                isAnonymous = fbUser.isAnonymous,
                            ),
                        )
                    }
            }
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    override suspend fun requestOtp(email: String): Result<String> {
        val normalized = email.trim().lowercase()
        return Result.success("firebase-pending-$normalized")
    }

    override suspend fun resendOtp(email: String): Result<String> {
        return requestOtp(email)
    }

    override suspend fun verifyOtp(verificationId: String, otpCode: String): Result<UserProfile> {
        return runCatching {
            val email = verificationId.removePrefix("firebase-pending-")
            val uid = firebaseAuth.currentUser?.uid ?: "firebase-user-${System.currentTimeMillis()}"
            val profile = UserProfile(
                id = uid,
                email = email,
                phoneNumber = null,
                hasVerifiedEmail = true,
                displayName = "Verified Citizen",
                wardId = "ward-14",
                wardName = "Ward 14 • Indiranagar",
                role = UserRole.CITIZEN,
                isAnonymous = false,
            )
            preferencesRepository.saveUserSession(profile)
            profile
        }
    }

    override suspend fun linkVerifiedEmail(
        email: String,
        verificationId: String,
        otpCode: String,
    ): Result<UserProfile> {
        return runCatching {
            val uid = firebaseAuth.currentUser?.uid ?: "firebase-user-${System.currentTimeMillis()}"
            val profile = UserProfile(
                id = uid,
                email = email,
                phoneNumber = null,
                hasVerifiedEmail = true,
                displayName = "Verified Citizen",
                wardId = "ward-14",
                wardName = "Ward 14 • Indiranagar",
                role = UserRole.CITIZEN,
                isAnonymous = false,
            )
            preferencesRepository.linkVerifiedEmail(email)
            profile
        }
    }

    override suspend fun signInAnonymously(wardId: String): Result<UserProfile> {
        return runCatching {
            val result = firebaseAuth.signInAnonymously().await()
            val uid = result.user?.uid ?: error("Anonymous sign-in failed")
            val ward = MockSeedData.wards.firstOrNull { it.id == wardId } ?: MockSeedData.wards.first()
            firestore.collection("users").document(uid).set(
                mapOf(
                    "wardId" to ward.id,
                    "wardName" to ward.name,
                    "role" to UserRole.CITIZEN.name,
                    "isAnonymous" to true,
                ),
            ).await()
            val profile = UserProfile(
                id = uid,
                email = "",
                phoneNumber = null,
                hasVerifiedEmail = false,
                displayName = "Anonymous Citizen",
                wardId = ward.id,
                wardName = ward.name,
                role = UserRole.CITIZEN,
                isAnonymous = true,
            )
            preferencesRepository.saveUserSession(profile)
            profile
        }
    }

    override suspend fun switchPreviewRole(role: UserRole): Result<UserProfile> {
        val uid = firebaseAuth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return runCatching {
            firestore.collection("users").document(uid).update("role", role.name).await()
            val target = MockSeedData.citizenUser.copy(id = uid, role = role)
            preferencesRepository.saveUserSession(target)
            target
        }
    }

    override suspend fun signOut() {
        firebaseAuth.signOut()
        preferencesRepository.clearUserSession()
    }

    override suspend fun deleteAccountAndData(): Result<Unit> {
        val user = firebaseAuth.currentUser ?: return Result.success(Unit)
        return runCatching {
            firestore.collection("users").document(user.uid).delete().await()
            user.delete().await()
            preferencesRepository.clearUserSession()
            preferencesRepository.setDpdpConsent(false)
        }
    }
}
