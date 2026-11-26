# GymFlow 1.0.8

- A clean first installation starts with SQLite and `gymflow.db`; the default database name is `gymflow`. Existing database selections remain unchanged on upgrade.
- Only Super Admin can activate a different database. The selected database must pass connection checks, supported migrations and schema verification before replacing the active connection settings. A failed candidate preserves the previous selection.
- The settings editor preserves separate drafts and custom ports for SQLite, MySQL, MariaDB and SQL Server while the window stays open.
- Switching databases refreshes the main application window. Each selected database retains its own records; changing connections does not transfer records across engines.
- Local MySQL/MariaDB automatic upgrades now take a native SQL backup, restore it into a temporary database, compare row fingerprints and rehearse migrations before upgrading the original schema. A named lock serializes upgrades by cooperating application instances.
- Data conflicts receive specific recovery messages without automatically deleting records.

## Packages and upgrade

Linux: DEB installer. Windows: EXE and MSI installers. All packages include Java 21 and JDBC drivers. Customer databases, passwords and local configuration are excluded. Platform-specific SHA-256 checksum files accompany the installers.

Back up existing data and close all running GymFlow instances before installing. Retain the existing application data directory. SQLite first-run operation does not require a database server or native MySQL tools.

## Automatic server upgrade requirements

Automatic native upgrade currently supports local MySQL/MariaDB profiles with explicitly disabled TLS and no custom views, triggers, routines or events. Native `mysqldump`/`mariadb-dump` and `mysql`/`mariadb` clients must be available on PATH; Linux XAMPP `/opt/lampp/bin` is also detected. These external native clients are not bundled. The database account needs backup/read-lock, temporary CREATE/DROP DATABASE and migration privileges. Verified backups remain in GymFlow's backups directory.

Stop older application versions and external writers during server upgrades. MySQL DDL can commit independently; a successful rehearsal does not guarantee rollback after a later hardware or permission failure. The application never automatically restores over live data. Remote/TLS profiles, custom server objects, SQL Server schema upgrades and conflicting records require administrator intervention. Already prepared compatible databases remain selectable from settings.

## Validation

Local Linux validation covered the default fresh-install SQLite startup, authenticated database activation, failed-candidate preservation, return to the original database, the four-engine settings editor, and native MySQL/MariaDB backup/restore and migration rehearsal using disposable schemas. The release workflow runs the Maven suite and verifies bundled-runtime smoke tests on Linux and Windows before publication. Native Windows installer acceptance and remote/TLS server upgrade workflows are not claimed by these checks.
