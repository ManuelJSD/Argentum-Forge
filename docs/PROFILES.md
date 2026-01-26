# Profile System Guide

Argentum Forge allows you to manage multiple configurations (profiles) to work with different Mods or Servers simultaneously without mixing their settings.

## Features

*   **Isolation**: Each profile has its own configuration file (e.g., `profiles/MyServer.ini`).
*   **Easy Switching**: Select your target environment at startup.
*   **Portability**: You can copy the `profiles/` folder to back up your configurations.

## Managing Profiles

### Startup Selector
When you launch the editor, if more than one profile exists, you will see the **Profile Selector**:
*   **Select**: Loads the chosen profile and starts the editor.
*   **New**: Creates a fresh profile with default settings.
*   **Delete**: Removes the selected profile and its configuration file.

### First Run
On the first run, the **Setup Wizard** will ask you to name your first profile.

## Directory Structure

All profile configurations are stored in the `profiles/` folder.
*   `profiles/Default.ini`
*   `profiles/Mod_Winter_AO.ini`
*   `profiles/My_Custom_Server.ini`

To manually back up a profile, simply copy its `.ini` file.
