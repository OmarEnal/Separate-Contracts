package com.example.separatecontacts

data class Contact(
    val id: String = "",
    val name: String = "",
    val phoneNumber: String = "",
    val email: String = "",
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    // No-argument constructor for Firebase
    constructor() : this("", "", "", "", "", 0)
}

