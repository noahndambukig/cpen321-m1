package com.noah.demo.data

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.delay

/** Who is signed in, as M1 wants it displayed: first and last name. */
data class SignedInUser(
    val firstName: String,
    val lastName: String,
    val email: String,
) {
    val displayName: String get() = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ")
}

sealed interface SignInResult {
    data class Success(val user: SignedInUser) : SignInResult
    data object Cancelled : SignInResult

    /** [retryable] marks failures that may be a slow-provider timeout rather than a real "no account". */
    data class Failed(val message: String, val retryable: Boolean = false) : SignInResult
}

/**
 * Google Sign-In through Credential Manager and the Google Identity library.
 *
 * Deliberately not Firebase Auth, which the Project Description forbids as a
 * services for major functionality such as authentication.
 */
object GoogleAuth {

    private const val RETRY_DELAY_MS = 800L

    /**
     * @param serverClientId the **Web** OAuth client ID, not the Android one. The
     * Android client only authorises this app's signing certificate; passing it
     * here fails with an opaque error.
     */
    suspend fun signIn(context: Context, serverClientId: String): SignInResult {
        if (serverClientId.isBlank()) {
            return SignInResult.Failed(
                "No GOOGLE_CLIENT_ID configured. Set the Web client ID in local.properties.",
            )
        }

        // On a slow device Play Services can take longer to answer than the
        // CredentialManager framework waits (~3s), which surfaces as a spurious
        // NoCredentialException. Observed on a 2 GB emulator: the provider
        // succeeded at ~5s, well after the framework had cancelled. One retry
        // costs little and turns that flake into a successful sign-in.
        val first = attemptSignIn(context, serverClientId)
        if (first is SignInResult.Failed && first.retryable) {
            delay(RETRY_DELAY_MS)
            return attemptSignIn(context, serverClientId)
        }
        return first
    }

    private suspend fun attemptSignIn(context: Context, serverClientId: String): SignInResult {

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(
                GetGoogleIdOption.Builder()
                    .setServerClientId(serverClientId)
                    // Show every account, not just ones already used with this app,
                    // so a grader signing in for the first time still sees a picker.
                    .setFilterByAuthorizedAccounts(false)
                    .build(),
            )
            .build()

        return try {
            val response = CredentialManager.create(context).getCredential(context, request)
            val credential = GoogleIdTokenCredential.createFrom(response.credential.data)

            SignInResult.Success(
                SignedInUser(
                    firstName = credential.givenName.orEmpty(),
                    lastName = credential.familyName.orEmpty(),
                    email = credential.id,
                ),
            )
        } catch (e: GetCredentialCancellationException) {
            SignInResult.Cancelled
        } catch (e: NoCredentialException) {
            // Play Services also reports an unregistered signing certificate as
            // "no credential" (the real cause shows as [28444] in logcat), so this
            // message has to cover both causes rather than assert the wrong one.
            SignInResult.Failed(
                "No usable Google account. Add one in Settings, or this build's " +
                    "signing certificate is not registered in the Google console.",
                retryable = true,
            )
        } catch (e: GetCredentialException) {
            SignInResult.Failed(e.message ?: e.javaClass.simpleName)
        }
    }
}
