#!/usr/bin/env python3
"""Recover the owner's private upload-key backup into ignored local files, never Git."""
import hashlib
import os
from pathlib import Path
import subprocess
import zipfile

root = Path(__file__).resolve().parents[1]
os.chdir(root)
os.umask(0o077)
private = root / '.signing'
private.mkdir(mode=0o700, exist_ok=True)
key = private / 'transport-private.pem'
if not key.is_file():
    raise SystemExit('Missing private transport key. Do not create a replacement: it cannot decrypt the existing backup.')
archive = root / 'SAN-private-signing-backup.zip'
subprocess.run(['openssl', 'cms', '-decrypt', '-binary', '-inform', 'DER',
                '-in', 'deliverables/upload-key-backup.cms', '-recip', '.github/signing-export-public.pem',
                '-inkey', str(key), '-out', str(archive)], check=True)
os.chmod(archive, 0o600)
allowed = {'san-upload.p12', 'PASSWORD.txt', 'README.txt'}
with zipfile.ZipFile(archive) as z:
    if set(z.namelist()) != allowed or z.testzip() is not None:
        raise SystemExit('Unexpected or damaged private archive')
    for name in allowed:
        dest = private / name
        dest.write_bytes(z.read(name))
        os.chmod(dest, 0o600)
# Verify the backup's password/MAC and compare its public certificate with the release certificate.
recovered = private / 'recovered-certificate.pem'
subprocess.run(['openssl', 'pkcs12', '-in', str(private/'san-upload.p12'),
                '-passin', 'file:'+str(private/'PASSWORD.txt'), '-clcerts', '-nokeys',
                '-out', str(recovered)], check=True, stdout=subprocess.DEVNULL)
def fingerprint(path):
    der = subprocess.check_output(['openssl', 'x509', '-in', str(path), '-outform', 'DER'])
    return hashlib.sha256(der).hexdigest()
assert fingerprint(recovered) == fingerprint(root/'deliverables/upload-certificate.pem'), 'Signing certificate mismatch'
assert subprocess.run(['git', 'check-ignore', '-q', str(archive)]).returncode == 0, 'Private backup must be ignored'
assert not subprocess.check_output(['git', 'ls-files', str(archive), str(private)]).strip(), 'Private files must not be tracked'
print('Private upload-key backup recovered and certificate matched. No passwords were printed.')
print('Download SAN-private-signing-backup.zip and store it securely. Never publish it.')
