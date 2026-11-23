"""Generate the final matrix only from the current executed evidence, preserving pending work."""
from pathlib import Path
import re,json,xml.etree.ElementTree as ET,hashlib
r=Path('qa-reports');e=r/'evidence/final-qa'
rows=[]
def add(id,module,description,preconditions,steps,expected,actual,status,evidence):
 rows.append(dict(id=id,module=module,description=description,preconditions=preconditions,steps=steps,expected=expected,actual=actual,status=status,evidence=evidence))
old={}
for line in (e/'TEST_CASES-before.md').read_text().splitlines():
 if line.startswith('| '):
  parts=[v.strip() for v in line.split('|')[1:-1]]
  if len(parts)==8:old[parts[0]]=parts
for id,status,timing in re.findall(r'RESULT\|([^|]+)\|(PASS|FAIL)\|([^\n]+)',(e/'legacy.log').read_text()):
 source=old[id];description=source[2].replace('twelve registered migrations','fourteen registered migrations');expected=source[4].replace('twelve registered migrations','fourteen registered migrations')
 add(id,source[1],description,'Fresh isolated profile; synthetic legacy-harness fixture','python3 qa-reports/tests/run_legacy.py; execute named Java harness case',expected,timing,status,'evidence/final-qa/legacy.log')
junit_time=0;junit_total=0
for file in sorted(e.glob('TEST-*.xml')):
 suite=ET.parse(file).getroot();junit_time+=float(suite.get('time','0'))
 for case in suite.findall('testcase'):
  method=case.get('name');id=method.split('_')[0];text=re.sub(r'([a-z])([A-Z])',r'\1 \2',method.split('_',1)[1]);cls=case.get('classname').split('.')[-1]
  status='FAIL' if case.find('error') is not None or case.find('failure') is not None else 'NOT EXECUTED' if case.find('skipped') is not None else 'PASS'
  add(id,cls,text,'Isolated Surefire home/XDG/AppData; per-test temporary database; synthetic data',f'mvn -o -Dtest={cls}#{method} test; fixture and exact actions in src/test/java',text,f'Assertions satisfied; {case.get("time")} s' if status=='PASS' else status,status,'evidence/final-qa/'+file.name)
  junit_total+=1
for i,(name,lang,status) in enumerate(re.findall(r'FXML\|([^|]+)\|(en|ar)\|(PASS|FAIL)',(e/'fxml-runtime.log').read_text()),1):
 add(f'FXML-{i:03}',name,'Instantiate packaged FXML/controller in '+lang,'Fresh disposable profile and actual JavaFX display','Run run_gui_smoke.py; set locale and FXMLLoader.load from final JAR','Load without controller/resource exceptions','Loaded',status,'evidence/final-qa/fxml-runtime.log')
ui=re.findall(r'^CASE\|([^|]+)\|(PASS|FAIL)\|([^\n]+)',(e/'ui-execution.log').read_text(),re.M)
for id,status,actual in ui:
 if id.startswith('RENDER-'):description='Render '+id[len('RENDER-'):]+' at 1366, 1024 and 390px widths';expected='Scene loads, layouts/snapshots complete and correct LTR/RTL orientation'
 elif id.startswith('FORM-'):description='Click Save on empty '+id[len('FORM-'):];expected='Validation feedback; no member/payment/revenue/expense inserted'
 elif id.startswith('DETAILS-') and 'FAILURE' not in id:description='Actual check-in details in '+id.split('-')[-1];expected='Correct member/subscription/dates; amount 100 USD, paid 35, due 65, remaining visits 10'
 else:description=id.replace('-',' ').lower();expected='All explicit assertions in the named FinalUiHarness case satisfied'
 add('UIAUTO-'+id,'JavaFX interaction',description,'Fresh synthetic UI DB, real JavaFX thread/display and disposable admin password','python3 qa-reports/tests/run_final_ui.py; execute named case using controls/handlers/Robot',expected,actual,status,'evidence/final-qa/ui-execution.log; screenshots/ for rendered views')
