# SAST Remediation Completion Report

## Remediation summary

| ID | Finding | Remediation | Relevant files |
|---|---|---|---|
| V1 | SQL Injection (CWE-89) | Employee search uses a bound JDBC parameter instead of SQL string concatenation. | `src/main/java/.../service/EmployeeService.java` |
| V2 | Weak Cryptography (CWE-327) | Passwords use BCrypt through an injected `PasswordEncoder` bean. | `build.gradle`; `src/main/java/.../config/PasswordConfig.java` (created); `src/main/java/.../service/AuthService.java` |
| V3 | Hardcoded Secret (CWE-798) | The unused API-secret/token feature was removed instead of replacing it with a fallback secret. | `src/main/java/.../controller/AuthController.java`; `src/main/java/.../service/AuthService.java`; `src/main/java/.../config/SecurityConfig.java` (removed) |
| V4 | Sensitive Data in Logs (CWE-532) | Login credentials and request data are not logged. | `src/main/java/.../controller/AuthController.java` |
| V5 | Path Traversal (CWE-22) | The normalized path and its resolved real path must remain under the real report directory. Download names use only the final path component. | `src/main/java/.../controller/ReportController.java` |
| V6 | Verbose Error Disclosure (CWE-209) | Clients receive a generic error and correlation ID; diagnostic details remain server-side. | `src/main/java/.../controller/GlobalExceptionHandler.java` |
| V7 | IDOR (CWE-639) | Employee endpoints return explicit response types that omit salary and national ID. Public creation also uses a request type that does not bind those fields. | `src/main/java/.../controller/EmployeeController.java` |
| V8 | Reflected XSS (CWE-79) | The message is HTML-escaped and the response includes a restrictive Content-Security-Policy header. | `src/main/java/.../controller/StatusController.java` |

## Detailed developer summary

### V1 — SQL Injection

- **What the vulnerability means:** The previous search code joined user input directly into SQL. A specially crafted search value could change the meaning of the database query, potentially exposing or modifying data.
- **How the change solves it:** The search term is passed as a JDBC parameter. The database treats it as data, not executable SQL.
- **Developer notes:** The `%` wildcards are intentionally added by the application to preserve partial-name searching. This approach works with H2 and other JDBC databases. If search syntax or database portability changes later, retain parameter binding.

### V2 — Weak Cryptography

- **What the vulnerability means:** MD5 is fast and obsolete for passwords, making stolen password hashes much easier to crack.
- **How the change solves it:** BCrypt deliberately performs salted, computationally expensive password hashing. Each password receives a different encoded hash, and `matches` verifies it without re-hashing manually.
- **Developer notes:** The `spring-security-crypto` dependency is sufficient; the application does not need the full Spring Security web filter stack just to hash passwords. BCrypt cost should be reviewed as hardware changes, and existing MD5 hashes would require a migration strategy rather than being silently treated as BCrypt hashes.

### V3 — Hardcoded Secret

- **What the vulnerability means:** A secret in source control can be copied by anyone with repository or artifact access and is difficult to rotate safely.
- **How the change solves it:** The unused API-secret/token feature was removed, so the application no longer has a source-controlled or fallback signing secret.
- **Developer notes:** Login currently returns only a success message; it does not create an authenticated session or token. If token-based authentication is added later, load the signing key from environment-specific secret management and fail startup when it is missing. Do not add a predictable development fallback.

### V4 — Sensitive Data in Logs

- **What the vulnerability means:** Passwords or request bodies in logs can be copied into centralized logging systems and retained or viewed by people who should not access credentials.
- **How the change solves it:** Login credentials and request bodies are not logged.
- **Developer notes:** This behavior is independent of the logging provider and applies across local files, containers, and hosted logging platforms. If diagnostic logging is added later, use an allowlist of safe fields and consider log-injection protections for user-controlled values.

### V5 — Path Traversal

