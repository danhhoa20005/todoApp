package com.example.appmanagement.util

// Holds global state flags for the application session
object AppGlobals {
    // Tracks whether any user session is active
    var isLoggedIn: Boolean = false
    // Stores the identifier of the authenticated user
    var currentUserId: Long? = null
}