perf=(e/'performance.log').read_text()
for i,(name,actual) in enumerate(re.findall(r'^PERF\|([^|]+)\|([^\n]+)',perf,re.M),1):
 add(f'LOAD-{i:03}','Performance',name,'10k members/subscriptions; 20k payments; 50k attendance rows in disposable DB','python3 qa-reports/tests/run_performance.py','Operations complete; fixture/count or financial assertions hold',actual,'PASS','evidence/final-qa/performance.log')
assert 'PERFORMANCE_SUMMARY|PASS' in perf
soak=re.search(r'^SOAK\|(.+)$',perf,re.M).group(1)
add('LOAD-SOAK','Stability','Bounded repeated JDBC read/open/close workload','Disposable large fixture','Run PerformanceHarness for 30 seconds','Every cycle returns 10k members without exception',soak,'PASS','evidence/final-qa/performance.log')
integrity=json.loads((e/'performance-integrity.json').read_text());assert integrity['integrity_check']=='ok' and integrity['foreign_key_violations']==0
add('LOAD-INTEGRITY','Database','Verify persisted large fixture independently','Completed load/soak fixture','Read-only Python SQLite counts, integrity_check and foreign_key_check','10000/10000/20000/50000 rows; ok; zero violations',json.dumps(integrity),'PASS','evidence/final-qa/performance-integrity.json')
for id,status,actual in re.findall(r'^CASE\|([^|]+)\|(PASS|FAIL)\|([^\n]+)',(e/'operational-faults.log').read_text(),re.M):
 add(id,'Failure recovery','Reject disk-full financial write and recover','Disposable SQLite; page-count limit, never fill host filesystem','Run run_operational_faults.py; cap pages; insert oversized notes inside transaction; retry normal payment','SQLITE_FULL, zero partial payments, next write succeeds',actual,status,'evidence/final-qa/operational-faults.log')
