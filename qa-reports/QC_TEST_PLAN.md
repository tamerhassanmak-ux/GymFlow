# Updated QC and release test plan

Current phase: defect correction and release validation, 2026-11-23. Original discovery below is historical; the changes in this addendum supersede its old dependency versions, nine-migration count, lack-of-tests and open defect statements.

## Current architecture and scope

Java 21/JavaFX 21.0.6/JDBC desktop application. New migration versions 010-012 add nonblank phone uniqueness, financial ownership constraints and managed installment schema. Current runtime patches: SQL Server JDBC 12.10.2.jre11, Commons Lang 3.18.0 and Log4j API 2.25.5. New classes provide validated SQLite recovery, shared transaction scopes, OPERATOR/SUPER_ADMIN approval and atomic configuration persistence. Inventory and each modification are detailed in CHANGELOG_RELEASE_FIX.md.

## Current executed plan

1. Read the prior readiness report, complete case matrix and original plan before code changes; RELEASE_REPORT.md did not previously exist.
2. Preserve pre-existing working-tree edits. Use synthetic isolated databases and credentials. Do not touch real records, credentials, deploy or publish.
3. Correct known recovery/phone/storage/default-TLS/CI/resource-warning defects; retain/retest earlier FK/ownership/null/package fixes.
4. Add 44 JUnit 5 unit/integration/security/recovery cases under src/test/java; preserve and rerun all 35 legacy workflow cases.
5. Verify registration/check-in, subscription lifecycle/renewal/expiry/session boundaries, payments/reports/expenses/revenues, schema constraints, rollback and database recovery. Use failure injection for corrupt schema, authorization denial and partial financial workflows.
6. Run a clean Maven package, complete compiler lint, static resource checks and actual bilingual FXML loading. Test the generated Linux image using only disposable user paths and compare JAR hashes.
7. Resolve the runtime dependency graph, query OSV, patch confirmed affected versions, then rerun all regression/build/runtime checks against the final patched candidate.
8. Update TEST_CASES, defect/security/performance/regression reports and RELEASE_REPORT with actual results. Keep blocked and manual rows; do not approve release without mandatory evidence.

## Acceptance boundaries

Automated local evidence is passing. Real MySQL/SQL Server execution is blocked; Windows installation, full UI/actions/hardware, historical-production-copy upgrades, interrupted/external-writer/full-asset recovery, sustained soak and business approval of the provisional local financial-write access policy remain mandatory. OPERATOR can retain financial-read access; no staff-account model was invented. Database-only backup does not include separately stored photos/attachments. Secret provisioning and legacy plaintext/TLS review require operator rollout steps. These prevent READY.

Commands and exact evidence paths: REGRESSION_REPORT.md. Test matrix: TEST_CASES.md. Operations and risk handling: RELEASE_OPERATIONS.md. Previous inventories/reproduction logs are preserved for traceability.

## Original discovery inventory (historical baseline)

# QC test plan

## Scope and safety

GymFlow 1.0.4 desktop gym management, assessed 2026-11-23 on Ubuntu Linux, OpenJDK 21.0.11, Maven 3.9.9. Source inventory: 97 Java files / 54,833 lines at initial static scan, 23 FXML screens, CSS, bilingual bundles. Recursive file inventory includes backups, data, dist and generated files; legacy copies are not active build source. Existing database contents and credentials were not inspected or modified. Many changes were present before QC; only DatabaseManager.java, small additions to PaymentRepository.java and the Windows packaging module-read option are QC code/config changes. No deployment or production database tests.

## Architecture and dependency inventory

Maven Java 21 desktop monolith: FXML/CSS -> JavaFX controllers -> service/repository -> JDBC. Models carry domain data; reporting has dedicated DTOs and PDF/XLSX exporters. No HTTP server or web routes. SQLite is default; MySQL and SQL Server selectable. JDBC connections are per operation and generally use try-with-resources. Financial fields use both BigDecimal (payments/reports) and double (subscriptions), requiring further rounding boundary tests. Read-side table bootstrapping exists for installment schedules outside migration management.

Direct dependencies: JavaFX controls/FXML 21.0.6; SQLite JDBC 3.50.3.0; MySQL Connector/J 9.4.0; MSSQL JDBC 12.10.1.jre11; jBCrypt 0.4; Apache POI 5.4.1; OpenPDF 2.0.3; ZXing core/javase 3.5.3; webcam-capture 0.3.12. Transitive versions in evidence/dependencies.log. Native cameras, printers, desktop file chooser and native fonts are external integration surfaces. No external web API integration identified by the risk scan.

## Modules and implemented functions

