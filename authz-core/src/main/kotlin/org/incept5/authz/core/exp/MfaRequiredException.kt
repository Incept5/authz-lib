package org.incept5.authz.core.exp

/**
 * Thrown when a principal holding a role that requires multi-factor authentication presents a
 * single-factor session for an endpoint that is not on the MFA-skip list.
 *
 * Extends [AuthzException], so it maps (via error-lib) to **HTTP 403** with
 * `errors[0].code == "MFA_REQUIRED"`. 403 rather than 401 is deliberate: consuming portals treat a
 * 401 as a sign-out, whereas a 403 lets them route the user to a second-factor challenge.
 */
class MfaRequiredException(
    msg: String = "A multi-factor authenticated session is required for this role",
) : AuthzException(AuthzErrorCodes.MFA_REQUIRED, msg)
