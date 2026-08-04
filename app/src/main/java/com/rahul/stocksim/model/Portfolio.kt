package com.rahul.stocksim.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Portfolio(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val isDefault: Boolean = false,
    val balance: Double = 0.0
) {
    // Helper to check if it's the primary one, being safe about IDs
    val isPrimary: Boolean get() = isDefault || id == "default"
}
