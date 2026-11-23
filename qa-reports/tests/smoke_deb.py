from pathlib import Path
import tempfile,os,subprocess,json
root=Path(__file__).resolve().parents[2];e=root/'qa-reports/evidence/final-qa';fixture=Path(tempfile.mkdtemp(prefix='gymflow-qc-deb-smoke-'));result=json.loads((e/'deb-result.json').read_text());env=os.environ.copy()
for key in ['GYMFLOW_DB_PASSWORD','JAVA_TOOL_OPTIONS','JDK_JAVA_OPTIONS','_JAVA_OPTIONS']:env.pop(key,None)
for key,folder in [('XDG_DATA_HOME','data'),('LOCALAPPDATA','data'),('XDG_CONFIG_HOME','config'),('APPDATA','config')]:env[key]=str(fixture/folder)
env['JAVA_TOOL_OPTIONS']=f'-Duser.home={fixture}'
with (e/'deb-launch.log').open('w') as log:
 try:r=subprocess.run([result['extracted_launcher']],env=env,stdout=log,stderr=subprocess.STDOUT,timeout=12);status=r.returncode
 except subprocess.TimeoutExpired:status='bounded first-run smoke stopped at 12 seconds'
text=(e/'deb-launch.log').read_text();assert 'Database connection initialized successfully using: SQLite' in text;assert '[ OK ] 014' in text;assert 'Exception' not in text
result['extracted_startup_verified']=True;result['startup_exit']=status;(e/'deb-result.json').write_text(json.dumps(result,indent=2));print('Extracted DEB startup/migrations verified without installing into the host.')
