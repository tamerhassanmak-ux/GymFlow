"""Historical Files.copy anti-pattern reproduction, retained as defect evidence.
The application now uses SqliteBackupService; current recovery regressions are RI09-RI15.
This script deliberately demonstrates the OLD unsafe primitive, not current app behavior.
"""
import sqlite3,tempfile,shutil,pathlib
p=pathlib.Path(tempfile.mkdtemp(prefix='gymflow-qc-backup-'))
c=sqlite3.connect(p/'source.db');c.execute('PRAGMA journal_mode=WAL');c.execute('CREATE TABLE sample(id INTEGER PRIMARY KEY)');c.commit();c.execute('PRAGMA wal_checkpoint(TRUNCATE)');c.execute('INSERT INTO sample VALUES(1)');c.commit()
shutil.copyfile(p/'source.db',p/'backup.db')
b=sqlite3.connect(p/'backup.db');print('Committed source rows:',c.execute('select count(*) from sample').fetchone()[0]);print('Files.copy equivalent backup rows:',b.execute('select count(*) from sample').fetchone()[0]);b.close();c.close()
corrupt=p/'invalid.db';corrupt.write_text('not a sqlite database');target=p/'target.db';shutil.copyfile(p/'source.db',target);shutil.copyfile(corrupt,target)
try:
 d=sqlite3.connect(target);d.execute('PRAGMA integrity_check').fetchall()
except sqlite3.DatabaseError as e:print('Restore copy accepts invalid .db; next access:',str(e))
