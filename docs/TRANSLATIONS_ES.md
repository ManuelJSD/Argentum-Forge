# Guía de Traducciones Personalizadas

Argentum Forge soporta un sistema de traducción flexible que te permite crear tus propios paquetes de idioma o modificar los existentes.

## Cómo funciona

El editor busca archivos de traducción en dos lugares, en este orden:
1.  **Carpeta local `lang/`**: La carpeta `lang` ubicada junto al ejecutable del editor.
2.  **Recursos Internos**: Los idiomas integrados dentro de la aplicación (backup).

**Prioridad**: Los archivos en la carpeta `lang/` local siempre tienen prioridad. Esto significa que puedes sobrescribir los idiomas por defecto simplemente creando un archivo con el mismo nombre.

## Crear un nuevo idioma

1.  Navega a la carpeta `lang` en el directorio del editor.
2.  Crea un nuevo archivo de texto con la extensión `.properties`.
3.  Nómbralo con el código del idioma (ej: `fr_FR.properties` para Francés, `it_IT.properties` para Italiano).
4.  Abre el archivo con un editor de texto (Notepad, VS Code, etc.).
5.  Añade las claves de traducción y sus valores.

### Formato

El formato es `Clave=Valor`. Las líneas que empiezan con `#` son comentarios.

Ejemplo (`it_IT.properties`):
```properties
# Traducción al Italiano
menu.file=File
menu.file.save=Salva
menu.file.exit=Esci
wizard.welcome.message=Benvenuto in Argentum Forge!
```

## Sobrescribir valores por defecto

Si quieres cambiar textos específicos de la traducción al español:
1.  Crea/Abre `lang/es_ES.properties`.
2.  Añade solo las líneas que quieras cambiar (o copia el archivo completo de nuestro código fuente).
3.  Reinicia el editor.

## Codificación

**Importante**: Asegúrate de que tus archivos `.properties` estén guardados con codificación **UTF-8** para soportar caracteres especiales (ñ, á, é, etc.).