- **What the vulnerability means:** A filename containing path-navigation segments could previously make the application read files outside the reports directory. Symlinks or Windows reparse points can create the same escape even when the textual path looks safe.
- **How the change solves it:** The requested path is normalized, resolved to a real path, and checked against the real report directory before reading. Only the final filename is used in the download header.
- **Developer notes:** `REPORT_DIR` is currently hardcoded as `C:/reports/`, so it is Windows-specific and must be externalized or changed for Linux/container deployments. The directory must exist because `toRealPath()` resolves an existing filesystem path. The process also needs read permission for the directory and files.

### V6 — Verbose Error Disclosure

- **What the vulnerability means:** Returning exception classes, database messages, or filesystem paths gives attackers internal details that can help them plan further attacks.
- **How the change solves it:** Clients receive a stable generic error and correlation ID, while diagnostics remain on the server.
- **Developer notes:** Correlation IDs should be included in log aggregation and request tracing if the application is deployed behind a proxy or distributed across services. Keep server-side logs access-controlled and avoid adding sensitive exception details to production responses.

### V7 — Insecure Direct Object Reference

- **What the vulnerability means:** Anyone could change an employee ID in the URL and retrieve another employee's salary and national ID without proving they were allowed to see them.
- **How the change solves it:** The public profile returns only ID and name. Create, list, search, and get-by-ID also return explicit response types rather than database entities, so none can include salary or national ID. Public create requests do not bind those fields to the database entity.
- **Developer notes:** This is intentional data minimization, not a complete private-profile authorization system. If salary or national ID must later be displayed, add real authentication and enforce an owner or administrator authorization rule on the server. Never treat the presence of an arbitrary request header as authentication.

### V8 — Reflected XSS

- **What the vulnerability means:** The previous endpoint inserted a request parameter directly into HTML, allowing an attacker to make a victim's browser execute injected JavaScript.
- **How the change solves it:** `HtmlUtils.htmlEscape` converts HTML-significant characters into harmless entities before the value is inserted into the page. The Content-Security-Policy header adds another browser-side restriction.
- **Developer notes:** The escaping is appropriate because the value is placed in HTML text content. If the value is later placed in an HTML attribute, JavaScript, CSS, URL, or JSON context, use encoding designed for that context instead. The CSP is supported by modern browsers and should be tested if future pages require scripts or external assets.

## Design decisions

- The application does not claim to authenticate users until a complete authentication flow is implemented.
- No arbitrary `Authorization` header is treated as proof of identity.
- No predictable fallback secret is used.
- BCrypt is provided through Spring dependency injection using the smaller `spring-security-crypto` dependency; the application does not enable a global `permitAll` security filter.
- Salary and national ID remain in the persistence model but are excluded from all public employee responses.

## Files changed or created

### Application files

- `build.gradle`
- `src/main/java/com/accenture/sast/tracker/config/PasswordConfig.java`
- `src/main/java/com/accenture/sast/tracker/service/AuthService.java`
- `src/main/java/com/accenture/sast/tracker/service/EmployeeService.java`
- `src/main/java/com/accenture/sast/tracker/controller/AuthController.java`
- `src/main/java/com/accenture/sast/tracker/controller/EmployeeController.java`
- `src/main/java/com/accenture/sast/tracker/controller/ReportController.java`
- `src/main/java/com/accenture/sast/tracker/controller/GlobalExceptionHandler.java`
- `src/main/java/com/accenture/sast/tracker/controller/StatusController.java`

### Removed files

- `src/main/java/com/accenture/sast/tracker/config/SecurityConfig.java` — removed because it permitted every request and disabled CSRF without providing authentication.

## Validation

Run:

```powershell
.\gradlew.bat test
.\gradlew.bat build
```

The V7 API response fields are:

- Profile: `id` and `name`.
- Create, list, search, and get-by-ID: `id`, `name`, `role`, `email`, and `onboardingDate`.

