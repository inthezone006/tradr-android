package com.rahul.stocksim.model

import com.google.firebase.Timestamp

data class Portfolio(
    val id: String = "default",
    val name: String = "Main Portfolio",
    val createdAt: Timestamp = Timestamp.now(),
    val isDefault: Boolean = true,
    val balance: Double = 0.0
)
