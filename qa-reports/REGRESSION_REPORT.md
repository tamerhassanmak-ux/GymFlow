# Current regression report

Updated 2026-11-23. Current evidence directory: `qa-reports/evidence/release-fix`. Previous logs remain as historical reproduction evidence.

- Final clean Maven build executes **44 JUnit 5 tests: 0 failures, 0 errors, 0 skipped**, then packages the release JAR/runtime libraries. Surefire XML is retained outside target so subsequent clean builds cannot erase the report evidence.
- Existing portable Java harness executes **35 workflow cases: 35 PASS**. Includes payment CRUD/totals/status filtering, expenses, revenues, subscription overlap/cancellation, attendance/session counting, report arithmetic, month-end installments/rollback, authentication/licensing and structural bilingual exports.
- Full Java compiler lint: **zero warnings/errors**. Static resource checks: all FXMLs parse, handlers/referenced keys present, 422 matching locale keys each, zero duplicate declarations. `git diff --check` clean.
- Current FXML probe loads **23 screens × English/Arabic = 46 PASS** from the packaged JAR on a permitted local display with a fresh disposable user home. It returns nonzero on loading errors. This is not visual or action acceptance.
- Linux app-image JAR SHA-256 equals the clean-build JAR; isolated launch initializes SQLite and completes all **12 migrations**. Timed stop at first-run setup does not count as completed account/license/startup/shutdown acceptance or a Windows install test.
- OSV resolved-runtime check initially matched 3 package versions; targeted patches applied; final query of 30 package versions returns **zero affected matches**. All automated suites and packaged smoke were repeated against the patched graph.

Reproduce local non-GUI tests:

```bash
mvn clean package
python3 qa-reports/tests/run_legacy.py
python3 qa-reports/tests/static_checks.py
javac -Xlint:all -cp 'target/libs/*' -d /tmp/gymflow-qc-release-lint $(rg --files src/main/java -g '*.java')
```

Optional Linux display/package probes:

```bash
python3 qa-reports/tests/prepare_package.py
python3 qa-reports/tests/run_gui_smoke.py
```

Dependency audit:

```bash
python3 qa-reports/tests/dependency_audit.py prepare
curl --fail --silent --show-error --max-time 30 -H 'Content-Type: application/json' --data-binary @qa-reports/evidence/release-fix/osv-query.json https://api.osv.dev/v1/querybatch --output qa-reports/evidence/release-fix/osv-results.json
python3 qa-reports/tests/dependency_audit.py
```

The query sends only public package names/versions. Sandbox cache/DNS/display restrictions required authorized tool escalation; all application data remained in isolated locations. Maven tests exclude the remote DB-password environment variable and override home/XDG/AppData paths. No production database was opened by tests. No installer was installed, release workflow invoked or production deployment performed.

Failure-injection evidence covers corrupt/wrong/altered backups, active managed connection refusal, legacy duplicate migration refusal, nested rollback-only financial failures, direct database ownership violations, negative/NaN financial inputs and unauthorized mutations. It does not certify external writer/power-loss/disk-full recovery, Windows, real historical datasets, server engines, UI interaction or sustained stress. Those remain blocked/not executed in TEST_CASES.md and prevent READY.
