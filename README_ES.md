<div align='center'>
    <br/>
    <p align='right'><a href="README_ES.md">[EN] Read in English</a></p>
    <img width="1024" height="1024" alt="ChatGPT Image 3 dic 2025, 01_55_12" src="https://github.com/user-attachments/assets/77ae11ca-8b34-489d-bf8b-010889771a25" />
    <a target="_blank"><img src="https://img.shields.io/badge/Built%20in-Java_17-43ca1f.svg?style=flat-square"></img></a>
    <a target="_blank"><img src="https://img.shields.io/badge/Made%20in-IntelliJ%20Community-be27e9.svg?style=flat-square"></img></a>
    <a target="_blank"><img src="https://img.shields.io/badge/License-GNU%20General%20Public%20License%20-e98227.svg?style=flat-square"></img></a>
</div>

<h1>Argentum Forge - Editor de Mapas para Argentum Online</h1>

  <b>Argentum Forge</b> es un editor de mapas y cliente para Argentum Online portado a Java utilizando LWJGL3.<br/><br/>
</p>

## Características Principales
- **Sistema de Deshacer/Rehacer (Undo/Redo):** Historial completo para tiles, bloqueos, NPCs y objetos.
- **Herramientas de Edición Avanzadas:**
  - **Bote de Pintura (Bucket Fill):** Relleno eficiente de áreas usando algoritmo BFS.
  - **Pinceles Especiales:** Pinceles Cuadrados, Circulares y tipo Scatter (Dispersión) para terrenos naturales.
  - **Operaciones Masivas:** Comandos optimizados para cambios de gran escala sin pérdida de rendimiento.

# Discord
Para aquellos que quieran colaborar con el proyecto, ¡son más que bienvenidos! Pueden comunicarse con nosotros en nuestro <a href="https://discord.gg/RtsGRqJVt9">Discord.</a>

# Requisitos

- <a href="https://www.oracle.com/java/technologies/downloads/#java17" target="_blank">Kit de Desarrollo de Java (JDK) 17</a> o superior
- <a href="https://www.jetbrains.com/idea/download/" target="_blank">IntelliJ IDEA</a> (recomendado), <a href="https://netbeans.apache.org/" target="_blank">NetBeans</a>, <a href="https://www.eclipse.org/downloads/" target="_blank">Eclipse</a> o cualquier IDE de Java
- Gradle (gestionado automáticamente por el IDE)

# Dependencias

El proyecto utiliza las siguientes dependencias principales (gestionadas por Gradle):
- LWJGL 3.3.3
- JOML 1.10.5
- Dear ImGui 1.86.11
- TinyLog 2.7.0

# Cómo compilar y ejecutar

1. Clonar el repositorio:
```bash
https://github.com/Lorwik/Argentum-Forge.git
```

2. Abrir el proyecto:
   - En IntelliJ IDEA: Ve a `File > Open` y selecciona la carpeta del proyecto
   - El IDE descargará automáticamente todas las dependencias a través de Gradle

3. Compilar el proyecto:
   - Usando el IDE: Haz clic en el botón 'Build Project' o presiona Ctrl+F9
   - Usando Gradle directamente: `./gradlew build`

4. Ejecutar el proyecto:
   - Localiza la clase principal `org.argentumforge.engine.Main`
   - Haz clic derecho y selecciona 'Run' o presiona Shift+F10

# Notas de Desarrollo

- El proyecto utiliza Gradle para la gestión de dependencias
- Las librerías nativas se descargan automáticamente según tu sistema operativo
- Compatible con Windows, Linux y MacOS (x64 & arm64)
- Asegúrate de tener los drivers gráficos actualizados para un rendimiento óptimo de OpenGL

# Imágenes

<img width="1918" height="1078" alt="image" src="https://github.com/user-attachments/assets/131cdca3-03a6-4c0d-a855-1e0b5a07da51" />

<h1>Agradecimientos:</h1>
<br/>

<div align='center'>

<p>
  <b>Pablo Marquez (Morgolock) creador de Argentum Online</b> <br/>
  <a target="_blank" href="https://www.gs-zone.org/temas/argentum-online-en-java-opengl.92672/#post-785702"><b>Lord Fers</b> - Usuario de GS-Zone que liberó un cliente base offline en LWJGL2</a> <br/>
  Y a la gente de GS-Zone <br/>
  <a target="_blank" href="https://www.gs-zone.org/"><img src="https://user-images.githubusercontent.com/82490615/187148671-1d7f92b9-7ea1-45f2-b6f1-f53b07454d93.png"></img></a>
</p>

<h1>Cómo Contribuir:</h1>
<br/>

1. Haz un Fork del Repositorio: Haz clic en "Fork" en la esquina superior derecha de la página para crear tu propia copia.
2. Clona tu Repositorio: Clona tu fork a tu computadora usando git clone https://github.com/TU_USUARIO/Argentum-Forge.git
3. Crea una Rama: Crea una rama con git checkout -b nombre-rama para tus cambios.
4. Realiza Cambios: Haz tus mejoras o correcciones y confírmalas.
5. Envía un Pull Request: Desde tu fork, crea un pull request para que revisemos tus cambios.

</div>
