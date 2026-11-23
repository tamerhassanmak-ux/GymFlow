# GymFlow 1.0.4 release report

**Decision: NOT READY**

Updated 2026-11-23. The automatically reproducible defects in the previous assessment have corrections with passing local regression evidence. Production approval remains withheld because mandatory Windows, server-database, UI/hardware, historical-data and operational acceptance evidence is missing. No production deployment or real-data/credential modification occurred.

## Results

| Metric | Result |
|---|---|
| Documented cases | 163 |
| PASS | 132 |
| FAIL | 0 |
| BLOCKED | 2 |
| NOT EXECUTED | 29 |
| JUnit 5 tests | 44 passed; 0 failed/errors/skipped; 18.309 s summed suite time |
| Existing Java workflow cases | 35 passed; 0 failed |
| Bilingual runtime FXML loads | 46 passed: 23 screens × ar/en |
| Compiler lint | 0 errors, 0 warnings across all main Java sources |
| Runtime dependency audit | 30 public Maven package versions queried against OSV; 0 matches after 3 dependency updates |
| Code coverage | Not measured |

Counts refer to current rows in TEST_CASES.md. The extra checks include clean build, static resource checks, compiler lint, diff hygiene, package checks and dependency scan. Blocked cases are MySQL and SQL Server integration; unexecuted cases are manual scenarios. Passing tests have evidence under qa-reports/evidence/release-fix, including Surefire XML, raw commands/results and artifact SHA-256. Historical failing logs are retained and are not presented as current failures.

## Corrected behavior

- Every SQLite application connection enables foreign keys. New migrations enforce unique nonblank phones and ownership of payments/attendance; invalid historical conflicts stop safely without deleting data.
- Payment save/edit/delete and installment reconciliation share one transaction; nested repository operations cannot commit partial workflows. Installment schema now belongs to migrations. Currency discount arithmetic rejects non-finite values and uses decimal rounding.
- Backup captures committed WAL data; restore rejects corrupt/wrong/future/incomplete/altered schema, creates a verified recovery snapshot, uses SQLite transactional restore, refuses active managed connections and exits the desktop afterward.
- Financial/plan/subscription writes, destructive member deletion and administrative mutations require recent Super Admin approval. Routine registration/check-in still works as OPERATOR. This local two-role policy is provisional pending business acceptance; it does not introduce staff accounts or change financial read visibility.
- Password changes require the current password and revoke approval; setup cannot reset an existing password. New DB configuration saves omit passwords, use atomic private files and default network TLS to certificate verification. Legacy plaintext config is read compatibly until an authorized rollout save/provisioning step.
- Attachments use per-user writable storage and unique names; old paths stay readable. Duplicate translations and all prior compiler warnings are removed.
- Maven and CI execute JUnit 5 tests; Windows CI also invokes the portable existing workflow harness. The SQLite packaged-module fix remains configured.
- Patched runtime dependencies: SQL Server JDBC 12.10.2.jre11, Commons Lang 3.18.0, Log4j API 2.25.5. OSV originally matched three vulnerable versions and now returns no matches for the resolved runtime graph. This is an advisory-database check, not proof of absence of all vulnerabilities.

Every code modification and associated tests are explained in CHANGELOG_RELEASE_FIX.md. Deployment/credential/recovery instructions are in RELEASE_OPERATIONS.md. Detailed defects and security limits appear in BUG_REPORT.md and SECURITY_AUDIT.md.

## Release validation

The final clean Maven build uses Java 21 and JavaFX 21.0.6, executes all 44 JUnit tests and packages GymFlow.jar plus runtime libraries. Existing export/payment/expense/subscription/check-in regressions pass. The final Linux app image contains the exact tested JAR and initializes SQLite/completes 12 migrations on a fresh temporary user home. Its timed first-run smoke stops before creating a real account or activating a license. It does not prove full user startup/shutdown or Windows installation.

## Remaining blockers and rollout risks

1. Windows EXE clean install, uninstall, upgrade, native libraries and actual business smoke are NOT EXECUTED. Windows CI configuration was changed but no remote workflow or release was triggered.
2. MySQL/SQL Server integration is BLOCKED: isolated servers/certificates were not available. Their migration branches, DDL retry behavior and financial transactions need actual execution.
3. The 23-screen visual/action matrix, RTL/LTR layout, keyboard/contrast, approval cancellation/expiry, camera/QR/printer and external attachment viewer remain NOT EXECUTED.
4. Approve the local operator/admin financial-write policy, or supply the desired staff roles/account/audit requirements. Backend authorization tests pass for the implemented policy; business acceptance is pending.
5. Historical real-data upgrade and full recovery drill are pending. Duplicate phones, ownership conflicts and invalid schedules intentionally block migration for approved remediation. Existing production data was not audited or rewritten.
6. `.db` backup is database-only. Independently archive attachments/photos/configuration and verify full asset recovery. External-writer, interrupted recovery, power/disk failure and Windows file-lock scenarios remain unverified.
7. Provision remote secrets outside properties and trusted TLS certificates. Existing explicitly disabled TLS and legacy plaintext properties require administrator rollout review; no real credential was changed by QA.
8. Sustained-load/soak testing, operational log-redaction review and deployment-specific security acceptance remain pending. OS-level access still controls direct database/file access.

No missing evidence is treated as PASS. READY requires these mandatory acceptance items, not another successful compilation.
