package org.incept5.authz.core.exp

import org.incept5.error.CoreException
import org.incept5.error.ErrorCategory

class AuthnException(code: AuthzErrorCodes, msg : String) : CoreException(
    category = ErrorCategory.AUTHENTICATION,
    errors = listOf(code.toError()),
    message = msg
)