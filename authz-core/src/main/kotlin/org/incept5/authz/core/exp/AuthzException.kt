package org.incept5.authz.core.exp

import org.incept5.error.CoreException
import org.incept5.error.ErrorCategory

open class AuthzException(code: AuthzErrorCodes, msg : String) : CoreException(
    category = ErrorCategory.AUTHORIZATION,
    errors = listOf(code.toError()),
    message = msg
)