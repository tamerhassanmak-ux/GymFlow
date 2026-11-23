# GymFlow release operations and remaining acceptance

## Credentials and access

- Existing Super Admin passwords are preserved. First-run setup creates a BCrypt cost-12 hash; later password changes require the current password, use an atomic private-file write and revoke approval. The 72-byte UTF-8 limit avoids BCrypt truncation. No real password was read, changed or reset during QA.
- Operators can register members and check them in/out. Financial writes, subscription/plan changes, destructive member deletion, configuration changes, license activation and backup/restore require recent Super Admin approval. UI prompts are bilingual; successful approval lasts 15 minutes in this desktop process. Closing the process or changing the admin password clears approval. Financial read visibility is unchanged. There is no multi-user staff-account directory or per-person audit trail; obtain policy approval before release.
- Database password persistence intentionally changed: new saves omit the password. Set `GYMFLOW_DB_PASSWORD` in the protected launcher/service environment for remote database access after restart, or enter it for the current session. Do not put secrets in command-line arguments, source control, QA logs or shared scripts. Provision the environment separately from source deployment. Existing legacy properties passwords are read for compatibility and removed only on an explicitly authorized configuration save. Production files were not edited in QA.
- New network configurations enable TLS; MySQL uses VERIFY_IDENTITY and SQL Server certificate validation. Existing explicitly disabled TLS choices remain readable. Provision trusted server certificates and verify remote connections on each supported server before release. Restrict local database/config access using the OS account/ACL boundary. Application roles do not protect against arbitrary code running as the database-owning OS user.

## Upgrade and data review

1. Stop other GymFlow instances and take a verified offline/full-system recovery copy before deploying an upgrade. Include attachments/member photos and configuration, not just the database.
2. Test upgrade against an isolated copy first. Migrations 010-012 do not repurpose 008: 010 enforces unique nonblank trimmed phones; 011 enforces payment/attendance ownership; 012 moves installments into schema management. Existing duplicate phones, ownership conflicts or invalid installments stop migration for administrator review. No customer rows are deleted or silently consolidated. Applied earlier migrations can remain committed; this is a forward migration system, not a universal down-migration framework.
3. Resolve historical conflicts through approved business review, not a destructive QA script. Real historical datasets were not provided or inspected in this assessment.
4. SQLite migrations and synthetic legacy conflict handling are tested. MySQL/SQL Server schema changes still need isolated engine-specific execution, especially computed indexes, foreign keys, DDL retries and financial workflows.

## SQLite backup and recovery

- In-app backup uses SQLite's online backup API, includes committed WAL transactions, validates the snapshot and atomically publishes the destination. It cannot overwrite the active database. Failed staging leaves the previous destination unchanged.
- Restore first snapshots the chosen file through a read-only SQLite connection; validates integrity, required schema, migration history, financial/foreign-key relationships and recognized trigger definitions; refuses unsupported versions/missing guards. It blocks while managed connections are active and aborts after 5 seconds rather than overwriting a database in use.
- Restore creates a validated `pre-restore-<UUID>.db` recovery snapshot alongside the live DB. SQLite's restore API performs transactional replacement under SQLite locking, rather than overwriting a live file and its WAL externally. Retain this recovery snapshot until business verification completes.
- After a successful restore the desktop exits; reopen it so migrations, settings and dashboard data are reloaded. Stop other application instances before recovery. QA verified active managed-connection refusal and a round trip recovering the pre-restore payment data. Cross-process recovery with external writers, power-loss/filesystem failure and Windows-specific locks still require acceptance tests.
- `.db` backups contain the database only. Photos and revenue attachments must be included in an independent full application-data backup. New attachments are kept under per-user AppPaths storage with unique names; legacy paths remain readable. A DB restore alone cannot recover a separately deleted attachment. Do not advertise DB-only backup as full asset recovery.

## Build and automated tests

```bash
mvn clean package
python3 qa-reports/tests/run_legacy.py
python3 qa-reports/tests/static_checks.py
```

Maven runs JUnit 5 through Surefire with isolated user.home/XDG/AppData paths and excludes `GYMFLOW_DB_PASSWORD` from forked tests. `clean` removes generated target fixtures, not customer data. The portable legacy runner creates a fresh temporary home and strips remote DB credentials. CI runs Maven and the existing workflow harness before building the installer. No workflow was triggered or release published during QA.

## Manual and external acceptance still required

Clean Windows EXE install/uninstall/upgrade and native library loading; Arabic RTL/English LTR screenshots at supported resolutions; keyboard/contrast/validation interactions; admin prompt cancellation and expired approval; camera/QR/printer and attachment-viewer behavior; actual MySQL/SQL Server connections with certificates; representative historical upgrades; external-writer and interrupted recovery; sustained performance; deployment-specific dependency vulnerability review; full asset-backup drill; and acceptance of the local operator/admin policy. Record actual results before changing NOT READY to READY.
