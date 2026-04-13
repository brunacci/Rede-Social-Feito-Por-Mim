package com.rdsocial.presentation.auth

import android.content.Context
import com.google.firebase.auth.FirebaseAuthException
import com.rdsocial.R

internal fun Throwable.toAuthUserMessage(context: Context): String = when (this) {
    is FirebaseAuthException -> mapFirebaseAuthError(context, errorCode)
    else -> localizedMessage?.takeIf { it.isNotBlank() }
        ?: context.getString(R.string.error_generic)
}

private fun mapFirebaseAuthError(context: Context, code: String?): String = when (code) {
    "ERROR_INVALID_EMAIL" -> context.getString(R.string.error_invalid_email)
    "ERROR_WRONG_PASSWORD",
    "ERROR_INVALID_CREDENTIAL",
    -> context.getString(R.string.error_invalid_credentials)
    "ERROR_USER_NOT_FOUND" -> context.getString(R.string.error_user_not_found)
    "ERROR_USER_DISABLED" -> context.getString(R.string.error_user_disabled)
    "ERROR_EMAIL_ALREADY_IN_USE" -> context.getString(R.string.error_email_in_use)
    "ERROR_WEAK_PASSWORD" -> context.getString(R.string.error_weak_password)
    else -> context.getString(R.string.error_generic)
}
