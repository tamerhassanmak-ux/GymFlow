# Current defect disposition

Updated 2026-11-23. The previous report is preserved in evidence/release-fix/BUG_REPORT-before.md. FIXED below means the named locally testable behavior was corrected and its regression passed. It does not certify unexecuted server-engine or Windows behavior.

| ID | Severity | Root cause / correction | Current evidence and disposition |
|---|---|---|---|
| QC-001 | HIGH | DatabaseManager initialized FK only on a discarded connection; now every SQLite connection enables FK/busy timeout | FIXED; RI02, DB-003/004/008, PAY-006 |
| QC-002 | HIGH | Independent member/subscription FKs permitted cross-owner payment; write validation plus migration 011 database ownership guards now protect payments, attendance and owner reassignment | FIXED on SQLite; RI03/04/06, PAY-007/008; server composite constraints BLOCKED pending server tests |
| QC-003 | MEDIUM | Null payment update dereferenced before validation; validation reordered and direct null guards added to other financial repositories | FIXED; RI05, PAY-009 |
| QC-004 | HIGH | Phone migration unregistered/collided with 008; new migration 010 preflights duplicates and enforces unique nonblank trimmed phone | FIXED on SQLite; RI01/07/08, DB-009. Existing conflicts are preserved for administrator review, not merged/deleted |
| QC-005 | HIGH | Files.copy omitted committed WAL data; SqliteBackupService now uses native consistent online snapshot and atomic staged publication | FIXED; RI09/14, validated backup/restore tests |
| QC-006 | HIGH | Restore blindly replaced live database; now input is staged/read-only, schema/integrity/relationships/constraints/triggers validated, safety snapshot retained, native transactional restore under exclusive managed access | FIXED for tested local conditions; RI10-RI15/26/31/32/36. External writer/power-loss/Windows acceptance pending |
| QC-007 | HIGH | Database password serialized into properties; new saves omit secret, use environment/session memory and atomic private properties files | FIXED for new saves; RI19/37. Existing plaintext files are read compatibly; administrator provisioning/save required during rollout. No production file changed |
| QC-008 | MEDIUM | TLS disabled by default; new default enables certificate validation, MySQL VERIFY_IDENTITY | FIXED default; RI20; real server TLS/certificates BLOCKED. Existing explicit false settings remain a deployment risk |
| QC-009 | MEDIUM | Relative attachment destination failed in protected install directories and name collisions overwrote assets; now AppPaths storage plus UUID; deletion checks real managed paths | FIXED storage behavior; RI23. Legacy paths readable; attachment viewer and complete asset-backup drill pending |
| QC-010 | MEDIUM | Build could publish without tests; Maven JUnit/Surefire and portable legacy harness are now CI gates | FIXED configuration/local execution; 44 JUnit + 35 legacy pass. Windows CI was not triggered |
| QC-011 | LOW | Duplicate translation keys silently overrode values; earlier duplicate declarations removed preserving final effective values | FIXED; 422 matching keys each locale, no duplicates/missing keys |
| QC-012 | LOW | Deprecated Locale and unchecked varargs calls | FIXED; full javac -Xlint:all reports zero warnings/errors |
| QC-013 | HIGH | Packaged SQLite module could not read legacy slf4j.api; explicit module read configured in jpackage | FIXED Linux startup; final image initializes SQLite and migrates. Windows runtime pending |
| QC-014 | HIGH | No backend financial authorization boundary and setup API could reset existing admin password; introduces OPERATOR/SUPER_ADMIN write checks and current-password-verified change | FIXED for implemented local policy; RI17/18/21/22. Policy approval, staff-account needs and interactive expiry acceptance pending |
| QC-015 | HIGH | Payment commit preceded separate installment update; ambient unit of work now groups create/edit/delete and schedule reconciliation; runtime DDL moved to migration 012; cross-database joining rejected | FIXED local transactional behavior; RI16/25/27/28/29/30/35 |
| QC-016 | MEDIUM | double discount subtraction produced floating-point artifacts; NaN bypassed negative checks | FIXED: decimal two-place HALF_UP calculation, finite-value validation and decimal JDBC binding; RU06/07, RI33 |
| QC-017 | HIGH (maximum) | Runtime dependency scan matched SQL Server JDBC, Commons Lang and Log4j API to published advisories | Patched to 12.10.2.jre11 / 3.18.0 / 2.25.5; repeat OSV query: zero matches across 30 runtime package versions; full regression repeated |

Exact file/method changes and tests are in CHANGELOG_RELEASE_FIX.md. Current cases/results are in TEST_CASES.md; raw evidence is under evidence/release-fix. Known automatic failures are zero; missing external/manual evidence remains a release blocker.

One intermediate legacy-harness rerun failed because a mechanical migration-count edit accidentally changed its expected discount from 90 to 120. The fixture expectation was corrected to the original 90; the production discount rule was not changed to satisfy it. That intermediate result remains in legacy-harness.log. All 35 current legacy cases pass. Initial Maven dependency resolution also required authorized cache/network access; no tests were claimed passed during that setup failure.
