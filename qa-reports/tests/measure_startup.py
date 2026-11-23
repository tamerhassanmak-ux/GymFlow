"""Measure a disposable package to its own mapped first-run window, without touching other windows."""
from pathlib import Path
import os,json,tempfile,subprocess,time,re,hashlib
root=Path(__file__).resolve().parents[2];e=root/'qa-reports/evidence/final-qa';paths=json.loads((e/'package-paths.json').read_text());image=Path(paths['output'])/'GymFlowQC';fixture=Path(tempfile.mkdtemp(prefix='gymflow-qc-startup-timing-'));env=os.environ.copy()
for key in ['GYMFLOW_DB_PASSWORD','JAVA_TOOL_OPTIONS','JDK_JAVA_OPTIONS','_JAVA_OPTIONS']:env.pop(key,None)
for key,folder in [('XDG_DATA_HOME','data'),('LOCALAPPDATA','data'),('XDG_CONFIG_HOME','config'),('APPDATA','config')]:env[key]=str(fixture/folder)
env['JAVA_TOOL_OPTIONS']=f'-Duser.home={fixture}'
log=e/'startup-timing.log';started=time.perf_counter();db_ms=None;window_ms=None
with log.open('w') as output:
 process=subprocess.Popen([str(image/'bin/GymFlowQC')],env=env,stdout=output,stderr=subprocess.STDOUT)
 try:
  while time.perf_counter()-started<15 and process.poll() is None:
   content=log.read_text()
   if db_ms is None and 'Database migrations completed successfully.' in content:db_ms=(time.perf_counter()-started)*1000
   tree=subprocess.run(['xwininfo','-root','-tree'],env=env,capture_output=True,text=True,timeout=2).stdout
   for line in tree.splitlines():
    if 'Super Admin Setup' not in line:continue
    found=re.search(r'0x[0-9a-fA-F]+',line)
    if not found:continue
    owner=subprocess.run(['xprop','-id',found.group(),'_NET_WM_PID'],env=env,capture_output=True,text=True,timeout=2).stdout
    if re.search(r'=\s*'+str(process.pid)+r'\s*$',owner):
     window_ms=(time.perf_counter()-started)*1000;break
   if window_ms is not None:break
   time.sleep(.025)
 finally:
  process.terminate()
  try:process.wait(timeout=5)
  except subprocess.TimeoutExpired:process.kill();process.wait(timeout=5)
assert db_ms is not None and window_ms is not None,'Owned first-run window/migration timing not observed'
(e/'startup-timing.json').write_text(json.dumps({'database_ready_ms':db_ms,'owned_first_run_window_mapped_ms':window_ms,'poll_interval_ms':25,'completed_login_or_license_flow':False,'jar_sha256':hashlib.sha256((root/'target/GymFlow.jar').read_bytes()).hexdigest()},indent=2))
print(f'Startup timing: DB ready {db_ms:.1f} ms; owned first-run window mapped {window_ms:.1f} ms')
