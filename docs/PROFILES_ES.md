# Guía del Sistema de Perfiles

Argentum Forge te permite gestionar múltiples configuraciones (perfiles) para trabajar con diferentes Mods o Servidores simultáneamente sin mezclar sus ajustes.

## Características

*   **Aislamiento**: Cada perfil tiene su propio archivo de configuración (ej: `profiles/MyServer.ini`).
*   **Fácil Cambio**: Selecciona tu entorno de trabajo al inicio.
*   **Portabilidad**: Puedes copiar la carpeta `profiles/` para hacer copias de seguridad de tus configuraciones.

## Gestión de Perfiles

### Selector al Inicio
Al iniciar el editor, si existe más de un perfil, verás el **Selector de Perfiles**:
*   **Seleccionar**: Carga el perfil elegido e inicia el editor.
*   **Nuevo**: Crea un perfil nuevo con la configuración por defecto.
*   **Eliminar**: Borra el perfil seleccionado y su archivo de configuración.

### Primera Ejecución
En la primera ejecución, el **Asistente de Configuración** te pedirá que le des un nombre a tu primer perfil.

## Estructura de Directorios

Todas las configuraciones de perfil se guardan en la carpeta `profiles/`.
*   `profiles/Default.ini`
*   `profiles/Mod_Winter_AO.ini`
*   `profiles/Mi_Servidor_Custom.ini`

Para hacer un backup manual de un perfil, simplemente copia su archivo `.ini`.
