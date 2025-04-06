package org.incept5.authz.core.access

import org.incept5.authz.core.context.AuthzContextProvider

interface AccessControlContext : org.incept5.authz.core.access.InvocationContext, AuthzContextProvider