package org.incept5.authz.core.access

interface InvocationContext {

    fun args(): Array<Any>

    @Suppress("UNCHECKED_CAST")
    fun <E> firstArg(): E = args()[0] as E

    @Suppress("UNCHECKED_CAST")
    fun <E> secondArg(): E = args()[1] as E

    @Suppress("UNCHECKED_CAST")
    fun <E> thirdArg(): E = args()[2] as E

    @Suppress("UNCHECKED_CAST")
    fun <E> fourthArg(): E = args()[3] as E

    fun <E> firstOfType(cls: Class<E>): E {
        for (arg in args()) {
            if (cls.isInstance(arg)) {
                return cls.cast(arg)
            }
        }
        throw Exception("No arg of type ${cls.simpleName} found")
    }
}
