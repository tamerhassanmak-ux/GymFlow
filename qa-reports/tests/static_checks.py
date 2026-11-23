from pathlib import Path
import re,xml.etree.ElementTree as ET
base=Path('src/main'); files=list(base.rglob('*')); sources=list((base/'java').rglob('*.java'))
print('Java files:',len(sources),'Java lines:',sum(len(x.read_text().splitlines()) for x in sources))
errors=[]
for f in sorted((base/'resources/fxml').glob('*.fxml')):
 try:
  root=ET.parse(f).getroot(); c=root.attrib.get('{http://javafx.com/fxml/1}controller',''); source=base/'java'/Path(c.replace('.','/')+'.java'); text=source.read_text() if source.exists() else next((p.read_text() for p in sources if re.search(r'package\s+'+re.escape(c.rsplit('.',1)[0])+r'\s*;',p.read_text()) and p.stem==c.rsplit('.',1)[-1]), '')
  if not text: errors.append('Missing controller: '+c)
  handlers=re.findall(r'on\w+="\#([^\"]+)"',f.read_text())
  missing=[h for h in handlers if not re.search(r'\b'+re.escape(h)+r'\s*\(',text)]
  print(f'{f}: XML parsed; controller={c}; handlers={len(handlers)}; missing={missing}')
  errors.extend(missing)
 except Exception as e: errors.append(str(e));print('FAIL',f,e)
print('FXML static failures:',len(errors))
def props(path):
 d={}; duplicates=[]
 for n,line in enumerate(path.read_text().splitlines(),1):
  if not line.strip() or line.lstrip().startswith(('#','!')):continue
  if '=' in line:
   k=line.split('=',1)[0].strip()
   if k in d:duplicates.append((k,n))
   d[k]=line.split('=',1)[1]
 return d,duplicates
a,ad=props(base/'resources/i18n/messages_ar.properties');e,ed=props(base/'resources/i18n/messages_en.properties')
print('Arabic keys:',len(a),'English keys:',len(e)); print('Missing Arabic:',sorted(e.keys()-a.keys()));print('Missing English:',sorted(a.keys()-e.keys()));print('Duplicate Arabic:',ad);print('Duplicate English:',ed)
for lang,values in [('ar',a),('en',e)]:
 missing=[]
 for f in (base/'resources/fxml').glob('*.fxml'):
  for key in re.findall(r'="%([^\"]+)"',f.read_text()):
   if key not in values:missing.append((str(f),key))
 print('Missing FXML resource keys',lang,missing)
 errors.extend(missing)

# Controller validation strings are also resources: FXML-only scans missed this defect.
for lang, values in [('ar',a),('en',e)]:
 missing=[]
 for source in sources:
  for key in re.findall(r'getString\(\s*"([^"\n]+)"\s*\)', source.read_text()):
   if '.' in key and key not in values:missing.append((str(source),key))
 print('Missing Java resource keys',lang,missing)
 errors.extend(missing)

errors.extend(ad+ed+list(a.keys()^e.keys()))
raise SystemExit(1 if errors else 0)
