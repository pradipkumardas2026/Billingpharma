package com.example.ui.viewmodel

sealed interface UserRole {
    object Unauthenticated : UserRole
    data class Admin(val mobile: String, val isMasterAdmin: Boolean = true, val adminName: String = "Admin") : UserRole
    data class Guest(val mobile: String) : UserRole
}

enum class NavigationTab {
    HOME,
    INVENTORY,
    NEW_BILL,
    SALES,
    SALES_HISTORY,
    ALERTS,
    SETTINGS,
    GST
}

data class AuthState(
    val role: UserRole = UserRole.Unauthenticated,
    val isOtpSent: Boolean = false,
    val currentGeneratedOtp: String = "",
    val otpTargetMobile: String = ""
) {
    val isLoggedIn: Boolean get() = role !is UserRole.Unauthenticated
    val isAuthenticated: Boolean get() = isLoggedIn
    val isAdmin: Boolean get() = role is UserRole.Admin
    val isMasterAdmin: Boolean get() = (role as? UserRole.Admin)?.isMasterAdmin == true
    val isGuest: Boolean get() = role is UserRole.Guest
}

sealed class ForgotPasswordResult {
    data class MasterAdminOtp(val otp: String, val targetMobile: String) : ForgotPasswordResult()
    data class SubAdminNotice(val subAdminName: String, val mobile: String) : ForgotPasswordResult()
    object NotFound : ForgotPasswordResult()
}
