# Plan: System Tray + Background en Linux App

## Archivo a modificar
`linuxApp/src/main/kotlin/LinuxMain.kt`

## Cambio 1: Reemplazar imports

**Eliminar:**
```kotlin
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
```

**Agregar:**
```kotlin
import androidx.compose.ui.window.Tray
import java.awt.Color
import java.awt.image.BufferedImage
```

## Cambio 2: Agregar función `createTrayIcon()` antes de `fun main()`

```kotlin
private fun createTrayIcon(): BufferedImage {
    val img = BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)
    val g = img.createGraphics()
    g.color = Color(123, 31, 162)
    g.fillRect(0, 0, 16, 16)
    g.dispose()
    return img
}
```

## Cambio 3: Reemplazar todo el bloque `application { ... }`

**Eliminar desde `application {` hasta el final del archivo.**

**Reemplazar con:**

```kotlin
    application {
        val windowState = rememberWindowState(
            width = screen.width.dp,
            height = screen.height.dp
        )
        var isVisible by remember { mutableStateOf(true) }
        var events by remember { mutableStateOf(listOf<EventUiModel>()) }
        var peers by remember { mutableStateOf(listOf<PeerUiModel>()) }

        LaunchedEffect(syncEngine) {
            syncEngine.addChangeListener {
                events = syncEngine.getEvents().map { it.toUiModel() }
                peers = syncEngine.getDevices().map { it.toPeerUiModel() }
            }
        }

        Tray(
            icon = createTrayIcon(),
            tooltip = "Distributed Calendar",
            onAction = { isVisible = !isVisible },
            menu = {
                Item("Show") { isVisible = true }
                Item("Quit") { exitApplication() }
            }
        )

        if (isVisible) {
            Window(
                onCloseRequest = { isVisible = false },
                title = "Distributed Calendar — Linux",
                state = windowState
            ) {
                CalendarTheme {
                    LinuxDesktopApp(
                        events = events,
                        peers = peers,
                        onCreateEvent = { title, description, duration ->
                            syncEngine.createEvent(title, description, duration)
                        }
                    )
                }
            }
        }
    }
```

## Resumen del cambio

- **Antes**: `onCloseRequest = ::exitApplication` → cerraba ventana y mataba el proceso
- **Después**: `onCloseRequest = { isVisible = false }` → esconde la ventana; el proceso sigue vivo con la red en background
- **Tray icon**: ícono púrpura 16×16 en el system tray con menú Show/Quit
- **Red**: las coroutines de `CoroutineScope(Dispatchers.IO)` siguen ejecutándose porque el `application {}` con `Tray` mantiene el JVM vivo
- **Sin nuevas dependencias**: `Tray` viene incluido en `compose.desktop.currentOs`
