# ADR-001: Store LDAP Domain in JWT Additional Details

**Date:** 2026-06-17  
**Status:** Accepted

---

## Context

XM UAA supports two authentication paths for human users:

1. **Local (DB) auth** — credentials validated against the internal `User` table.
2. **LDAP / Active Directory auth** — credentials validated against one of the configured AD domains (
   `yourcompanyname.com`). After successful LDAP authentication, `UaaLdapUserDetailsContextMapper` creates or updates
   the corresponding local `User` record and then loads `DomainUserDetails` from the DB.

Because both paths ultimately load the same `DomainUserDetails` from the local DB, downstream code cannot distinguish an
LDAP-originated session from a local one solely by inspecting the token or the security context.

This distinction became necessary to implement LDAP account-status re-validation during OAuth2 token refresh. Without a
way to identify LDAP sessions, the refresh-time LEP would have to query every LDAP domain for every token refresh — even
for local users — which is wasteful and fragile.

`DomainUserDetails.additionalDetails` (`Map<String, String>`) is already serialised into the JWT via
`DomainJwtAccessTokenConverter` under the `additionalDetails` claim and deserialised back during token refresh by
`DefaultAuthenticationRefreshProvider`. It is therefore the appropriate carrier for session-scoped metadata.

---

## Decision

In `UaaLdapUserDetailsContextMapper.mapUserFromContext()`, after loading `DomainUserDetails` from the DB, write the LDAP
domain of the authenticating provider into `additionalDetails`:

```java
DomainUserDetails domainUserDetails = (DomainUserDetails) userDetailsService.loadUserByUsername(username);

domainUserDetails.getAdditionalDetails().put("ldapDomain",ldapConf.getDomain());

return domainUserDetails;
```

**Key properties of this approach:**

| Property              | Detail                                                                                                        |
|-----------------------|---------------------------------------------------------------------------------------------------------------|
| Key name              | `ldapDomain`                                                                                                  |
| Value                 | The domain string from `TenantProperties.Ldap.domain`                                                         |
| Stored in JWT         | Yes — via existing `additionalDetails` claim                                                                  |
| Readable on refresh   | Yes — `DefaultAuthenticationRefreshProvider` copies `additionalDetails` back into the new `DomainUserDetails` |
| Impact on local users | None — key is never written for non-LDAP auth paths                                                           |

---

## Consequences

### Positive

- Downstream code (LEPs, token enrichers) can cleanly detect LDAP sessions without LDAP round-trips.
- Carries the specific domain, enabling per-domain logic (e.g. choosing a different LDAP search template per domain).
- Zero impact on local (DB) users — no key is added, no extra queries, no size overhead.
- No schema changes; `additionalDetails` is an existing, unbounded `Map<String, String>`.

### Negative / Risks

- Tokens issued **before this change** contain no `ldapDomain` claim. For those sessions the refresh-time LDAP check is
  silently skipped until the user re-authenticates and obtains a new token. This is intentional and safe during the
  rollout window.
- The `ldapDomain` value is stored in the JWT payload (not encrypted by default). It is not sensitive, but consumers
  should treat it as informational metadata, not as a security assertion on its own.

---

## Related

- `src/main/java/com/icthh/xm/uaa/security/ldap/UaaLdapUserDetailsContextMapper.java`
- `src/main/java/com/icthh/xm/uaa/security/DomainUserDetails.java`
- `src/main/java/com/icthh/xm/uaa/security/DomainJwtAccessTokenConverter.java`
- `src/main/java/com/icthh/xm/uaa/security/provider/DefaultAuthenticationRefreshProvider.java`
