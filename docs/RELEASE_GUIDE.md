# 🚀 Automated Release Guide

This project is configured to automatically generate GitHub releases when you create a version tag.

## 📋 How to Create a Release

### 1. Make sure everything is ready

```bash
# Verify that everything builds correctly
./gradlew build

# Verify there are no uncommitted changes
git status
```

### 2. Create a version tag

```bash
# Format: v[MAJOR].[MINOR].[PATCH]
# Examples: v1.0.0, v1.2.3, v2.0.0

git tag v1.0.0
```

**Versioning Convention (Semantic Versioning):**
- **MAJOR** (1.x.x): Incompatible changes
- **MINOR** (x.1.x): New backward-compatible functionality
- **PATCH** (x.x.1): Bug fixes

### 3. Push the tag to GitHub

```bash
git push origin v1.0.0
```

### 4. Wait for GitHub Actions

1. Go to your repository on GitHub
2. Click on the **Actions** tab
3. You will see the "Create Release" workflow running
4. Wait ~5-10 minutes (depends on GitHub load)

### 5. Ready! Your release is published

Go to the **Releases** tab on GitHub and you will see:

- ✅ `ArgentumForge-1.0.0.jar` - Cross-platform JAR
- ✅ `ArgentumForge-1.0.0-windows.zip` - Windows Executable
- ✅ `checksums.txt` - SHA256 Checksums
- ✅ Automatic release notes

## 🔧 Useful Commands

### View all tags
```bash
git tag
```

### Delete a local tag
```bash
git tag -d v1.0.0
```

### Delete a remote tag (careful!)
```bash
git push origin --delete v1.0.0
```

### Create a tag with a message
```bash
git tag -a v1.0.0 -m "First stable version"
```

## 📦 What's included in each Release

### Cross-platform JAR
- **File**: `ArgentumForge-{version}.jar`
- **Size**: ~50-100 MB (includes all dependencies)
- **Requirements**: Java 17 or higher
- **Platforms**: Windows, Linux, macOS
- **Execution**: `java -jar ArgentumForge-{version}.jar`

### Windows Executable
- **File**: `ArgentumForge-{version}-windows.zip`
- **Size**: ~200-300 MB (includes Java Runtime)
- **Requirements**: None
- **Platforms**: Windows 10/11
- **Execution**: Unzip and run `ArgentumForge.exe`
- **Includes**:
  - Embedded Java Runtime
  - `resources/` folder with assets
  - `lang/` folder with translations
  - `grh_library.json` and `Triggers.json` files

## ⚠️ Important Notes

### ❌ DO NOT do this:
- Do not create tags with names like `test`, `beta`, `release` (use `v1.0.0-beta` instead)
- Do not delete tags that already have published releases
- Do not push tags without testing the build locally

### ✅ Best Practices:
- Always test `./gradlew build` before creating a tag
- Use Semantic Versioning (v1.0.0, v1.1.0, v2.0.0)
- Document important changes in the commit before the tag
- Create tags from the `main` or `master` branch

## 🐛 Troubleshooting

### Workflow fails on GitHub Actions

1. Go to **Actions** → Click on the failed workflow
2. Check the logs to see what step failed
3. Common issues:
   - **JAR not found**: Verify `shadowJar` builds correctly
   - **jpackage fails**: Verify `resources/icon.ico` exists
   - **Permissions**: Workflow needs `contents: write` (already configured)

### I want to change release notes

1. Go to **Releases** on GitHub
2. Click **Edit** on the release
3. Modify text and save

### I want to add more files to the release

Edit `.github/workflows/release.yml` and add files in the `files:` section:

```yaml
files: |
  build/libs/ArgentumForge-${{ steps.version.outputs.version }}.jar
  ArgentumForge-${{ steps.version.outputs.version }}-windows.zip
  checksums.txt
  README.md  # ← Add here
```

## 📚 Resources

- [Semantic Versioning](https://semver.org/)
- [GitHub Releases Documentation](https://docs.github.com/en/repositories/releasing-projects-on-github)
- [Git Tagging](https://git-scm.com/book/en/v2/Git-Basics-Tagging)

---

**Ready for your first release?** 🎉

```bash
git tag v1.0.0-beta5
git push origin v1.0.0-beta5
```
