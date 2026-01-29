# Argentum Forge User Guide ⚒️

Welcome to the comprehensive guide for **Argentum Forge**, the next-generation map editor for the Argentum Online universe. This guide is designed to take you from initial setup to mastering professional cinematic rendering tools.

---

## 1. Initial Setup (The Wizard) 🧙

When you start the editor for the first time, the **Setup Wizard** will activate. This step is crucial for linking the engine with your game resources.

- **Graphics Path**: Folder where your `.bmp` or `.png` files are located.
- **Dats Path**: `Server/Dats` folder (to read NPCs, Objects, etc.).
- **Inits Path**: Client `INIT` folder (to read `Grh.ini` and others).
- **Minimap**: It is recommended to generate colors at the start to prevent the minimap from appearing black.

> [!TIP]
> You can create different **Profiles** if you work on multiple versions of Argentum (e.g., "Mod 0.13" and "Argentum Forge Dev").

---

## 2. Navigation and Interface 🗺️

The interface is designed to maximize the map workspace.

- **Left Click**: Draw / Place elements.
- **Right Click**: Open **Context Menu** (Quick editing, properties).
- **Mouse Wheel**: Dynamic **Zoom** control.
- **Middle Click Drag**: Fluid camera panning.

### The Layer Panel (1-4)
1. **Layer 1**: Ground and base surfaces.
2. **Layer 2**: Coasts, edges, and ground-level details.
3. **Layer 3**: Trees, houses, walls (middle coverage layer).
4. **Layer 4**: Roofs and treetops (high coverage layer).

---

## 3. Keyboard Shortcuts (Mastering Shortcuts) ⌨️

Mastering shortcuts will allow you to work 50% faster.

| Command | Key |
| :--- | :--- |
| **Undo / Redo** | `Ctrl + Z` / `Ctrl + Y` |
| **Save Map** | `Ctrl + S` |
| **Save As...** | `Ctrl + Shift + S` |
| **Photo Mode** | `N / A` (Via View menu) |
| **Take Photo (Screenshot)** | `F2` |
| **Reset Zoom** | `Ctrl + 0` |
| **Toggle Grid** | `G` |
| **Walking Mode** | `W` |
| **Go To Position** | `Ctrl + G` |
| **Map Properties** | `P` |

---

## 4. Editing Tools 🖌️

### Brush
Allows you to paint individual tiles or groups (based on brush size). Use `Shift` while painting for straight lines.

### Bucket
Fills enclosed areas of the same tile. Ideal for quickly creating dense forests or uniform floors.

### Magic Wand
Automatically selects connected tile areas of the same type for quick editing.

### Capture (Pick)
Copies the map element you click on and automatically selects it in your palette. It's the fastest way to duplicate decorations.

---

## 5. Masterclass: Platinum Photo Mode 📸

The Argentum Forge Photo Mode transforms the map into a cinematic scene using the **Master Shader**.

### Available Effects:
- **Bloom**: Controls the glow of lights. Use the **Threshold** to decide what glows and **Intensity** for the glow effect.
- **DoF (Depth of Field)**: Blurs the background or foreground to create a professional lens effect.
- **Film Grain**: Adds a subtle cinematic texture.
- **Optical Zoom**: Not only enlarges the image but changes the focal perspective of the shot.
- **Time Stop**: Freezes water animations and NPCs to find the perfect angle.

> [!IMPORTANT]
> Photos are saved in the project's `/screenshots` folder and are NOT uploaded to your Git repository by default.

---

## 6. Troubleshooting (FAQ) 🛠️

- **Why is the map loading black?**: Verify that the graphics path in `Options` is correct.
- **Why can't I edit Layer 3?**: Ensure that Layer 3 is marked as "Visible" and is the "Active Layer" in the side panel.
- **The editor is slow**: If you use heavy Shaders in Photo Mode, performance may drop on older PCs. Disable Bloom if you are not using it.

---
*Argentum Forge - Created with passion for the Argentum Online community.*
