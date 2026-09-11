package com.example.utils

object ErrorMapper {
    fun getUserFriendlyMessage(e: Throwable): String {
        val msg = e.message ?: return "Something went wrong. Please try again."
        
        return when {
            msg.contains("SOFT_DELETED_DUE_TO_FK") -> "This product is used in past orders, so it was archived instead."
            msg.contains("row-level security policy") -> "Unable to save this right now. Please try again."
            msg.contains("Failed to connect") || msg.contains("UnknownHostException") -> "Unable to connect. Check your internet connection and try again."
            msg.contains("Ordering is currently closed") -> "Ordering is currently closed."
            msg.contains("is currently unavailable") -> "This item is currently unavailable."
            msg.contains("Payment failed") -> "Payment failed. Please try again."
            msg.contains("Payment cancelled") -> "Payment cancelled."
            msg.contains("Payment could not be verified") -> "Payment could not be verified. Please contact support before trying again."
            msg.contains("Checkout failed") -> "Order could not be placed. Please try again."
            msg.contains("JWT") || msg.contains("Session expired") || msg.contains("Unauthorized") -> "Your session expired. Please sign in again."
            msg.contains("Failed to upload image") -> "Image upload failed. Please try again."
            else -> "Something went wrong. Please try again."
        }
    }
}
