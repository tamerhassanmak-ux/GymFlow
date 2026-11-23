"""Build and extract a disposable DEB; never install it into the host OS."""
from pathlib import Path
import json,subprocess,hashlib
root=Path(__file__).resolve().parents[2];e=root/'qa-reports/evidence/final-qa';paths=json.loads((e/'package-paths.json').read_text());image=Path(paths['output'])/'GymFlowQC';dest=Path(paths['root'])/'deb';dest.mkdir()
args=['jpackage','--type','deb','--app-image',str(image),'--name','GymFlowQC','--app-version','1.0.4','--linux-package-name','gymflow-qc','--dest',str(dest)]
(e/'deb-command.json').write_text(json.dumps(args,indent=2))
with (e/'deb-build.log').open('w') as log:subprocess.run(args,check=True,stdout=log,stderr=subprocess.STDOUT,timeout=120)
packages=list(dest.glob('*.deb'));assert len(packages)==1
package=packages[0];metadata=subprocess.check_output(['dpkg-deb','--info',str(package)],text=True);(e/'deb-metadata.txt').write_text(metadata)
extract=Path(paths['root'])/'deb-extracted';subprocess.run(['dpkg-deb','--extract',str(package),str(extract)],check=True)
jars=list(extract.rglob('GymFlow.jar'));assert len(jars)==1
assert hashlib.sha256(jars[0].read_bytes()).digest()==hashlib.sha256((root/'target/GymFlow.jar').read_bytes()).digest()
launchers=list(extract.glob('opt/*/bin/GymFlowQC'));assert len(launchers)==1
(e/'deb-result.json').write_text(json.dumps({'package':str(package),'extracted_launcher':str(launchers[0]),'metadata_verified':True,'jar_hash_matches':True,'installed_on_host':False},indent=2))
print('DEB metadata/extraction/JAR verification passed; no host installation performed.')
