# Updated security assessment

2026-11-23. All active tests used synthetic credentials and isolated databases/homes; no real financial or credential records were read or changed.

## Executed controls

- OPERATOR cannot invoke protected financial, plan/subscription, backup/restore or configuration mutations (RI17). OPERATOR can register/check in a member (RI18). Backend repositories/services enforce the boundary, with bilingual UI approval prompts. Super Admin approval expires after 15 minutes by implementation; expiry/prompt interactions remain manual. No staff-account or per-person audit system is claimed.
- BCrypt cost 12 retained; setup cannot reset a configured password; password change requires current password, rejects over-72-byte values and revokes approval. Atomic mode-0600 writes tested on POSIX; Windows ACL acceptance pending (RI21/22/37).
- New configuration saves never serialize database passwords; environment/session-only secret handling and private atomic file persistence tested (RI19/37). Legacy plaintext remains readable for compatibility and must be retired by the administrator during rollout. No real credential modified.
- TLS enabled by default; MySQL VERIFY_IDENTITY and SQL Server certificate validation when enabled. Real remote TLS connections and existing explicitly disabled settings need operator review (RI20).
- Per-connection SQLite FK, phone uniqueness and database payment/attendance ownership guards tested. Invalid legacy conflicts fail safely without record deletion (RI01-RI08).
- Recovery input corruption, wrong app/schema, unsupported versions, unknown triggers, missing columns/guards/indexes and active managed connections are rejected. Verified pre-restore snapshots recover financial data (RI09-RI15/26/31/32/36). Recovery exits the desktop after success.
- Existing SQL-injection search case, password negatives, licensing tamper checks and bilingual PDF/XLSX regressions pass in the 35-case workflow harness.

## Runtime dependency advisory scan

OSV querybatch was executed for 30 resolved Maven runtime package/version pairs. Before: 3 affected versions. After targeted patch updates: **0 matched affected versions**. The raw input/replies and before/after evidence are in evidence/release-fix/osv-*.json and runtime-dependencies.txt. Classifier-specific JavaFX packages are deduplicated by Maven coordinate/version. This does not scan the bundled OS/JDK, installer tooling, build/test dependencies, new unpublished issues or native code independently, and is not proof of absence of all exploitable vulnerabilities.

| Advisory | Previous | Corrected |
|---|---|---|
| [SQL Server JDBC CVE-2025-59250](https://github.com/advisories/GHSA-m494-w24q-6f7w) | 12.10.1.jre11 | 12.10.2.jre11 |
| [Commons Lang CVE-2025-48924](https://github.com/advisories/GHSA-j288-q9x7-2f5v) | 3.16.0 | 3.18.0 |
| [Log4j API CVE-2026-49844](https://logging.apache.org/security.html#CVE-2026-49844) | 2.24.3 | 2.25.5 |

Publisher references: [Microsoft security update](https://msrc.microsoft.com/update-guide/vulnerability/CVE-2025-59250), [Apache Commons Lang advisory](https://github.com/apache/commons-lang/security/advisories/GHSA-j288-q9x7-2f5v), and the Apache Log4j security notice above. Dependencies were updated without changing POI/OpenPDF application APIs; clean build and both regression suites were rerun. Application-level exploitability was not asserted solely from a version match; vulnerable versions were patched regardless.

## Remaining security/operational acceptance

Approve the local write-authorization policy and financial-read visibility; execute real MySQL/SQL Server authorization/TLS/migration tests; verify Windows profile ACLs/native drivers; test UI approval cancellation/timeout; complete log-redaction and full asset-backup drills. Protect source/config/DB with OS accounts: in-process roles cannot resist arbitrary code or file access as the same OS user. Local license signing remains casual tamper detection, not server-backed entitlement security. Do not treat native desktop functionality as web session/CSRF testing. No full penetration-test certification is issued.
