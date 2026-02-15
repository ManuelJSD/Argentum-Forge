<div align='center'>
    <p align='right'><a href="README_EN.md">🇬🇧 Read in English</a></p>
    <img width="512" height="512" alt="Argentum Forge Logo" src="https://github.com/user-attachments/assets/77ae11ca-8b34-489d-bf8b-010889771a25" />
    <br/>
    <a target="_blank"><img src="https://img.shields.io/badge/Built%20in-Java_17-43ca1f.svg?style=flat-square"></img></a>
    <a target="_blank"><img src="https://img.shields.io/badge/Made%20in-IntelliJ%20Community-be27e9.svg?style=flat-square"></img></a>
    <a target="_blank"><img src="https://img.shields.io/badge/License-GNU%20General%20Public%20License%20-e98227.svg?style=flat-square"></img></a>
</div>

<h1>Argentum Forge - Editor de Mapas para Argentum Online</h1>

<p>
Un potente y moderno editor de mapas para Argentum Online construido con Java y LWJGL3. Crea, edita y prueba tus mundos de juego con una interfaz intuitiva y herramientas de nivel profesional.
</p>

## 🎮 Para Usuarios Finales

### Inicio Rápido (Sin Conocimientos Técnicos Requeridos)

1. **Descarga la última versión** desde la [página de Releases](https://github.com/ManuelJSD/Argentum-Forge/releases/latest)
2. **Elige tu versión:**
   - **Usuarios de Windows (Recomendado)**: Descarga `ArgentumForge-X.X.X-windows.zip`
     - No necesita instalación de Java
     - Extrae y ejecuta `ArgentumForge.exe`
   - **Todas las Plataformas**: Descarga `ArgentumForge-X.X.X.jar`
     - Requiere [Java 17+](https://adoptium.net/)
     - Ejecuta con: `java -jar ArgentumForge-X.X.X.jar`

3. **Configuración inicial:**
   - El Asistente de Configuración te guiará en la configuración de rutas esenciales
   - Apunta a tus archivos de Argentum Online (gráficos, DATs, música)
   - Configura tu idioma y ajustes de pantalla preferidos

¡Eso es todo! Ya estás listo para empezar a crear mapas.

## ✨ Características Principales

### Edición Integral de Mapas
- **Sistema Multicapa:** Edita hasta 4 capas gráficas simultáneamente
- **Gestión de Entidades:** Coloca y configura NPCs, Objetos y Triggers con previsualización en vivo
- **Control de Colisiones:** Herramienta de 'Bloqueo' precisa para definir áreas transitables y no transitables
- **Editor de Traslados:** Gestiona conexiones entre mapas y puntos de teletransporte

### Herramientas Avanzadas
- **Sistema de Deshacer/Rehacer:** Historial completo para tiles, bloqueos, NPCs y objetos
- **Bote de Pintura:** Relleno eficiente de áreas grandes usando algoritmo BFS
- **Pinceles Inteligentes:** Cuadrados, Circulares y de Dispersión para creación de terrenos naturales
- **Generador de Minimapas:** Generación en tiempo real de vistas previas del mapa
- **Auto-Tiler:** Colocación inteligente de tiles para transiciones de terreno sin costuras

### Pruebas y Usabilidad
- **Modo Caminata:** Prueba tu mapa instantáneamente con un personaje jugable para verificar colisiones y triggers
- **Asistente de Configuración Inicial:** Configuración guiada para nuevos usuarios
- **Internacionalización (i18n):** Soporte nativo para Inglés, Español y Portugués
- **Sistema de Audio Mejorado:** Selector de música integrado con soporte para MP3, WAV, MIDI y OGG

## 💬 Comunidad

Únete a nuestro [servidor de Discord](https://discord.gg/RtsGRqJVt9) para:
- Obtener ayuda y soporte
- Compartir tus creaciones
- Colaborar en el proyecto
- Reportar bugs y sugerir características

---

## 👨‍💻 Para Desarrolladores

### Requisitos

- [Kit de Desarrollo de Java (JDK) 17](https://www.oracle.com/java/technologies/downloads/#java17) o superior
- [IntelliJ IDEA](https://www.jetbrains.com/idea/download/) (recomendado), [NetBeans](https://netbeans.apache.org/), [Eclipse](https://www.eclipse.org/downloads/) o cualquier IDE de Java
- Gradle (gestionado automáticamente por el IDE)

### Dependencias

El proyecto utiliza las siguientes dependencias principales (gestionadas por Gradle):
- LWJGL 3.3.3
- JOML 1.10.5
- Dear ImGui 1.86.11
- TinyLog 2.7.0

### Cómo Compilar y Ejecutar

1. **Clonar el repositorio:**
```bash
git clone https://github.com/ManuelJSD/Argentum-Forge.git
cd Argentum-Forge
```

2. **Abrir el proyecto:**
   - En IntelliJ IDEA: Ve a `File > Open` y selecciona la carpeta del proyecto
   - El IDE descargará automáticamente todas las dependencias a través de Gradle

3. **Compilar el proyecto:**
   - Usando el IDE: Haz clic en el botón 'Build Project' o presiona `Ctrl+F9`
   - Usando Gradle directamente: `./gradlew build`

4. **Ejecutar el proyecto:**
   - Localiza la clase principal `org.argentumforge.Main`
   - Haz clic derecho y selecciona 'Run' o presiona `Shift+F10`

### Crear un Release

El proyecto usa GitHub Actions para releases automáticos:

```bash
# Crear un tag de versión
git tag v1.0.0
git push origin v1.0.0

# GitHub Actions automáticamente:
# - Compilará el JAR
# - Creará el ejecutable Windows
# - Publicará el release con ambos archivos
```

### Documentación

- [TRANSLATIONS_ES.md](docs/TRANSLATIONS_ES.md)
- [USER_GUIDE_ES.md](docs/USER_GUIDE_ES.md) **¡Nuevo!**
- [RELEASE_GUIDE_ES.md](.github/RELEASE_GUIDE_ES.md) para instrucciones detalladas.

### Notas de Desarrollo

- El proyecto utiliza Gradle para la gestión de dependencias
- Las librerías nativas se descargan automáticamente según tu sistema operativo
- Compatible con Windows, Linux y macOS (x64 & arm64)
- Asegúrate de tener los drivers gráficos actualizados para un rendimiento óptimo de OpenGL
- Ejecuta `./gradlew spotlessApply` antes de hacer commit para formatear el código

## 📸 Capturas de Pantalla

<img width="1366" height="768" alt="image" src="https://github.com/user-attachments/assets/8a72836d-55a5-43c7-b1e5-c8c64e1845a6" />

## 🤝 Cómo Contribuir

¡Damos la bienvenida a las contribuciones! Aquí te explicamos cómo empezar:

1. **Haz un Fork del Repositorio:** Haz clic en "Fork" en la esquina superior derecha
2. **Clona tu Fork:** 
   ```bash
   git clone https://github.com/TU_USUARIO/Argentum-Forge.git
   ```
3. **Crea una Rama:** 
   ```bash
   git checkout -b feature/nombre-de-tu-caracteristica
   ```
4. **Realiza Cambios:** Implementa tus mejoras o correcciones y confírmalas
5. **Envía un Pull Request:** Desde tu fork, crea un pull request para revisión

### Guías de Contribución

- Sigue el estilo de código existente (usa `./gradlew spotlessApply`)
- Escribe mensajes de commit claros
- Prueba tus cambios exhaustivamente
- Actualiza la documentación si es necesario

## 💖 Apoyar el Proyecto

Si encuentras útil este proyecto y quieres apoyar su desarrollo, ¡puedes dejar una estrella ⭐ en el repositorio!

Además, si te sientes generoso, puedes hacer una donación en criptomonedas:

- **USDT (TRC20):** `TMBg4fdAnWcUFJALY74U2m8s4jVs2UDasA`

## 📄 Licencia

Este proyecto está licenciado bajo la Licencia Pública General GNU v3.0 - consulta el archivo LICENSE para más detalles.

---

<div align='center'>
Hecho con ❤️ por Lorwik
</div>
