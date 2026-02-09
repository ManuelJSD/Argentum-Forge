# 🚀 Guía de Releases Automáticos

Este proyecto está configurado para generar releases automáticamente en GitHub cuando creas un tag de versión.

## 📋 Cómo Crear un Release

### 1. Asegúrate de que todo esté listo

```bash
# Verifica que todo compile correctamente
./gradlew build

# Verifica que no haya cambios sin commitear
git status
```

### 2. Crea un tag de versión

```bash
# Formato: v[MAJOR].[MINOR].[PATCH]
# Ejemplos: v1.0.0, v1.2.3, v2.0.0

git tag v1.0.0
```

**Convención de versionado (Semantic Versioning):**
- **MAJOR** (1.x.x): Cambios incompatibles con versiones anteriores
- **MINOR** (x.1.x): Nueva funcionalidad compatible con versiones anteriores
- **PATCH** (x.x.1): Correcciones de bugs

### 3. Sube el tag a GitHub

```bash
git push origin v1.0.0
```

### 4. Espera a que GitHub Actions termine

1. Ve a tu repositorio en GitHub
2. Click en la pestaña **Actions**
3. Verás el workflow "Create Release" ejecutándose
4. Espera ~5-10 minutos (depende de la velocidad de GitHub)

### 5. ¡Listo! Tu release está publicado

Ve a la pestaña **Releases** en GitHub y verás:

- ✅ `ArgentumForge-1.0.0.jar` - JAR multiplataforma
- ✅ `ArgentumForge-1.0.0-windows.zip` - Ejecutable Windows
- ✅ `checksums.txt` - Checksums SHA256
- ✅ Notas de release automáticas

## 🔧 Comandos Útiles

### Ver todos los tags
```bash
git tag
```

### Eliminar un tag local
```bash
git tag -d v1.0.0
```

### Eliminar un tag remoto (¡cuidado!)
```bash
git push origin --delete v1.0.0
```

### Crear un tag con mensaje
```bash
git tag -a v1.0.0 -m "Primera versión estable"
```

## 📦 Qué Incluye Cada Release

### JAR Multiplataforma
- **Archivo**: `ArgentumForge-{version}.jar`
- **Tamaño**: ~50-100 MB (incluye todas las dependencias)
- **Requisitos**: Java 17 o superior
- **Plataformas**: Windows, Linux, macOS
- **Ejecución**: `java -jar ArgentumForge-{version}.jar`

### Ejecutable Windows
- **Archivo**: `ArgentumForge-{version}-windows.zip`
- **Tamaño**: ~200-300 MB (incluye Java Runtime)
- **Requisitos**: Ninguno
- **Plataformas**: Windows 10/11
- **Ejecución**: Descomprimir y ejecutar `ArgentumForge.exe`
- **Incluye**:
  - Runtime de Java embebido
  - Carpeta `resources/` con assets
  - Carpeta `lang/` con traducciones
  - Archivos `grh_library.json` y `Triggers.json`

## ⚠️ Notas Importantes

### ❌ NO hagas esto:
- No crees tags con nombres como `test`, `beta`, `release` (usa `v1.0.0-beta` en su lugar)
- No elimines tags que ya tienen releases publicados
- No subas tags sin haber probado la compilación localmente

### ✅ Buenas prácticas:
- Siempre prueba `./gradlew build` antes de crear un tag
- Usa versionado semántico (v1.0.0, v1.1.0, v2.0.0)
- Documenta cambios importantes en el commit antes del tag
- Crea tags desde la rama `main` o `master`

## 🐛 Solución de Problemas

### El workflow falla en GitHub Actions

1. Ve a **Actions** → Click en el workflow fallido
2. Revisa los logs para ver qué paso falló
3. Problemas comunes:
   - **JAR no encontrado**: Verifica que `shadowJar` compile correctamente
   - **jpackage falla**: Verifica que `resources/icon.ico` existe
   - **Permisos**: El workflow necesita `contents: write` (ya configurado)

### Quiero cambiar las notas del release

1. Ve a **Releases** en GitHub
2. Click en **Edit** en el release
3. Modifica el texto y guarda

### Quiero agregar más archivos al release

Edita `.github/workflows/release.yml` y añade archivos en la sección `files:`:

```yaml
files: |
  build/libs/ArgentumForge-${{ steps.version.outputs.version }}.jar
  ArgentumForge-${{ steps.version.outputs.version }}-windows.zip
  checksums.txt
  README.md  # ← Añade aquí
```

## 📚 Recursos

- [Semantic Versioning](https://semver.org/)
- [GitHub Releases Documentation](https://docs.github.com/en/repositories/releasing-projects-on-github)
- [Git Tagging](https://git-scm.com/book/en/v2/Git-Basics-Tagging)

---

**¿Listo para tu primer release?** 🎉

```bash
git tag v1.0.0-beta5
git push origin v1.0.0-beta5
```