assert (e/'javac-lint.log').stat().st_size==0 and (e/'diff-check.log').stat().st_size==0
assert json.loads((e/'osv-summary.json').read_text())==[]
package=json.loads((e/'package-result.json').read_text());assert package['packaged_jar_matches'] and package['fourteen_migrations_completed']
deb=json.loads((e/'deb-result.json').read_text());assert deb['jar_hash_matches'] and deb['extracted_startup_verified'] and not deb['installed_on_host']
checks=[('BUILD-001','Build','mvn -o clean verify','Compilation, tests and packaging succeed','BUILD SUCCESS; '+str(junit_total)+' JUnit tests','final-verify.log'),('STATIC-001','Resources','Run static_checks.py on FXML and Java translation lookups','No missing controllers/handlers/keys or duplicate properties','All resource checks clean','static-checks.log'),('LINT-001','Java','Compile all main Java sources with -Xlint:all','Zero compiler errors/warnings','Empty compiler diagnostic file','javac-lint.log'),('DIFF-001','Patch','git diff --check','No whitespace errors','Clean','diff-check.log'),('PKG-001','Linux packaging','Build app image; compare JAR SHA-256','Packaged JAR matches final clean build','Verified','artifact-sha256.txt'),('PKG-002','Linux runtime','Launch isolated app image to first-run setup boundary','SQLite ready and 14 migrations complete; no startup exception','Verified; timed stop before account/license completion','artifact-launch.log'),('SCA-001','Security','Query OSV for resolved runtime graph','No affected versions returned','30 coordinates; zero matched versions','osv-results.json'),('DEB-001','Linux DEB','Generate DEB, inspect metadata, extract and hash JAR','Correct metadata and exact JAR; no host install','Verified','deb-result.json'),('DEB-002','Linux DEB runtime','Launch extracted native launcher with isolated home','SQLite ready and 14 migrations complete','Verified; first-run boundary only','deb-launch.log')]
for id,module,steps,expected,actual,file in checks:add(id,module,steps,'Final candidate; necessary local tools; disposable profiles',steps,expected,actual,'PASS','evidence/final-qa/'+file)
startup=json.loads((e/'startup-timing.json').read_text());assert startup['jar_sha256']==hashlib.sha256(Path('target/GymFlow.jar').read_bytes()).hexdigest()
add('STARTUP-TIMING','Linux startup','Measure package to its own mapped first-run window','Fresh profile; PID-verified X11 window','Run measure_startup.py; poll database milestone and matching process window','Both milestones observed within 15s; stop own process',f'DB {startup["database_ready_ms"]:.1f} ms; first-run window {startup["owned_first_run_window_mapped_ms"]:.1f} ms','PASS','evidence/final-qa/startup-timing.json')
# Preserve each original composite acceptance case; partial automation is never relabeled complete.
review=[]
for id,source in old.items():
 if not id.startswith(('UI-','MAN-','ENV-')):continue
 status='NOT EXECUTED';remaining='Complete all interaction/accessibility/contrast/long-input scenarios; independent manual review still required';done='Both-locale rendering at 3 widths; see matching RENDER cases. Relevant forms, navigation and selected keyboard/business actions also automated.'
 if id.startswith('ENV-'):
  status='BLOCKED';remaining='No isolated server binary/container/runtime or provisioned test instance; no production services probed';done='SQLite counterpart and static dialect inspection only'
 elif id=='MAN-001':
  status='BLOCKED';remaining='Physical camera, scanner and printer acceptance unavailable';done='Software QR encode/decode, missing/valid photo and card UI rendering executed'
 elif id=='MAN-002':
  remaining='Full photos/attachments/config recovery and genuine power-loss/Windows filesystem drill remain';done='WAL backup, corrupt-schema refusal, mid-restore rollback, external writer refusal and killed transaction recovery executed'
 elif id=='MAN-003':
  remaining='Overnight soak, unavailable server/network interruption and wider resource-failure acceptance remain';done='30s/57k+ JDBC cycles, 50 navigation actions, connection failure and simulated SQLITE_FULL executed'
 elif id=='MAN-004':
  status='BLOCKED';remaining='Representative historical customer fixtures and all server engines not supplied';done='Synthetic nine-version database upgrade and conflict refusal executed without deleting data'
 elif id=='MAN-005':
  status='BLOCKED';remaining='Linux host has no Windows VM/runtime; installer install/uninstall/upgrade unavailable';done='Windows script inspected; Linux app image and extracted DEB startup executed'
 elif id=='MAN-006':
  status='BLOCKED';remaining='Approved staff/permission acceptance matrix not supplied; local write-approval policy needs owner acceptance';done='Implemented OPERATOR/SUPER_ADMIN denials, login/wrong password/cancel/logout/expiry tests executed'
 add(id,source[1],source[2],'Disposable fixtures and required human/hardware/infra access',source[2],source[4],done+' Remaining: '+remaining,status,'OUTSTANDING_TESTS_REVIEW.md; evidence/final-qa/infrastructure.json and ui-execution.log')
 review.append((id,source[1],done,remaining,status))
