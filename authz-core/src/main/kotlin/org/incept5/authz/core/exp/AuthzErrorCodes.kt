package org.incept5.authz.core.exp

import org.incept5.error.ErrorCode

enum class AuthzErrorCodes(private val code: String) : ErrorCode {

    PERMISSION_DENIED("authz.permission_denied"),
    INVALID_TOKEN("authz.invalid_token"),
    ;


    override fun getCode(): String {
        return code
    }


}