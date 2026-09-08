#!/usr/bin/env python3
"""Reproducible, non-executing inventory of the supplied JAR and its packed string table."""
import hashlib, json, struct, zipfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
jar=ROOT/'app/libs/original-game.jar'
sha=lambda b:hashlib.sha256(b).hexdigest()
with zipfile.ZipFile(jar) as z:
    entries=[]
    for item in z.infolist():
        if item.is_dir():continue
        path=Path(item.filename)
        assert not path.is_absolute() and '..' not in path.parts
        data=z.read(item)
        row={'path':item.filename,'bytes':len(data),'sha256':sha(data)}
        if data.startswith(b'\x89PNG\r\n\x1a\n'):row['dimensions']=list(struct.unpack('>II',data[16:24]))
        entries.append(row)
    data=z.read('bin.all'); pos=0
    def read(fmt):
        global pos
        size=struct.calcsize(fmt);value=struct.unpack_from(fmt,data,pos)[0];pos+=size;return value
    def skip(n):
        global pos
        pos+=n
    def string():
        global pos
        n=read('>H');s=data[pos:pos+n].decode('latin1');pos+=n;return s
    palettes=read('>h')
    for _ in range(palettes):skip(read('>i'))
    fonts=[]
    for _ in range(read('>h')):
        name=string();variants=read('>h');fonts.append({'name':name,'variants':variants})
        for _ in range(variants):skip(4+2+256*4)
    for _ in range(read('>h')):skip(8)
    languages=[]
    for _ in range(read('>h')):
        ident=read('>i');strings=[string() for _ in range(read('>h'))];font=read('>h')
        languages.append({'id':ident,'font':font,'strings':strings})
    result={'source_sha256':sha(jar.read_bytes()),'manifest':z.read('META-INF/MANIFEST.MF').decode(),
            'palette_count':palettes,'fonts':fonts,'languages':languages,'entries':entries}
    (ROOT/'docs/asset-inventory.json').write_text(json.dumps(result,indent=2,ensure_ascii=False)+'\n')
    print(f'{len(entries)} entries, {len(languages)} languages, {palettes} palettes')
    for i,s in enumerate(languages[0]['strings']):
        if any(w in s.lower() for w in ['press','key','move','weapon','control','attack','drive','car','saint','row']):print(i,s)
