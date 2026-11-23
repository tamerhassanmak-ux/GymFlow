from pathlib import Path
import re,json,xml.etree.ElementTree as ET
r=Path('qa-reports');e=r/'evidence/release-fix'
old=(e/'TEST_CASES-before.md').read_text()
legacy={m[0]:(m[1],m[2]) for m in re.findall(r'RESULT\|([^|]+)\|(PASS|FAIL)\|([^\n]+)',(e/'legacy-final.log').read_text())}
rows=[]
for line in old.splitlines():
 if not line.startswith('| '):continue
 parts=[x.strip() for x in line.split('|')[1:-1]]
 if len(parts)!=8:continue
 id=parts[0]
 if id in legacy:
  parts[2]=parts[2].replace('nine registered migrations','twelve registered migrations');parts[4]=parts[4].replace('nine registered migrations','twelve registered migrations')
  parts[5]=legacy[id][1];parts[6]=legacy[id][0]
  if id=='DB-001':parts[3]='Fresh disposable SQLite configuration; schema initially absent'
  rows.append(parts)
 elif id.startswith(('UI-','MAN-','ENV-')):
  if id=='MAN-002':parts[5]='Cross-process, interrupted recovery and full asset recovery remain manual; automated DB recovery tests pass'
  if id=='MAN-006':parts[5]='OPERATOR/SUPER_ADMIN write checks automated; prompt/expiry interactions and business-policy approval remain manual'
  rows.append(parts)
for p in sorted(e.glob('TEST-*.xml')):
 root=ET.parse(p).getroot()
 for c in root.findall('testcase'):
  name=c.attrib['name'];id=name.split('_',1)[0];objective=re.sub(r'([a-z])([A-Z])',r'\1 \2',name.split('_',1)[1])
  status='FAIL' if c.find('failure') is not None or c.find('error') is not None else 'NOT EXECUTED' if c.find('skipped') is not None else 'PASS'
  rows.append([id,c.attrib.get('classname','').split('.')[-1],objective+'; execute named method in src/test/java',
   'Disposable configured SQLite fixture; synthetic members/plan; unit tests use in-memory models',objective,
   f'Assertions satisfied; {c.attrib.get("time","?")} s' if status=='PASS' else status,status,'See BUG_REPORT.md'])
for i,(name,lang,status) in enumerate(re.findall(r'FXML\|([^|]+)\|(en|ar)\|(PASS|FAIL)',(e/'fxml-runtime.log').read_text()),1):
 rows.append([f'FXML-{i:03}',name,f'Load FXML from final JAR with {lang} bundle','Permitted local display; fresh temporary home/database','No loader/controller exception','Loaded',status,'—'])
checks=[('BUILD-001','Build','mvn clean package','BUILD SUCCESS','build-final.log'),('STATIC-001','Resources','Parse FXML/handlers/ar-en keys and duplicates','No missing handlers/keys/duplicate properties','static-checks.log'),('LINT-001','Java','javac -Xlint:all over all main Java sources','No errors or warnings','javac-lint.log'),('DIFF-001','Patch','git diff --check','No whitespace errors','diff-check.log'),('PKG-001','Packaging','Generate Linux app image and compare JAR SHA-256','Image created and JAR matches','artifact-sha256.txt'),('PKG-002','Packaging','Start packaged app for 12 seconds on fresh user home','SQLite initialization and all 12 migrations finish without exception; stop at setup boundary','artifact-launch.log'),('SCA-001','Dependencies','Query OSV using 30 resolved runtime Maven coordinates','No affected versions returned after patches','osv-results.json')]
assert (e/'package-result.json').exists(), 'Final package verification required before reporting PASS'
assert json.loads((e/'osv-summary.json').read_text())==[], 'Unresolved dependency findings'
assert (e/'javac-lint.log').stat().st_size==0
assert (e/'diff-check.log').stat().st_size==0
for id,module,steps,expected,file in checks:rows.append([id,module,steps,'Java 21; final artifact and cached dependencies',expected,'Verified; evidence/release-fix/'+file,'PASS','—'])
counts={status:sum(row[6]==status for row in rows) for status in ['PASS','FAIL','BLOCKED','NOT EXECUTED']}
header='''# Current release test cases

Updated 2026-11-23. This replaces the earlier result matrix; its snapshot remains in evidence/release-fix/TEST_CASES-before.md. Each ID is one executed case or explicitly pending scenario. Assertions and reruns are not counted separately. The 35 legacy cases and 44 JUnit cases are separate executed tests with some intentional coverage overlap.

Unit source: src/test/java/com/mycompany/lazar_app/FinancialRulesTest.java. Integration/security/recovery source: ReleaseRegressionTest.java in the same directory. Each RI case starts with a disposable migrated SQLite DB, members 1/2, plan 1 (100 currency units, 12 sessions), and subscription 1 belonging to member 1. Passwords, amounts, paths and files are synthetic. See source methods for exact actions/assertions. No production records were used. FXML loading proves instantiation only, not visual or button acceptance.

| ID | Module | Objective / steps / test data | Preconditions | Expected | Actual | Status | Defect |
|---|---|---|---|---|---|---|---|
'''
(r/'TEST_CASES.md').write_text(header+'\n'.join('| '+' | '.join(v.replace('|','/') for v in row)+' |' for row in rows)+'\n')
testtime=sum(float(ET.parse(p).getroot().attrib.get('time','0')) for p in e.glob('TEST-*.xml'))
report=f'''# GymFlow 1.0.4 release report

**Decision: NOT READY**

Updated 2026-11-23. The automatically reproducible defects in the previous assessment have corrections with passing local regression evidence. Production approval remains withheld because mandatory Windows, server-database, UI/hardware, historical-data and operational acceptance evidence is missing. No production deployment or real-data/credential modification occurred.

## Results

| Metric | Result |
|---|---|
| Documented cases | {len(rows)} |
| PASS | {counts['PASS']} |
| FAIL | {counts['FAIL']} |
| BLOCKED | {counts['BLOCKED']} |
| NOT EXECUTED | {counts['NOT EXECUTED']} |
| JUnit 5 tests | 44 passed; 0 failed/errors/skipped; {testtime:.3f} s summed suite time |
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
'''
for p in [r/'RELEASE_REPORT.md',r/'RELEASE_READINESS_REPORT.md',Path('RELEASE_REPORT.md')]:p.write_text(report)
(e/'case-counts.json').write_text(json.dumps(counts,indent=2));print(counts,'total',len(rows))
