"""Package the verified local development build and its corresponding GPL source."""
from pathlib import Path
import hashlib
import shutil
import zipfile

root = Path(__file__).resolve().parent
output = root.parent / 'deliverables'
output.mkdir(exist_ok=True)
apk = root / 'presentation/build/outputs/apk/debug/Texto-v1.4.0-debug.apk'
if 'BUILD SUCCESSFUL' not in (root / 'build-validation.log').read_text(encoding='utf-8-sig'):
    raise SystemExit('Run build-texto.ps1 and capture build-validation.log before packaging')
shutil.copy2(apk, output / 'Texto-1.4.0-debug.apk')
shutil.copy2(root / 'DEVICE-TESTS.md', output / 'DEVICE-TESTS.md')
shutil.copy2(root / 'VALIDATION.md', output / 'VALIDATION.md')
shutil.copy2(root / 'common/build/test-results/testDebugUnitTest/TEST-dev.texto.privacy.TwoFingerPullTest.xml', output / 'two-finger-test-results.xml')
shutil.copy2(root / 'build-validation.log', output / 'build-validation.log')
tests = root / 'common/build/test-results/testDebugUnitTest/TEST-dev.texto.privacy.RuleEngineTest.xml'
shutil.copy2(tests, output / 'privacy-test-results.xml')
shutil.copy2(root / 'common/build/test-results/testDebugUnitTest/TEST-dev.texto.privacy.VaultSessionTest.xml', output / 'vault-session-test-results.xml')
shutil.copy2(root / 'common/build/test-results/testDebugUnitTest/TEST-dev.texto.privacy.TrashRetentionTest.xml', output / 'trash-retention-test-results.xml')
excluded = {'.git', '.gradle', '.tmp', 'build', '.idea', '__pycache__', '.android-test', '.tools'}
with zipfile.ZipFile(output / 'Texto-1.4.0-source.zip', 'w', zipfile.ZIP_DEFLATED) as archive:
    for path in sorted(root.rglob('*')):
        relative = path.relative_to(root)
        if any(part in excluded for part in relative.parts): continue
        if not path.is_file() or path.name == 'local.properties': continue
        if path.suffix in {'.apk', '.log', '.jks', '.keystore', '.enc'}: continue
        if path.name in {'texto_customize.py', 'texto_refine.py', 'texto_harden.py', 'update_texto_110.py', 'implement-120.py', 'wire-120.py', 'style-120.py'}: continue
        archive.write(path, Path('Texto') / relative)
checksums = []
for name in ['Texto-1.4.0-debug.apk', 'Texto-1.4.0-source.zip']:
    path = output / name
    digest = hashlib.file_digest(path.open('rb'), 'sha256').hexdigest()
    checksums.append(f'{digest}  {name}')
    print(f'{name}: {path.stat().st_size:,} bytes; SHA256 {digest}')
(output / 'SHA256SUMS.txt').write_text('\n'.join(checksums) + '\n', encoding='utf-8')
