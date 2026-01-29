# Guía del Usuario de Argentum Forge ⚒️

Bienvenido a la guía exhaustiva de **Argentum Forge**, el editor de mapas de nueva generación para el universo de Argentum Online. Esta guía está diseñada para llevarte desde la configuración inicial hasta el dominio de las herramientas de renderizado cinemático.

---

## 1. Configuración Inicial (The Wizard) 🧙

Al iniciar el editor por primera vez, se activará el **Asistente de Configuración**. Este paso es crucial para enlazar el motor con tus recursos de juego.

- **Ruta de Gráficos**: Carpeta donde se encuentran tus archivos `.bmp` o `.png`.
- **Ruta de Dats**: Carpeta `Server/Dats` (para leer NPCs, Objetos, etc.).
- **Ruta de Inits**: Carpeta `INIT` del cliente (para leer `Grh.ini` y otros).
- **Minimapa**: Se recomienda generar los colores al inicio para evitar que el minimapa aparezca en negro.

> [!TIP]
> Puedes crear diferentes **Perfiles** si trabajas en múltiples versiones de Argentum (ej: "Mod 0.13" y "Argentum Forge Dev").

---

## 2. Navegación e Interfaz 🗺️

La interfaz está diseñada para maximizar el espacio de trabajo del mapa.

- **Click Izquierdo**: Dibujar / Colocar elementos.
- **Click Derecho**: Abrir **Menú Contextual** (Edición rápida, propiedades).
- **Rueda del Ratón**: Control de **Zoom dinámico**.
- **Arrastrar con Click Central**: Desplazamiento de cámara fluido.

### El Panel de Capas (1-4)
1. **Capa 1**: Suelos y superficies base.
2. **Capa 2**: Costas, bordes y detalles a nivel de suelo.
3. **Capa 3**: Árboles, casas, paredes (capa de cobertura media).
4. **Capa 4**: Techos y copas de árboles (capa de cobertura alta).

---

## 3. Atajos de Teclado (Mastering Shortcuts) ⌨️

Dominar los atajos te permitirá trabajar un 50% más rápido.

| Comando | Tecla |
| :--- | :--- |
| **Deshacer / Rehacer** | `Ctrl + Z` / `Ctrl + Y` |
| **Guardar Mapa** | `Ctrl + S` |
| **Guardar como...** | `Ctrl + Shift + S` |
| **Modo Foto** | `N / A` (Vía menú Ver) |
| **Hacer Foto (Captura)** | `F2` |
| **Resetear Zoom** | `Ctrl + 0` |
| **Alternar Rejilla** | `G` |
| **Modo Caminata** | `W` |
| **Ir a Posición** | `Ctrl + G` |
| **Propiedades de Mapa** | `P` |

---

## 4. Herramientas de Edición 🖌️

### Pincel (Brush)
Permite pintar tiles individuales o en grupos (según el tamaño del pincel). Usa `Shift` mientras pintas para líneas rectas.

### Cubo de Relleno (Bucket)
Rellena áreas cerradas del mismo tile. Ideal para crear bosques densos o suelos uniformes rápidamente.

### Varita Mágica (Magic Wand)
Selecciona automáticamente áreas de tiles conectados del mismo tipo para edición rápida.

### Capturar (Pick)
Copia el elemento del mapa sobre el que hagas click y lo selecciona automáticamente en tu paleta. Es la forma más rápida de duplicar decoraciones.

---

## 5. Masterclass: Modo Foto Platinum 📸

El Modo Foto de Argentum Forge transforma el mapa en una escena cinemática mediante el uso del **Master Shader**.

### Efectos Disponibles:
- **Bloom (Resplandor)**: Controla el brillo de las luces. Usa el **Umbral** para decidir qué brilla y la **Intensidad** para el efecto de resplandor.
- **DoF (Profundidad de Campo)**: Desenfoca el fondo o el frente para crear un efecto de lente profesional.
- **Grano de Película**: Añade una textura cinematográfica sutil.
- **Zoom Óptico**: No solo agranda la imagen, sino que cambia la perspectiva focal de la toma.
- **Pausa del Tiempo**: Congela las animaciones de agua y NPCs para buscar el ángulo perfecto.

> [!IMPORTANT]
> Las fotos se guardan en la carpeta `/screenshots` del proyecto y NO se suben a tu repositorio de Git por defecto.

---

## 6. Resolución de Problemas (FAQ) 🛠️

- **¿Por qué el mapa carga en negro?**: Verifica que la ruta de gráficos en `Opciones` sea correcta.
- **¿Por qué no puedo editar la Capa 3?**: Asegúrate de que la Capa 3 esté marcada como "Visible" y sea la "Capa Activa" en el panel lateral.
- **El editor va lento**: Si usas Shaders pesados en el Modo Foto, el rendimiento puede bajar en PCs antiguos. Desactiva el Bloom si no lo estás usando.

---
*Argentum Forge - Creado con pasión para la comunidad de Argentum Online.*
