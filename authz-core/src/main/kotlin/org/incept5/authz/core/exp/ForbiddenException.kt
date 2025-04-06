package org.incept5.authz.core.exp

open class ForbiddenException (msg: String) : AuthzException(AuthzErrorCodes.PERMISSION_DENIED, msg)