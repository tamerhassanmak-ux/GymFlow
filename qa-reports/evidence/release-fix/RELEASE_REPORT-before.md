# Release readiness report

**Decision: NOT READY FOR RELEASE**

## Executive summary

GymFlow 1.0.4 builds successfully and has meaningful isolated regression evidence. QC corrected missing SQLite foreign-key enforcement, incorrect payment/subscription ownership validation, a null-update crash and a packaged-runtime module-access failure. Release is refused: duplicate-phone protection is absent at the database layer, backup can omit committed WAL data and restore can replace a valid database with corrupt input. Full role policy, upgrade/recovery, visual/action coverage, Windows artifact acceptance and server database testing remain incomplete. Successful screen loading and compilation do not establish complete functionality.

## Project overview

Java 21 / JavaFX desktop monolith with Maven, JDBC SQLite/MySQL/SQL Server, BCrypt, ZXing, webcam capture, POI and OpenPDF. Scope includes members, plans, subscriptions, attendance, payments/installments, revenues/attachments, expenses, financial reporting, settings, administration and licensing. See QC_TEST_PLAN.md for module/schema inventory.

## Test execution summary

Counts are explicit case rows in TEST_CASES.md, including two primitive recovery reproductions; they do not count assertions or historical reruns twice.

| Metric | Result |
|---|---|
| Total test cases | 118 |
| Passed | 84 |
| Failed | 3 |
| Blocked | 2 |
| Not executed | 29 |
| Skipped automated tests | 0 |
| Critical defects | 0 classified Critical; unresolved data-loss risks are High |
| High defects | 7 total, 3 corrected / 4 open (Windows retest pending) |
| Medium defects | 4 total, 1 fixed / 3 open |
| Low defects | 2 open |
| Code coverage | Not measured |

Java harness: 35 cases, 34 passed / 1 failed (DB-009). Runtime FXML: 46 loads passed, 23 screens x 2 languages. Remaining case results and measurements are in TEST_CASES.md and evidence logs. Original Maven suite contained no tests. Harness execution is explicit and returns nonzero for the remaining verified failure.

## Release acceptance gates

| Gate | Result |
|---|---|
| Clean Java/Maven build | PASS |
| SQLite referential integrity on synthetic fixture | PASS after fix; existing user data not certified |
| Tested financial calculations and rollback | PASS for documented fixtures only |
| All critical functional paths | NOT EXECUTED comprehensively |
| No unresolved high defects | FAIL |
| Backup/recovery validated | FAIL |
| All required automated tests passing | FAIL (DB-009) |
| Authentication | Password API positive/negative PASS; role-based authorization acceptance incomplete |
| Security clearance and complete dependency audit | NOT EXECUTED comprehensively |
| All supported databases/upgrades | BLOCKED / NOT EXECUTED |
| Visual usability and all action workflows | NOT EXECUTED comprehensively |
| Windows installer acceptance | NOT EXECUTED |

## Defects, performance, security and regression

Detailed defect/root-cause/fix evidence: BUG_REPORT.md. Measured timings/RSS/CPU and limitations: PERFORMANCE_REPORT.md. Security evidence and risks: SECURITY_AUDIT.md. Retest commands, clean build and packaging evidence: REGRESSION_REPORT.md.

The corrected Linux app image initializes SQLite and completes all nine migrations. The Windows packaging option was corrected, but the Windows artifact still requires verification.

## Required work before approval

1. Correct backup/restore with a consistent SQLite snapshot, input/schema validation, safe replacement/recovery and coordinated connection shutdown; test WAL, corrupt files and interruptions.
2. Add a new unique phone migration with an explicit existing-duplicate remediation policy and supported-engine upgrade tests. Do not repurpose already-applied version 008 or delete existing records.
3. Complete documented manual business/UI/hardware and recovery cases, all-engine tests, role-policy acceptance and Windows installation/upgrade acceptance.
4. Address plaintext DB credentials and disabled-by-default transport encryption for network deployments; complete dependency vulnerability review.
5. Add the harness or an equivalent maintained JUnit suite to CI; preserve failure visibility. Obtain all required release evidence before approval. No risk acceptance approval was supplied.

This assessment records discovered issues and bounded evidence. It does not certify exhaustive code review, full penetration testing or complete production readiness. No application deployment or production-data modification occurred.
