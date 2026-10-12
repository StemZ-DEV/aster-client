"""Validate public release metadata, archive contents, and checksums using Python 3."""
import hashlib
import json
from pathlib import Path
import zipfile

root = Path(__file__).resolve().parent.parent
properties = dict(line.split("=", 1) for line in (root / "gradle.properties").read_text().splitlines() if "=" in line)
version = properties["version"]
directory = root / "build" / "release" / version
prefix = f"aster-client-{version}"
expected = {f"{prefix}.jar", f"{prefix}-sources.jar", f"{prefix}-release.zip", f"{prefix}-source.zip"}

checksums = {}
for line in (directory / "SHA256SUMS.txt").read_text().splitlines():
    digest, name = line.split("  ", 1)
    assert name in expected and name not in checksums, f"Unexpected checksum entry: {name}"
    actual = hashlib.sha256((directory / name).read_bytes()).hexdigest()
    assert digest == actual, f"Checksum mismatch: {name}"
    checksums[name] = digest
assert set(checksums) == expected, "Missing release artifacts"

with zipfile.ZipFile(directory / f"{prefix}.jar") as jar:
    entries = set(jar.namelist())
    metadata = json.loads(jar.read("fabric.mod.json"))
    assert metadata["id"] == "aster" and metadata["version"] == version
    assert metadata["environment"] == "client"
    assert metadata["depends"]["minecraft"] == "~" + properties["minecraft_version"]
    assert metadata["depends"]["java"] == ">=25"
    for required in ["LICENSE_aster", "THIRD_PARTY_NOTICES.md", "assets/aster/icon.png", "assets/aster/font/ofl.txt", "assets/aster/font/outfit.ttf"]:
        assert required in entries, f"Missing release resource: {required}"
    assert "dev/aster/client/AsterClient.class" in entries
    assert not any(name.startswith(("net/minecraft/", "dev/aster/test/", "META-INF/jars/")) for name in entries), "Production JAR contains test code or bundled dependencies"

with zipfile.ZipFile(directory / f"{prefix}-release.zip") as bundle:
    assert hashlib.sha256(bundle.read(f"{prefix}/{prefix}.jar")).hexdigest() == checksums[f"{prefix}.jar"]
    for required in ["LICENSE", "README.md", "CHANGELOG.md", "THIRD_PARTY_NOTICES.md", "docs/INSTALL.md", "docs/screenshots/flow-in-game.png"]:
        assert f"{prefix}/{required}" in bundle.namelist(), f"Missing release documentation: {required}"

with zipfile.ZipFile(directory / f"{prefix}-source.zip") as source:
    entries = source.namelist()
    for required in ["build.gradle", "gradlew", "gradle/wrapper/gradle-wrapper.jar", "gradle/wrapper/LICENSE.gradle", "src/main/resources/assets/aster/font/ofl.txt", ".github/workflows/ci.yml"]:
        assert f"{prefix}/{required}" in entries, f"Incomplete source archive: {required}"
    forbidden = ("build", "run", "logs", ".gradle", ".git")
    assert not any(set(Path(name).parts[1:]).intersection(forbidden) for name in entries), "Source archive contains runtime or private files"

print(f"Verified Aster {version}: four artifacts, checksums, metadata, licenses, and clean archive contents.")
