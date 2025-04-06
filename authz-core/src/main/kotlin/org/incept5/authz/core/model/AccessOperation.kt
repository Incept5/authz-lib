package org.incept5.authz.core.model

enum class AccessOperation {
    CREATE,
    READ,
    UPDATE,
    DELETE,
    ALL, // all is a shorthand which means CREATE + READ + UPDATE + DELETE
    ;

    companion object {
        fun parse(str: String): AccessOperation {
            return valueOf(str.uppercase())
        }
    }

    fun matches(required: AccessOperation): Boolean {
        return this == required || this == ALL
    }
}
