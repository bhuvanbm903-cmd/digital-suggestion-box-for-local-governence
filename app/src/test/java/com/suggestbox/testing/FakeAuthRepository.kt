package com.suggestbox.testing

import com.suggestbox.data.mock.MockSeedData
import com.suggestbox.domain.model.UserProfile
import com.suggestbox.domain.model.UserRole
import com.suggestbox.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Pure Kotlin test double for [AuthRepository] running cleanly in JVM unit tests.
 */
class FakeAuthRepository(
    initialUser: UserProfile? = MockSeedData.citizenUser,
) : AuthRepository {

    private val _currentUser = MutableStateFlow(initialUser)
    override val currentUser: Flow<UserProfile?> = _currentUser.asStateFlow()

    override suspend fun requestOtp(email: String): Result<String> = Result.success("test-ver-id")

    override suspend fun resendOtp(email: String): Result<String> = Result.success("test-ver-id")

    override suspend fun verifyOtp(verificationId: String, otpCode: String): Result<UserProfile> =
        Result.success(_currentUser.value ?: MockSeedData.citizenUser)

    override suspend fun linkVerifiedEmail(email: String, verificationId: String, otpCode: String): Result<UserProfile> {
        val current = _currentUser.value ?: MockSeedData.citizenUser
        val updated = current.copy(email = email, hasVerifiedEmail = true)
        _currentUser.value = updated
        return Result.success(updated)
    }

    override suspend fun signInAnonymously(wardId: String): Result<UserProfile> =
        Result.success(_currentUser.value ?: MockSeedData.citizenUser)

    override suspend fun switchPreviewRole(role: UserRole): Result<UserProfile> {
        val target = when (role) {
            UserRole.CITIZEN -> MockSeedData.citizenUser
            UserRole.OFFICIAL -> MockSeedData.officialUser
            UserRole.SUPER_ADMIN -> MockSeedData.superAdminUser
        }
        _currentUser.value = target
        return Result.success(target)
    }

    override suspend fun signOut() {
        _currentUser.value = null
    }

    override suspend fun deleteAccountAndData(): Result<Unit> {
        _currentUser.value = null
        return Result.success(Unit)
    }
}
