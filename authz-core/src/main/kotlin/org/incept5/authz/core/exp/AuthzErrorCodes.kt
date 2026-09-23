package org.incept5.authz.core.exp

import org.incept5.error.ErrorCode

enum class AuthzErrorCodes(private val code: String) : ErrorCode {

    PERMISSION_DENIED("authz.permission_denied"),
    INVALID_TOKEN("authz.invalid_token"),

    // Deliberately the bare "MFA_REQUIRED" rather than an "authz."-prefixed code: consuming portals
    // and clients branch on this exact string (story AC5-AC7 require errors[0].code == "MFA_REQUIRED").
    MFA_REQUIRED("MFA_REQUIRED"),
    ;


    override fun getCode(): String {
        return code
    }


}