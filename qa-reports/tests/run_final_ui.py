from pathlib import Path
import tempfile,os,subprocess,json
root=Path(__file__).resolve().parents[2]
evidence=root/'qa-reports/evidence/final-qa';fixture=Path(tempfile.mkdtemp(prefix='gymflow-qc-final-ui-'));classes=fixture/'classes';classes.mkdir()
env=os.environ.copy();env.pop('GYMFLOW_DB_PASSWORD',None)
for option in ['JAVA_TOOL_OPTIONS','JDK_JAVA_OPTIONS','_JAVA_OPTIONS']:env.pop(option,None)
for key,folder in [('XDG_DATA_HOME','data'),('LOCALAPPDATA','data'),('XDG_CONFIG_HOME','config'),('APPDATA','config')]:env[key]=str(fixture/folder)
cp=os.pathsep.join([str(root/'target/classes'),str(root/'target/libs/*')])
subprocess.run(['javac','-cp',cp,'-d',str(classes),str(root/'qa-reports/tests/FinalUiHarness.java')],check=True,env=env)
(evidence/'ui-fixture.json').write_text(json.dumps({'fixture':str(fixture)},indent=2))
with (evidence/'ui-execution.log').open('w') as log:
 try:result=subprocess.run(['java',f'-Duser.home={fixture}','-Dprism.order=sw','-cp',str(classes)+os.pathsep+cp,'FinalUiHarness',str(evidence/'screenshots')],cwd=root,env=env,stdout=log,stderr=subprocess.STDOUT,timeout=240)
 except subprocess.TimeoutExpired:print('UI test process timed out; inspect ui-execution.log');raise SystemExit(2)
raise SystemExit(result.returncode)
