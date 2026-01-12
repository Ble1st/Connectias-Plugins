# GitHub Actions Workflows für Plugin Releases

Dieses Verzeichnis enthält Workflows für das automatische Builden und Releasen von Plugins.

## Workflows

### 1. `release-changed-plugins.yml` ⭐ **Empfohlen**

**Zweck:** Baut und released nur Plugins, die sich seit dem letzten Release geändert haben.

**Features:**
- ✅ Automatische Change Detection (Git-basiert)
- ✅ Baut nur geänderte Plugins
- ✅ Separate Releases für jedes Plugin
- ✅ Tag-Format: `v{version}-{plugin-name}` (kompatibel mit `GitHubPluginDownloadManager`)
- ✅ SHA256 Checksums für Sicherheit
- ✅ Plugin-Manifest wird mit hochgeladen

**Verwendung:**

1. Gehe zu **Actions** → **Release Changed Plugins**
2. Klicke auf **Run workflow**
3. Gib die Version ein (z.B. `1.0.0`)
4. Optional: Aktiviere **Force build all plugins** um alle Plugins zu bauen (ignoriert Änderungen)

**Change Detection Logik:**

Der Workflow prüft für jedes Plugin:
- **Plugin-spezifische Änderungen:** Dateien im Plugin-Verzeichnis
- **Gemeinsame Änderungen:** `build.gradle.kts`, `settings.gradle.kts`, `gradle/`, `gradle.properties`

Wenn eines dieser Kriterien erfüllt ist, wird das Plugin gebaut.

**Beispiel:**
```
Änderungen seit letztem Release:
- ping-plugin/src/main/java/.../PingPlugin.kt (geändert)
- test-plugin/ (keine Änderungen)
- build.gradle.kts (geändert - betrifft alle Plugins)

Ergebnis:
✅ ping-plugin wird gebaut (eigene Änderungen)
✅ test-plugin wird gebaut (build.gradle.kts geändert)
```

---

### 2. `release-single-plugin.yml`

**Zweck:** Wiederverwendbarer Workflow zum Bauen eines einzelnen Plugins.

**Verwendung:**

Wird normalerweise von `release-changed-plugins.yml` aufgerufen, kann aber auch direkt verwendet werden:

```yaml
jobs:
  build-plugin:
    uses: ./.github/workflows/release-single-plugin.yml
    with:
      plugin: ping-plugin
      version: 1.0.0
```

---

### 3. `release-plugins.yml` (Legacy)

**Zweck:** Baut alle Plugins zusammen in einem Release.

**⚠️ Hinweis:** Dieser Workflow ist nicht kompatibel mit `GitHubPluginDownloadManager`, da er ein anderes Tag-Format verwendet. Wird nur für Backwards-Kompatibilität beibehalten.

---

## Release-Struktur

### Tag-Format
```
v{version}-{plugin-name}
```

**Beispiele:**
- `v1.0.0-ping-plugin`
- `v1.2.3-test-plugin`
- `v2.0.0-network-info-plugin`

### Release-Assets

Jedes Release enthält:
1. **APK-Datei:** `{plugin-name}-{version}.apk`
2. **Plugin-Manifest:** `plugin-manifest.json`
3. **SHA256 Checksum:** `sha256sum.txt`

### Release-Body

Jedes Release enthält:
- Plugin-Informationen (ID, Version, Build-Datum)
- Liste der Artifacts
- Vollständiges Plugin-Manifest als JSON

---

## Workflow-Beispiele

### Beispiel 1: Normaler Release (nur geänderte Plugins)

```
Input:
- Version: 1.0.0
- Force all: false

Änderungen:
- ping-plugin/src/... (geändert)
- test-plugin/ (keine Änderungen)

Ergebnis:
✅ Release: v1.0.0-ping-plugin
❌ test-plugin wird nicht gebaut
```

### Beispiel 2: Force Build All

```
Input:
- Version: 1.0.0
- Force all: true

Ergebnis:
✅ Release: v1.0.0-ping-plugin
✅ Release: v1.0.0-test-plugin
✅ Release: v1.0.0-network-info-plugin
```

### Beispiel 3: Gemeinsame Build-Dateien geändert

```
Änderungen:
- build.gradle.kts (geändert)
- settings.gradle.kts (geändert)

Ergebnis:
✅ Alle Plugins werden gebaut (da gemeinsame Dateien geändert)
```

---

## Kompatibilität mit GitHubPluginDownloadManager

Der `GitHubPluginDownloadManager` erwartet:
- Tag-Format: `v{version}-{plugin-name}`
- Assets: `.aab` oder `.apk`, `plugin-manifest.json`, `sha256sum.txt`

✅ **`release-changed-plugins.yml`** ist vollständig kompatibel!

---

## Troubleshooting

### Problem: Plugin wird nicht gebaut obwohl es geändert wurde

**Lösung:**
1. Prüfe, ob es einen vorherigen Release-Tag gibt: `git tag -l "v*-{plugin-name}"`
2. Prüfe die Git-Historie: `git log --oneline --name-only`
3. Verwende **Force build all** um alle Plugins zu bauen

### Problem: Tag existiert bereits

**Lösung:**
Der Workflow überspringt die Tag-Erstellung automatisch, wenn der Tag bereits existiert. Verwende eine neue Version.

### Problem: APK-Datei nicht gefunden

**Lösung:**
1. Prüfe, ob der Build erfolgreich war
2. Prüfe den Build-Output-Pfad in `build.gradle.kts`
3. Prüfe, ob `assembleRelease` erfolgreich war

---

## Best Practices

1. **Semantic Versioning:** Verwende `MAJOR.MINOR.PATCH` (z.B. `1.0.0`)
2. **Versionierung:** Erhöhe die Version nur bei tatsächlichen Änderungen
3. **Change Detection:** Vertraue auf die automatische Change Detection
4. **Force Build:** Nur verwenden wenn nötig (z.B. nach Dependency-Updates)
5. **Release Notes:** Aktualisiere die Release Notes nach dem Release

---

## Migration von `release-plugins.yml`

Wenn du von `release-plugins.yml` migrierst:

1. **Alte Releases:** Bleiben erhalten (keine Breaking Changes)
2. **Neue Releases:** Verwende `release-changed-plugins.yml`
3. **Tag-Format:** Ändert sich von `0.0.1` zu `v1.0.0-ping-plugin`
4. **App-Update:** `GitHubPluginDownloadManager` unterstützt bereits das neue Format

---

## Support

Bei Problemen oder Fragen:
1. Prüfe die Workflow-Logs in GitHub Actions
2. Prüfe die Release-Seite auf GitHub
3. Prüfe die Git-Tags: `git tag -l`