| Module | Implementation | Functions / workflows |
|---|---|---|
| Startup/navigation | Lazar_APP, ApplicationBootstrap, DashboardController | Database validation/migrations; super-admin first-run setup; license gate; dashboard navigation; language switching |
| Dashboard | DashboardContentController, DashboardService/Repository, dashboard DTOs | KPIs, recent members/payments, monthly charts, expiry summary and attendance refresh |
| Members | Members/AddMember/MemberDetails controllers; MemberRepository | Create/edit/delete/detail/search; generated member code; phone/code duplicate checks; member image/camera; QR and printable membership card |
| Plans | MembershipPlans/MembershipPlanForm; MembershipPlanRepository | Bilingual plan names; code, duration, price, sessions, status; CRUD/search |
| Subscriptions | Memberships/AddMembership; MemberMembershipRepository/model | Assign plan; authoritative plan price/session snapshot; dates/overlap protection; discount/net price; renewal/history; cancellation/deletion; remaining sessions and expiry |
| Attendance | Checkin/AttendanceHistory; CheckInRepository | Member lookup; check-in/out; open attendance guard; previous-day auto checkout; date/status filters; distinct-calendar-day session consumption; paging/detail dialogs |
| Payments | Payments/AddPayment; PaymentService/Repository | CRUD; member/subscription selection; paid/pending totals; outstanding balance; installment schedule; search/filter/order/paging |
| Other revenue | Revenues/RevenueForm; RevenueRepository | CRUD, auto code, types, dates, amounts, attachments, search and totals |
| Expenses | Expenses/ExpenseForm; ExpenseService/Repository | CRUD, category, beneficiary, paid/pending/cancelled, references, dates and totals |
| Reports | ReportsController; ReportService/Repository | Date/type summaries; activity; monthly aggregates; revenue/expense/profit/margin; export PDF and Excel |
| Settings | SettingsController; AppSettingsManager | Business/display preferences, themes/language, SQLite backup and restore, configuration persistence |
| Database administration | DatabaseAuth/DatabaseSettings controllers; DatabaseConfigManager | Super-admin password gate, database selection/credentials/TLS setting, connection test and save |
| Security/licensing | SuperAdminAuthService; SuperAdminSetup; ProgramLicense* | BCrypt password verification; period/lifetime license; date/clock/tamper checks; license settings |
| Shared desktop UI | WindowManager, PageViewport, AppDialog, TablePager/PageResult | Responsive windows/dialogs, paging, modal behavior, RTL/LTR and styling |

Full public API inventory for every Java file: evidence/source-api-inventory.txt. All resource paths: evidence/file-inventory.txt.

## Database inventory

MigrationRunner registers 001-009: members; membership_plans; member_memberships; check_ins; payments; session column upgrade; expenses; revenue_types/revenues; revenue schema reconciliation. schema_migrations records applied versions. payment_installments is lazily created by PaymentRepository with no FK. An additional phone-uniqueness migration has duplicate version 008 and is unregistered. Members/code, plans/code, revenues/code and expense/code have unique constraints. Subscriptions reference member (cascade) and plan; attendance references member (cascade) and subscription; payments restrict member deletion and set subscription reference null on deletion. SQLite schema and foreign keys are tested on synthetic data only. No migration down/rollback API identified; operational rollback must use verified backups. MySQL DDL transaction semantics and SQL Server upgrade behavior remain unverified.

## Authentication, configuration, error handling and deployment

This is a local desktop operator application. A BCrypt super-admin password protects database administration and license settings; ordinary business modules have no account/role authorization model. Startup checks configuration/license, not an operator login. Password hash/license use user.home/.lazar-gym; database and app settings use OS XDG/AppData paths. DB password is stored in plaintext properties; network encryption defaults false. No production credentials changed. Exception handling mixes dialogs, RuntimeException wrappers, System.err and printStackTrace; no centralized structured logger identified. GUI controller initialization often performs synchronous database operations, a responsiveness risk.

Maven compiler/JAR/dependency plugins build GymFlow.jar + libs; no existing test classes or test dependency detected. Surefire 2.17 ran with no original tests. Added dependency-free Java harness is invoked explicitly by tests/run.sh; it is not silently counted as Maven tests. Windows CI builds package, checks native JavaFX libraries and jpackages EXE; tag workflow publishes releases. Workflow was inspected, never triggered. Linux jpackage assessment evidence is separate. No production deployment performed.

## Risk-based execution sequence

1. Snapshot working-tree state and discover architecture/configuration without reading user databases.
2. Clean offline build; compile all Java with -Xlint:all; scan all FXML/translation keys and source APIs.
3. Isolated schema/constraint/rollback tests; payments and report arithmetic; attendance and sessions.
4. Reproduce defects; apply small connection/ownership/null-input fixes; run full fresh harness.
5. Exercise subscriptions, expenses, revenues, installments, bilingual exports, synthetic load and parallel writers.
6. Load all FXML in ar/en with actual JavaFX toolkit. This does not verify appearance or button outcomes.
7. Reproduce copy-based backup/restore risks using disposable SQLite fixtures; inspect packaging; evaluate release criteria.
8. Explicitly retain unexecuted scenarios in TEST_CASES.md. Acceptance requires no unresolved high release blockers and all mandatory evidence.

## Commands

```bash
mvn -o clean package
mvn -o dependency:tree
javac -Xlint:all -cp 'target/libs/*' -d /tmp/gymflow-qc-static-classes $(rg --files src/main/java -g '*.java')
bash qa-reports/tests/run.sh
python3 qa-reports/tests/static_checks.py
python3 qa-reports/tests/backup_reproduction.py
```

FXML probe source is tests/FxmlProbe.java; invocation and artifact checks are recorded in REGRESSION_REPORT.md. No coverage agent, SAST engine or complete dependency vulnerability scanner was run. The static scan is limited analysis, not proof that all 54k lines are defect-free. GUI manual interaction, hardware, long-duration stress, recovery acceptance, all engine upgrades and a clean Windows install remain mandatory follow-up coverage.
