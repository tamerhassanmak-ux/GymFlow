from pathlib import Path
import tempfile,os,subprocess,json
root=Path(__file__).resolve().parents[2];fixture=Path(tempfile.mkdtemp(prefix='gymflow-qc-final-perf-'));classes=fixture/'classes';classes.mkdir();evidence=root/'qa-reports/evidence/final-qa'
env=os.environ.copy();env.pop('GYMFLOW_DB_PASSWORD',None)
for option in ['JAVA_TOOL_OPTIONS','JDK_JAVA_OPTIONS','_JAVA_OPTIONS']:env.pop(option,None)
for key,folder in [('XDG_DATA_HOME','data'),('LOCALAPPDATA','data'),('XDG_CONFIG_HOME','config'),('APPDATA','config')]:env[key]=str(fixture/folder)
cp=os.pathsep.join([str(root/'target/classes'),str(root/'target/libs/*')]);subprocess.run(['javac','-cp',cp,'-d',str(classes),str(root/'qa-reports/tests/PerformanceHarness.java')],check=True,env=env)
args=['java',f'-Duser.home={fixture}','-cp',str(classes)+os.pathsep+cp,'PerformanceHarness']
if Path('/usr/bin/time').exists():args=['/usr/bin/time','-v']+args
(evidence/'performance-fixture.json').write_text(json.dumps({'fixture':str(fixture)},indent=2))
with (evidence/'performance.log').open('w') as log:result=subprocess.run(args,cwd=root,env=env,stdout=log,stderr=subprocess.STDOUT,timeout=180)
raise SystemExit(result.returncode)
