package com.example.data.firebase

import android.content.Context
import android.util.Base64
import android.util.Log
import com.example.R
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import java.util.Locale

sealed interface SendEmailVerificationOutcome {
    data class Sent(val email: String) : SendEmailVerificationOutcome
    data class AlreadyVerified(val user: FirebaseUser, val email: String) : SendEmailVerificationOutcome
    data class EmailAlreadyInUse(val message: String) : SendEmailVerificationOutcome
    data class ProviderNotEnabled(val message: String) : SendEmailVerificationOutcome
    data class Error(val message: String) : SendEmailVerificationOutcome
}

sealed interface SignInExistingUserOutcome {
    data class Verified(val user: FirebaseUser, val email: String) : SignInExistingUserOutcome
    data class NotYetVerified(val user: FirebaseUser, val email: String) : SignInExistingUserOutcome
    data class InvalidCredentials(val message: String) : SignInExistingUserOutcome
    data class UserNotFound(val message: String) : SignInExistingUserOutcome
    data class Error(val message: String) : SignInExistingUserOutcome
}

sealed interface CheckEmailVerificationOutcome {
    data class Verified(val user: FirebaseUser, val email: String) : CheckEmailVerificationOutcome
    data class NotYetVerified(val message: String) : CheckEmailVerificationOutcome
    data class NoActiveSession(val message: String) : CheckEmailVerificationOutcome
    data class Error(val message: String) : CheckEmailVerificationOutcome
}

sealed interface SendPasswordResetOutcome {
    data class Sent(val email: String) : SendPasswordResetOutcome
    data class Error(val message: String) : SendPasswordResetOutcome
}

object EmailVerificationAuthHelper {
    private const val TAG = "EmailAuth"
    const val MIN_PASSWORD_LENGTH = 6

    private val EMAIL_REGEX = Regex(
        "^[a-z0-9._%+\\-]{1,64}@[a-z0-9.\\-]{1,180}\\.[a-z]{2,24}$"
    )

    fun normalizeEmail(raw: String): String = raw.trim().lowercase(Locale.US)

    fun isValidEmail(raw: String): Boolean {
        val clean = normalizeEmail(raw)
        if (clean.length > 254 || clean.contains("..")) return false
        return EMAIL_REGEX.matches(clean)
    }

    /**
     * Generates a fresh cryptographically random high-entropy credential for passwordless-UX
     * Firebase Auth account creation + standard email verification (never hardcoded or logged).
     */
    internal fun generateEphemeralStrongPassword(): String {
        val bytes = ByteArray(24)
        SecureRandom().nextBytes(bytes)
        val token = Base64.encodeToString(bytes, Base64.NO_WRAP or Base64.URL_SAFE)
        return "Sf!9$token"
    }

