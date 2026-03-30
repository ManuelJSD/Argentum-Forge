$utf8 = New-Object System.Text.UTF8Encoding $False

$files = @{
    "resources/lang/es_ES.properties" = @"

# Nuevas traducciones Prefab Editor
prefab.block=Bloqueos
prefab.trigger=Triggers
prefab.object=Objetos
prefab.npc=NPCs
prefab.particle=Partículas

# Notificaciones Editor
msg.editor.consoleActiveBlock=La consola está activa. Presiona ENTER o ESC para continuar editando.
"@

    "resources/lang/pt_BR.properties" = @"

# Nuevas traducciones Prefab Editor
prefab.block=Bloqueios
prefab.trigger=Triggers
prefab.object=Objetos
prefab.npc=NPCs
prefab.particle=Partículas

# Notificaciones Editor
msg.editor.consoleActiveBlock=O console está ativo. Pressione ENTER ou ESC para continuar editando.
"@

    "resources/lang/en_US.properties" = @"

# Nuevas traducciones Prefab Editor
prefab.block=Blocks
prefab.trigger=Triggers
prefab.object=Objects
prefab.npc=NPCs
prefab.particle=Particles

# Notificaciones Editor
msg.editor.consoleActiveBlock=Console is active. Press ENTER or ESC to resume editing.
"@
}

foreach ($item in $files.GetEnumerator()) {
    $path = $item.Key
    $content = $item.Value
    if (Test-Path $path) {
        [System.IO.File]::AppendAllText($path, $content, $utf8)
        Write-Host "Updated $($path)"
    } else {
        Write-Host "File not found: $($path)"
    }
}
