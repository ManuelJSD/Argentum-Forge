# Custom Translations Guide

Argentum Forge supports a flexible translation system that allows you to create your own language packs or modify existing ones.

## How it works

The editor looks for translation files in two places, in this order:
1.  **Local `lang/` folder**: The `lang` folder located next to the editor executable.
2.  **Internal Resources**: The built-in languages inside the application (backup).

**Priority**: Files in the local `lang/` folder always take priority. This means you can override the default languages by simply creating a file with the same name.

## Creating a new Language

1.  Navigate to the `lang` folder in the editor directory.
2.  Create a new text file with the extension `.properties`.
3.  Name it with the language code (e.g., `fr_FR.properties` for French, `it_IT.properties` for Italian).
4.  Open the file with a text editor (Notepad, VS Code, etc.).
5.  Add translation keys and values.

### Format

The format is `Key=Value`. Lines starting with `#` are comments.

Example (`it_IT.properties`):
```properties
# Italian Translation
menu.file=File
menu.file.save=Salva
menu.file.exit=Esci
wizard.welcome.message=Benvenuto in Argentum Forge!
```

## Overriding Defaults

If you want to change specific texts in the Spanish translation:
1.  Create/Open `lang/es_ES.properties`.
2.  Add only the lines you want to change (or copy the full file from our source code).
3.  Restart the editor.

## Encoding

**Important**: Ensure your `.properties` files are saved with **UTF-8** encoding to support special characters (ñ, á, é, etc.).