    /**
     * Authenticates an existing Firebase Auth Email/Password user so returning verified users,
     * post-password-reset logins, and missing Firestore username-mapping recoveries succeed cleanly.
     */
    suspend fun signInExistingVerifiedUser(
        context: Context,
        rawEmail: String,
        rawPassword: String
    ): SignInExistingUserOutcome {
        val email = normalizeEmail(rawEmail)
        if (!isValidEmail(email)) {
            return SignInExistingUserOutcome.Error(context.getString(R.string.auth_email_error_invalid))
        }
        if (rawPassword.isEmpty()) {
            return SignInExistingUserOutcome.InvalidCredentials(context.getString(R.string.auth_password_error_empty))
        }

        val firebaseAuth = FirebaseSafeProvider.getAuth(context)
            ?: return SignInExistingUserOutcome.Error(context.getString(R.string.msg_firebase_not_ready))

        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, rawPassword)
                .addOnFailureListener { err -> Log.w(TAG, "signInWithEmailAndPassword failed: ${err.message}") }
                .await()
            val signedInUser = authResult.user
                ?: return SignInExistingUserOutcome.Error(context.getString(R.string.auth_verify_error_generic))
            runCatching {
                signedInUser.reload()
                    .addOnFailureListener { err -> Log.w(TAG, "reload after signIn failed: ${err.message}") }
                    .await()
            }
            val refreshed = firebaseAuth.currentUser ?: signedInUser
            val resolvedEmail = refreshed.email?.trim()?.lowercase(Locale.US).orEmpty().ifEmpty { email }
            if (refreshed.isEmailVerified) {
                SignInExistingUserOutcome.Verified(user = refreshed, email = resolvedEmail)
            } else {
                SignInExistingUserOutcome.NotYetVerified(user = refreshed, email = resolvedEmail)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: FirebaseAuthInvalidCredentialsException) {
            SignInExistingUserOutcome.InvalidCredentials(context.getString(R.string.auth_password_error_wrong))
        } catch (_: FirebaseAuthInvalidUserException) {
            SignInExistingUserOutcome.UserNotFound(context.getString(R.string.auth_forgot_password_user_not_found))
        } catch (_: FirebaseNetworkException) {
            SignInExistingUserOutcome.Error(context.getString(R.string.auth_error_network))
        } catch (e: FirebaseAuthException) {
            Log.w(TAG, "signInExistingVerifiedUser failed with code=${e.errorCode}")
            when (e.errorCode) {
                "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL" ->
                    SignInExistingUserOutcome.InvalidCredentials(context.getString(R.string.auth_password_error_wrong))
                "ERROR_USER_NOT_FOUND" ->
                    SignInExistingUserOutcome.UserNotFound(context.getString(R.string.auth_forgot_password_user_not_found))
                "ERROR_TOO_MANY_REQUESTS" ->
                    SignInExistingUserOutcome.Error(context.getString(R.string.auth_email_error_rate_limit))
                else ->
                    SignInExistingUserOutcome.Error(
                        e.localizedMessage ?: context.getString(R.string.auth_verify_error_generic)
                    )
            }
        } catch (e: Exception) {
            Log.w(TAG, "signInExistingVerifiedUser error: ${e.javaClass.simpleName}", e)
            SignInExistingUserOutcome.Error(
                e.localizedMessage ?: context.getString(R.string.auth_error_network)
            )
        }
    }

    /**
     * Sends a standard Firebase Email Verification link (FirebaseUser.sendEmailVerification()).
     * If the email is already registered in Firebase Auth, attempts recovery sign-in with the
     * provided password so verified accounts can automatically restore their username profile
     * instead of getting trapped in a "Username belum terdaftar" / "Email sudah terdaftar" loop.
     */
    suspend fun sendStandardVerificationEmail(
        context: Context,
        rawEmail: String,
        customPassword: String,
        isResend: Boolean = false
    ): SendEmailVerificationOutcome {
        val email = normalizeEmail(rawEmail)
        if (!isValidEmail(email)) {
            return SendEmailVerificationOutcome.Error(context.getString(R.string.auth_email_error_invalid))
        }
        val trimmedPassword = customPassword.trim()
        if (!isResend) {
            if (trimmedPassword.isEmpty()) {
                return SendEmailVerificationOutcome.Error(context.getString(R.string.auth_password_error_required))
            }
            if (trimmedPassword.length < MIN_PASSWORD_LENGTH) {
                return SendEmailVerificationOutcome.Error(context.getString(R.string.auth_password_error_short))
            }
        }

        val firebaseAuth = FirebaseSafeProvider.getAuth(context)
            ?: return SendEmailVerificationOutcome.Error(context.getString(R.string.msg_firebase_not_ready))

        suspend fun recoverExistingEmailAccount(): SendEmailVerificationOutcome {
            if (trimmedPassword.isEmpty()) {
                return SendEmailVerificationOutcome.EmailAlreadyInUse(
                    context.getString(R.string.auth_email_error_already_in_use)
                )
            }
            return when (val signIn = signInExistingVerifiedUser(context, email, trimmedPassword)) {
                is SignInExistingUserOutcome.Verified -> {
                    SendEmailVerificationOutcome.AlreadyVerified(
                        user = signIn.user,
                        email = signIn.email
                    )
                }
                is SignInExistingUserOutcome.NotYetVerified -> {
                    runCatching {
                        signIn.user.sendEmailVerification()
                            .addOnFailureListener { err -> Log.w(TAG, "recovery sendEmailVerification failed: ${err.message}") }
                            .await()
                    }
                    SendEmailVerificationOutcome.Sent(signIn.email)
                }
                is SignInExistingUserOutcome.InvalidCredentials -> {
                    SendEmailVerificationOutcome.EmailAlreadyInUse(
                        context.getString(R.string.auth_email_error_already_in_use_wrong_pw)
                    )
                }
                else -> {
                    SendEmailVerificationOutcome.EmailAlreadyInUse(
                        context.getString(R.string.auth_email_error_already_in_use)
                    )
                }
            }
        }

        return try {
            val currentUser = firebaseAuth.currentUser
            if (currentUser != null && currentUser.email.equals(email, ignoreCase = true)) {
                currentUser.reload()
                    .addOnFailureListener { err -> Log.w(TAG, "reload user failed: ${err.message}") }
                    .await()
                val refreshed = firebaseAuth.currentUser ?: currentUser
                if (refreshed.isEmailVerified) {
                    return SendEmailVerificationOutcome.AlreadyVerified(
                        user = refreshed,
                        email = refreshed.email ?: email
                    )
                }
                refreshed.sendEmailVerification()
                    .addOnFailureListener { err -> Log.w(TAG, "sendEmailVerification failed: ${err.message}") }
                    .await()
                return SendEmailVerificationOutcome.Sent(refreshed.email ?: email)
            }

            if (isResend) {
                return SendEmailVerificationOutcome.Error(context.getString(R.string.auth_verify_no_session))
            }

            // Create Firebase Auth user for this email and immediately send standard verification email
            val authResult = firebaseAuth
                .createUserWithEmailAndPassword(email, trimmedPassword)
                .addOnFailureListener { err -> Log.w(TAG, "createUserWithEmailAndPassword failed: ${err.message}") }
                .await()
            val createdUser = authResult.user
                ?: return SendEmailVerificationOutcome.Error(context.getString(R.string.auth_verify_error_generic))

            createdUser.sendEmailVerification()
                .addOnFailureListener { err -> Log.w(TAG, "sendEmailVerification failed: ${err.message}") }
                .await()
            SendEmailVerificationOutcome.Sent(createdUser.email ?: email)
        } catch (e: CancellationException) {
            throw e
        } catch (_: FirebaseAuthUserCollisionException) {
            Log.w(TAG, "Email already registered in Firebase Auth; attempting verified profile recovery")
            recoverExistingEmailAccount()
        } catch (_: FirebaseNetworkException) {
            Log.w(TAG, "sendStandardVerificationEmail: network unavailable")
            SendEmailVerificationOutcome.Error(context.getString(R.string.auth_error_network))
        } catch (e: FirebaseAuthException) {
            Log.w(TAG, "sendStandardVerificationEmail failed with code=${e.errorCode}")
            when (e.errorCode) {
                "ERROR_OPERATION_NOT_ALLOWED" -> SendEmailVerificationOutcome.ProviderNotEnabled(
                    context.getString(R.string.auth_email_error_provider_disabled)
                )
                "ERROR_INVALID_EMAIL" -> SendEmailVerificationOutcome.Error(
                    context.getString(R.string.auth_email_error_invalid)
                )
                "ERROR_TOO_MANY_REQUESTS" -> SendEmailVerificationOutcome.Error(
                    context.getString(R.string.auth_email_error_rate_limit)
                )
                "ERROR_EMAIL_ALREADY_IN_USE" -> recoverExistingEmailAccount()
                else -> SendEmailVerificationOutcome.Error(
                    context.getString(
                        R.string.auth_email_send_failed,
                        e.localizedMessage ?: e.errorCode
                    )
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "sendStandardVerificationEmail error: ${e.javaClass.simpleName}", e)
            SendEmailVerificationOutcome.Error(
                context.getString(
                    R.string.auth_email_send_failed,
                    e.localizedMessage ?: context.getString(R.string.auth_error_network)
                )
            )
        }
    }

    /**
     * Sends a standard Firebase Password Reset email via
     * FirebaseAuth.sendPasswordResetEmail(email.trim()) with safe initialization & failure handling.
     */
    suspend fun sendPasswordResetEmail(
        context: Context,
        rawEmail: String
    ): SendPasswordResetOutcome {
        val trimmedEmail = rawEmail.trim()
        if (trimmedEmail.isEmpty()) {
            return SendPasswordResetOutcome.Error(
                context.getString(R.string.auth_forgot_password_empty_email)
            )
        }
        if (!isValidEmail(trimmedEmail)) {
            return SendPasswordResetOutcome.Error(
                context.getString(R.string.auth_email_error_invalid)
            )
        }

        val firebaseAuth = FirebaseSafeProvider.getAuth(context)
            ?: return SendPasswordResetOutcome.Error(context.getString(R.string.msg_firebase_not_ready))

        return try {
            firebaseAuth.sendPasswordResetEmail(trimmedEmail)
                .addOnFailureListener { err -> Log.w(TAG, "sendPasswordResetEmail failed: ${err.message}") }
                .await()
            SendPasswordResetOutcome.Sent(trimmedEmail)
        } catch (e: CancellationException) {
            throw e
        } catch (_: FirebaseAuthInvalidUserException) {
            Log.w(TAG, "sendPasswordResetEmail: user not found")
            SendPasswordResetOutcome.Error(
                context.getString(R.string.auth_forgot_password_user_not_found)
            )
        } catch (_: FirebaseNetworkException) {
            Log.w(TAG, "sendPasswordResetEmail: network unavailable")
            SendPasswordResetOutcome.Error(
                context.getString(R.string.auth_error_network)
            )
        } catch (e: FirebaseAuthException) {
            Log.w(TAG, "sendPasswordResetEmail failed with code=${e.errorCode}")
            when (e.errorCode) {
                "ERROR_USER_NOT_FOUND" -> SendPasswordResetOutcome.Error(
                    context.getString(R.string.auth_forgot_password_user_not_found)
                )
                "ERROR_INVALID_EMAIL" -> SendPasswordResetOutcome.Error(
                    context.getString(R.string.auth_email_error_invalid)
                )
                "ERROR_TOO_MANY_REQUESTS" -> SendPasswordResetOutcome.Error(
                    context.getString(R.string.auth_email_error_rate_limit)
                )
                "ERROR_OPERATION_NOT_ALLOWED" -> SendPasswordResetOutcome.Error(
                    context.getString(R.string.auth_email_error_provider_disabled)
                )
                else -> SendPasswordResetOutcome.Error(
                    context.getString(
                        R.string.auth_forgot_password_failed,
                        e.localizedMessage ?: e.errorCode
                    )
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "sendPasswordResetEmail error: ${e.javaClass.simpleName}", e)
            SendPasswordResetOutcome.Error(
                context.getString(
                    R.string.auth_forgot_password_failed,
                    e.localizedMessage ?: context.getString(R.string.auth_error_network)
                )
            )
        }
    }

    /**
     * Reloads the active Firebase Auth user and checks user.isEmailVerified == true.
     */
    suspend fun checkEmailVerificationStatus(
        context: Context,
        expectedEmail: String
    ): CheckEmailVerificationOutcome {
        val cleanExpected = normalizeEmail(expectedEmail)
        val firebaseAuth = FirebaseSafeProvider.getAuth(context)
            ?: return CheckEmailVerificationOutcome.Error(context.getString(R.string.msg_firebase_not_ready))

        val currentUser = runCatching { firebaseAuth.currentUser }.getOrNull()
            ?: return CheckEmailVerificationOutcome.NoActiveSession(
                context.getString(R.string.auth_verify_no_session)
            )

        return try {
            currentUser.reload()
                .addOnFailureListener { err -> Log.w(TAG, "reload verification status failed: ${err.message}") }
                .await()
            val refreshed = firebaseAuth.currentUser ?: currentUser
            val userEmail = normalizeEmail(refreshed.email.orEmpty())
            if (cleanExpected.isNotEmpty() && userEmail.isNotEmpty() && userEmail != cleanExpected) {
                return CheckEmailVerificationOutcome.NoActiveSession(
                    context.getString(R.string.auth_verify_no_session)
                )
            }

            if (refreshed.isEmailVerified) {
                CheckEmailVerificationOutcome.Verified(
                    user = refreshed,
                    email = refreshed.email ?: cleanExpected
                )
            } else {
                CheckEmailVerificationOutcome.NotYetVerified(
                    context.getString(
                        R.string.auth_verify_not_yet_verified,
                        refreshed.email ?: cleanExpected
                    )
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: FirebaseNetworkException) {
            Log.w(TAG, "checkEmailVerificationStatus: network unavailable")
            CheckEmailVerificationOutcome.Error(context.getString(R.string.auth_error_network))
        } catch (e: FirebaseAuthException) {
            Log.w(TAG, "checkEmailVerificationStatus failed with code=${e.errorCode}")
            CheckEmailVerificationOutcome.Error(
                context.getString(
                    R.string.auth_verify_error_detail,
                    e.localizedMessage ?: e.errorCode
                )
            )
        } catch (e: Exception) {
            Log.w(TAG, "checkEmailVerificationStatus error: ${e.javaClass.simpleName}", e)
            CheckEmailVerificationOutcome.Error(
                context.getString(
                    R.string.auth_verify_error_detail,
                    e.localizedMessage ?: context.getString(R.string.auth_error_network)
                )
            )
        }
    }

    /**
     * Safely refreshes an existing persisted Firebase Auth session in the background when online
     * so cloud listeners stay healthy without blocking offline username+password login.
     */
    suspend fun syncBackgroundFirebaseSession(
        context: Context,
        expectedEmail: String
    ): Boolean {
        val firebaseAuth = FirebaseSafeProvider.getAuth(context) ?: return false
        val currentUser = runCatching { firebaseAuth.currentUser }.getOrNull() ?: return false
        return try {
            currentUser.reload()
                .addOnFailureListener { err -> Log.w(TAG, "Background session reload failed: ${err.message}") }
                .await()
            val refreshed = firebaseAuth.currentUser ?: currentUser
            val normalizedExpected = normalizeEmail(expectedEmail)
            val actualEmail = normalizeEmail(refreshed.email.orEmpty())
            normalizedExpected.isEmpty() || actualEmail.isEmpty() || actualEmail == normalizedExpected
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Background Firebase session sync skipped safely: ${e.javaClass.simpleName}")
            false
        }
    }

    suspend fun performFirebaseSignOut(context: Context) {
        withContext(NonCancellable) {
            runCatching {
                FirebaseSafeProvider.getAuth(context)?.signOut()
            }.onFailure { err ->
                Log.w(TAG, "performFirebaseSignOut failed safely", err)
            }
        }
    }
}
