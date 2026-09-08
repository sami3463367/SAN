#!/usr/bin/env python3
"""Prove original non-code assets survive Android packaging byte-for-byte."""
import hashlib, json, sys, zipfile
from pathlib import Path
root=Path(__file__).resolve().parents[1]
package=Path(sys.argv[1]); checked=[]
with zipfile.ZipFile(root/'app/libs/original-game.jar') as original,zipfile.ZipFile(package) as target:
    is_bundle=package.suffix=='.aab'
    names=set(target.namelist())
    for item in original.infolist():
        if item.is_dir() or item.filename.endswith('.class') or item.filename.startswith('META-INF/'):continue
        dest=('base/root/' if is_bundle else '')+item.filename
        assert dest in names, f'Missing original asset: {dest}'
        data=target.read(dest)
        assert data==original.read(item),f'Modified original asset: {dest}'
        checked.append(item.filename)
    assert any(n.endswith('classes.dex') for n in names),'No compiled game code'
    assert not any(n.endswith('.so') for n in names),'Unexpected native dependency'
print(json.dumps({'package':str(package),'sha256':hashlib.sha256(package.read_bytes()).hexdigest(),
    'original_assets_verified':len(checked),'byte_identical':True,'native_libraries':0},indent=2))
