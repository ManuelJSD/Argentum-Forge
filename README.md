
<div align='center'>
    <br/>
    <p align='right'><a href="README_ES.md">🇪🇸 Leer en Español</a></p>
    <img width="1024" height="1024" alt="Argentum Forge Logo" src="https://github.com/user-attachments/assets/77ae11ca-8b34-489d-bf8b-010889771a25" />
    <a target="_blank"><img src="https://img.shields.io/badge/Built%20in-Java_17-43ca1f.svg?style=flat-square"></img></a>
    <a target="_blank"><img src="https://img.shields.io/badge/Made%20in-IntelliJ%20Community-be27e9.svg?style=flat-square"></img></a>
    <a target="_blank"><img src="https://img.shields.io/badge/License-GNU%20General%20Public%20License%20-e98227.svg?style=flat-square"></img></a>
</div>

<h1>Argentum Forge - World Editor for Argentum Online</h1>

<p>
A powerful, modern map editor for Argentum Online built with Java and LWJGL3. Create, edit, and test your game worlds with an intuitive interface and professional-grade tools.
</p>

## 🎮 For End Users

### Quick Start (No Technical Knowledge Required)

1. **Download the latest release** from the [Releases page](https://github.com/ManuelJSD/Argentum-Forge/releases/latest)
2. **Choose your version:**
   - **Windows Users (Recommended)**: Download `ArgentumForge-X.X.X-windows.zip`
     - No Java installation needed
     - Extract and run `ArgentumForge.exe`
   - **All Platforms**: Download `ArgentumForge-X.X.X.jar`
     - Requires [Java 17+](https://adoptium.net/)
     - Run with: `java -jar ArgentumForge-X.X.X.jar`

3. **First-time setup:**
   - The Setup Wizard will guide you through configuring essential paths
   - Point to your Argentum Online game files (graphics, DATs, music)
   - Configure your preferred language and display settings

That's it! You're ready to start creating maps.

## ✨ Key Features

### Comprehensive Map Editing
- **Multi-Layer System:** Edit up to 4 graphic layers simultaneously
- **Entity Management:** Place and configure NPCs, Objects, and Triggers with live previews
- **Collision Control:** Precise 'Block' tool to define walkable and non-walkable areas
- **Teleport Editor:** Manage map connections and warp points

### Advanced Tools
- **Undo/Redo System:** Full history for tiles, blocks, NPCs, and objects
- **Bucket Fill:** Fill large areas efficiently using BFS algorithm
- **Smart Brushes:** Square, Circle, and Scatter brushes for natural terrain creation
- **Minimap Generator:** Real-time generation of map previews
- **Auto-Tiler:** Intelligent tile placement for seamless terrain transitions

### Testing & Usability
- **Walk Mode:** Test your map instantly with a playable character to verify collisions and triggers
- **First-Time Setup Wizard:** Guided configuration for new users
- **Internationalization (i18n):** Native support for English, Spanish, and Portuguese
- **Enhanced Audio System:** Integrated music selector with support for MP3, WAV, MIDI, and OGG

## 💬 Community

Join our [Discord server](https://discord.gg/RtsGRqJVt9) to:
- Get help and support
- Share your creations
- Collaborate on the project
- Report bugs and suggest features

---

## 👨‍💻 For Developers

### Requirements

- [Java Development Kit (JDK) 17](https://www.oracle.com/java/technologies/downloads/#java17) or higher
- [IntelliJ IDEA](https://www.jetbrains.com/idea/download/) (recommended), [NetBeans](https://netbeans.apache.org/), [Eclipse](https://www.eclipse.org/downloads/) or any Java IDE
- Gradle (automatically managed by the IDE)

### Dependencies

The project uses the following main dependencies (managed by Gradle):
- LWJGL 3.3.3
- JOML 1.10.5
- Dear ImGui 1.86.11
- TinyLog 2.7.0

### How to Compile and Run

1. **Clone the repository:**
```bash
git clone https://github.com/ManuelJSD/Argentum-Forge.git
cd Argentum-Forge
```

2. **Open the project:**
   - In IntelliJ IDEA: Go to `File > Open` and select the project folder
   - The IDE will automatically download all dependencies through Gradle

3. **Build the project:**
   - Using IDE: Click on the 'Build Project' button or press `Ctrl+F9`
   - Using Gradle directly: `./gradlew build`

4. **Run the project:**
   - Locate the main class `org.argentumforge.Main`
   - Right click and select 'Run' or press `Shift+F10`

### Creating a Release

The project uses GitHub Actions for automated releases:

```bash
# Create a version tag
git tag v1.0.0
git push origin v1.0.0

# GitHub Actions will automatically:
# - Build the JAR
# - Create Windows executable
# - Publish release with both files
```

See [.github/RELEASE_GUIDE.md](.github/RELEASE_GUIDE.md) for detailed instructions.

### Development Notes

- The project uses Gradle for dependency management
- Native libraries are automatically downloaded based on your operating system
- Supports Windows, Linux and macOS (x64 & arm64)
- Make sure your graphics drivers are up to date for optimal OpenGL performance
- Run `./gradlew spotlessApply` before committing to format code

## 📸 Screenshots

<img width="1918" height="1078" alt="Argentum Forge Editor Interface" src="https://github.com/user-attachments/assets/f188b837-b803-4686-81f7-ed8f9d9b0b6e" />

## 🤝 How to Contribute

We welcome contributions! Here's how to get started:

1. **Fork the Repository:** Click on "Fork" in the top right corner of the page
2. **Clone Your Fork:** 
   ```bash
   git clone https://github.com/YOUR_USERNAME/Argentum-Forge.git
   ```
3. **Create a Branch:** 
   ```bash
   git checkout -b feature/your-feature-name
   ```
4. **Make Changes:** Implement your improvements or fixes and commit them
5. **Submit a Pull Request:** From your fork, create a pull request for review

### Contribution Guidelines

- Follow the existing code style (use `./gradlew spotlessApply`)
- Write clear commit messages
- Test your changes thoroughly
- Update documentation if needed

## 📄 License

This project is licensed under the GNU General Public License v3.0 - see the LICENSE file for details.

---

<div align='center'>
Made with ❤️ by Lorwik
</div>

