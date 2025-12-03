# Plan de Eliminación del Sistema de Networking

## Objetivo
Eliminar completamente el sistema de networking de Argentum Forge, ya que es un editor de mundo standalone que no requiere conexión a servidor.

## Archivos y Carpetas a Eliminar

### 1. Carpeta completa de networking
- `src/main/java/org/argentumforge/network/` (completa)

### 2. Formularios relacionados con conexión
- `FConnect.java` - Formulario de conexión al servidor
- Posiblemente otros formularios que dependan de networking

## Archivos a Modificar

### 1. Engine.java
- Eliminar import de `Connection`
- Eliminar lógica de conexión/desconexión
- Eliminar referencias a Connection.INSTANCE

### 2. FCreateCharacter.java
- Eliminar import de `Connection`
- Eliminar código que envía datos al servidor
- Convertir en funcionalidad local si es necesario

### 3. UserInventory.java
- Eliminar import de `Protocol`
- Eliminar llamadas a Protocol para sincronización

### 4. FSpawnList.java
- Eliminar import de `Protocol`
- Eliminar sincronización con servidor

### 5. FTrainer.java
- Eliminar import de `Protocol`
- Convertir en funcionalidad local

### 6. FMSG.java
- Eliminar import de `Protocol`
- Simplificar para uso local

## Pasos de Implementación

1. **Fase 1: Análisis**
   - Identificar todas las dependencias de networking
   - Determinar qué funcionalidad debe mantenerse localmente
   - Identificar formularios que pueden eliminarse completamente

2. **Fase 2: Modificación de archivos**
   - Modificar Engine.java para eliminar Connection
   - Actualizar formularios para funcionar sin servidor
   - Eliminar o simplificar formularios de red

3. **Fase 3: Eliminación**
   - Eliminar carpeta network/ completa
   - Eliminar FConnect.java
   - Limpiar imports no utilizados

4. **Fase 4: Verificación**
   - Compilar proyecto
   - Verificar que no hay errores
   - Probar funcionalidad básica del editor

## Consideraciones

- El editor debe funcionar completamente offline
- Toda la funcionalidad de edición debe ser local
- Guardar/cargar mapas debe ser mediante archivos locales
- No se requiere autenticación ni login

## Estado
- [ ] Análisis completado
- [ ] Archivos modificados
- [ ] Networking eliminado
- [ ] Proyecto compilado exitosamente
- [ ] Funcionalidad verificada