counts={s:sum(x['status']==s for x in rows) for s in ['PASS','FAIL','BLOCKED','NOT EXECUTED']};assert counts['FAIL']==0
(e/'case-results.json').write_text(json.dumps(rows,ensure_ascii=False,indent=2));(e/'case-counts.json').write_text(json.dumps(counts,indent=2))
header='''# Final QA test cases

Current evidence: `qa-reports/evidence/final-qa`. Original 163-case snapshot: `TEST_CASES-before.md` in that directory. Every PASS below was executed in this assessment; old results are not reused as proof. Case counts include intentional overlapping checks across JUnit, legacy, standalone GUI and runtime suites; they are execution cases, not unique features or assertion counts. A rendered scene is not full visual/accessibility acceptance. Broad original manual composites remain pending even when individual subcases pass.

All database tests use temporary synthetic records and isolated home/XDG/AppData paths. RI fixtures use two members, plan 1 (100 units / 12 sessions), subscription 1 owned by member 1. GUI fixtures use active, expired, missing and exhausted memberships plus partial payments. No customer records or real secrets are used.

| Test case ID | Module | Description | Preconditions | Test steps | Expected result | Actual result | Status | Execution evidence |
|---|---|---|---|---|---|---|---|---|
'''
def clean(v):return str(v).replace('|','/').replace('\n',' ')
(r/'TEST_CASES.md').write_text(header+'\n'.join('| '+' | '.join(clean(x[key]) for key in ['id','module','description','preconditions','steps','expected','actual','status','evidence'])+' |' for x in rows)+'\n')
(r/'OUTSTANDING_TESTS_REVIEW.md').write_text('''# Disposition of all 31 previously outstanding cases

The original two blocked and 29 unexecuted entries were reviewed individually. Their broad acceptance steps cannot be completed merely by running newly added narrower subcases. BLOCKED means required external prerequisites are missing; NOT EXECUTED identifies remaining manual/extended acceptance. No original composite is marked PASS from a partial result.

| Original ID | Purpose/module | Executed parts and evidence | Remaining requirements / reason | Current status |
|---|---|---|---|---|
'''+ '\n'.join('| '+' | '.join(clean(v) for v in row)+' |' for row in review)+'\n')
sha=hashlib.sha256(Path('target/GymFlow.jar').read_bytes()).hexdigest()
report=f'''# GymFlow 1.0.4 final release readiness

**Release decision: NOT READY**

Assessed 2026-11-23 on Ubuntu, Java 21 / JavaFX 21.0.6. Prior evidence was independently rerun and expanded. Confirmed application defects were corrected and retested. Mandatory external-platform and full manual/operational acceptance remain unverified; successful automation is not treated as production approval.

| Metric | Current result |
|---|---|
| Total documented cases | {len(rows)} |
| PASS | {counts['PASS']} |
| FAIL | {counts['FAIL']} |
| BLOCKED | {counts['BLOCKED']} |
| NOT EXECUTED | {counts['NOT EXECUTED']} |
| JUnit unit/integration/security/recovery tests | {junit_total} PASS, 0 failed/errors/skipped; {junit_time:.3f}s summed suite time |
| Existing Java workflow cases | 35 PASS |
| Actual JavaFX interaction/rendering cases | {len(ui)} PASS |
| Independent packaged FXML loads | 46 PASS |
| Compiler lint | 0 main-source errors/warnings |
| Runtime advisory query | 30 Maven package versions, zero OSV matches |
| Code coverage | Not measured |

Counts are generated from case-results.json and TEST_CASES.md, not inferred from compilation. This replaces the prior 163 / 132 / 0 / 2 / 29 assessment. All 31 original pending entries are mapped in OUTSTANDING_TESTS_REVIEW.md; some have been reclassified BLOCKED because prerequisites are absent. Automated subcases do not erase unexecuted portions of the old composites.

## Executed business and UI evidence

Actual Save buttons and real parent-window workflows persist a member, subscription, partial payment, expense and revenue in a disposable database. Both languages have empty-form validation, all 23 screen renders at 1366/1024/390px, approval cancellation/wrong credentials/success, Tab traversal, 50 navigation actions and software QR round-trip checks. JavaFX checks also cover active/expired/missing/exhausted subscriptions, duplicate check-in, same-day re-entry, missing/valid photo, database failure and unavailable financial data. Check-in details assert identity, exact subscription/dates, 100 USD total, 35 paid, 65 due and 10 remaining visits. Unavailable finance displays a dash rather than an invented zero.

Headless tests additionally cover CRUD, renewals/history, date/session boundaries, reports, negative/sub-cent/non-finite values, duplicate submission guards, role denials, session expiry/logout, password security, environment secrets, referential integrity, rollback, migration conflicts and synthetic legacy upgrades. A separate process holds a real SQLite write lock during recovery, and a killed uncommitted transaction recovers committed payments. A page-limit SQLITE_FULL simulation rejects a financial operation without filling the host filesystem or leaving partial payments.

## Corrections in this assessment

- Migration 013 enforces a single open attendance record and a unique client submission token for payments; repeated submission no longer duplicates money. Separate intentional payments remain allowed.
- Sub-cent payments are rejected; check-in details now use the configured currency and consumed-day-based remaining visits. The last allowed visit permits same-day re-entry consistently with the one-session-per-day rule.
- Missing member-validation translations, Settings locale reset, first-run setup localization, unreadable database captions, currency glyphs/Close caption and hardcoded EGP displays were corrected. Existing cached currency helpers are reused; no stored monetary values or exchange conversions were changed.
- Recovery now stages/upgrades/validates input and copies rows under an exclusive SQLite transaction with constraints enabled. It refuses an external writer, rolls back mid-copy errors, preserves the verified safety snapshot and never overwrites live database files blindly.
- Migration 014 adds membership/visit lookup indexes. One Arabic attendance-search sample improved from 2391.966ms to 244.977ms on the same synthetic workload; this is a single-run observation, not a capacity guarantee.
- Duplicate-member exception messages no longer embed personal values. Static resource checks now include Java validation keys and fail on errors. Tests and test runners were strengthened; no unstable TestFX dependency was added.

Details, exact locations and regression references: BUG_REPORT.md and CHANGELOG.md. Earlier fixes remain under regression. Existing unrelated working-tree edits were preserved.

## Build and Linux artifacts

`mvn -o clean verify` succeeds with all {junit_total} JUnit cases. Integration tests execute real SQLite through JUnit/Surefire; no separate Failsafe suite is claimed. The final app image and an additionally generated DEB contain the exact clean-build JAR. The DEB was inspected/extracted and its native launcher started in isolation; it was never installed into the host OS. Both runtimes initialize SQLite and complete all 14 migrations. Timed first-run smoke does not certify completed account/license flow, graceful user shutdown, or system installation/uninstallation.

JAR SHA-256: `{sha}`.

Measured startup: database readiness {startup['database_ready_ms']:.1f}ms, PID-verified mapped first-run setup window {startup['owned_first_run_window_mapped_ms']:.1f}ms (25ms polling). This measures first-run readiness, not completed login. Runtime paths and package/extraction metadata are preserved in evidence JSON. No production deployment, remote workflow, commit, push or release publication occurred.

## Remaining mandatory work

1. MySQL and SQL Server engine suites: no isolated server binaries, containers or test instances available. No production endpoints or credentials were guessed/probed.
2. Windows installer clean install/uninstall/upgrade and native-device acceptance: no Windows environment available. Linux DEB extraction is not a substitute.
3. Complete human UI/action/accessibility/contrast review beyond the automated subcases. 390px stress views use scrolling/truncation; no mobile-layout guarantee is made. Rapid fixture resizes emitted GTK geometry warnings, while JavaFX unhandled-exception assertions passed; platform/window-manager review remains.
4. Physical camera/scanner/printer and full asset recovery. DB backups do not include separately stored photographs/attachments/configuration. Synthetic upgrades and transaction-kill tests are not customer-data or power-loss certification.
5. Obtain the required staff/permission acceptance matrix. The existing local OPERATOR/SUPER_ADMIN write-approval policy is tested; individual staff accounts/audit trails and broader financial-read restrictions are not invented or certified.
6. Complete overnight soak, real server/network outages, deployment ACL/TLS/legacy-secret review and full operational recovery. OSV is an advisory match query, not a full penetration test or bundled OS/JDK audit.

Release remains NOT READY because these mandatory acceptance conditions have no executed evidence. No production secrets, credentials or financial records were modified.
'''
for file in [r/'RELEASE_READINESS_REPORT.md',r/'RELEASE_REPORT.md',Path('RELEASE_REPORT.md'),Path('RELEASE_READINESS_REPORT.md')]:file.write_text(report)
print('Final statistics:',counts,'total',len(rows),'JUnit',junit_total,'UI',len(ui))
