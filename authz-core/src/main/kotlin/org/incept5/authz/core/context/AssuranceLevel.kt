package org.incept5.authz.core.context

/**
 * How strongly the principal's session was authenticated — provider-neutral.
 *
 * A token-exchange plugin maps its own identity provider's assurance claim onto this enum, so
 * nothing in authz-lib needs to know any provider's claim names or vocabulary. Enforcement
 * ([org.incept5.authz.quarkus.filter.AssuranceLevelFilter]) reasons only about this enum.
 */
enum class AssuranceLevel {
    /** Password/single-credential session (or an unknown/absent assurance claim). */
    SINGLE_FACTOR,

    /** A verified second factor was presented (e.g. TOTP). */
    MULTI_FACTOR,
}
