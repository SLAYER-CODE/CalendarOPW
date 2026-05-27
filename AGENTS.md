Guía para agentes — leer antes de editar o ejecutar

Mantener este archivo mínimo: incluir solo hechos que un agente automático probablemente no detecte.

Hechos clave
- Usa el Gradle wrapper desde la raíz del repo: ./gradlew. Prefiere tareas de módulo explícitas.
- El núcleo JVM está en el módulo :core. Mains importantes:
  - ServerMain.kt: inicia DiscoveryBroadcaster (UDP) y el servidor WebSocket (usar para servidor completo).
  - Main.kt: inicia solo el servidor WebSocket (más ligero).
  - ClientMain.kt: escucha discovery (UDP) y luego se conecta como cliente WebSocket.

Cómo ejecutar (comandos exactos)
- Construir todo: ./gradlew build
- Ejecutar servidor completo (broadcaster + WebSocket): ./gradlew :core:runServer
- Ejecutar cliente (discovery -> connect): ./gradlew :core:runClient
- Ejecutar linuxApp (niri/Wayland): _JAVA_AWT_WM_NONREPARENTING=1 ./gradlew :linuxApp:run (necesario para que XWayland reenvíe resize events a Skiko)

Red y discovery (imprescindible)
- Servidor WebSocket: puerto TCP 8080, path /sync (Ktor Netty). El cliente se conecta con host + puerto 8080 y path /sync.
- Discovery: puerto UDP 9999. El broadcaster envía "DISCOVER_SERVER:8080" a 255.255.255.255 cada 5s; el listener abre DatagramSocket(9999) y devuelve packet.address.hostAddress del primer paquete recibido.
- Estos valores están hard-coded en core y deben cambiarse juntos (DiscoveryBroadcaster + DiscoveryListener).

Persistencia y archivos locales
- Identidad de dispositivo: DeviceManager usa un proveedor basado en archivo que lee/escribe device.id en el directorio de trabajo para ejecuciones JVM (Android usa un proveedor específico en androidApp). Se crea perezosamente en la primera ejecución; no lo borres si quieres conservar la misma identidad.
- El repo incluye un archivo SQLite kls_database.db en la raíz. .gitignore ignora *.db, pero las BD locales afectan el estado en tiempo de ejecución; no las borres salvo que sea intencional.

Build / codegen
- SQLDelight está configurado en :core (app.cash.sqldelight) y usa el driver sqlite; una build normal de Gradle ejecuta la generación necesaria — no hay paso de codegen manual.

Pruebas
- No hay suites de pruebas automáticas en el repo. Usa ./gradlew build para verificar compilación y tareas runtime.

Peligros comunes para ediciones automáticas
- No elimines ni regeneres device.id ni la BD local sin entender los efectos (identidad de dispositivo, estado persistido).
- Si cambias puertos o el payload de discovery, edita ambos: broadcaster y listener en core/src/main/kotlin/org/distributed/calendar/core/discovery.
- Evita ejecutar broadcaster y cliente simultáneamente en el mismo directorio de trabajo (comparten device.id y colisionan). Si es necesario, ejecuta desde directorios distintos o sobrescribe DeviceIdProvider.

Dónde mirar primero (fuente de verdad)
- core/build.gradle.kts (define runServer/runClient y la configuración de SQLDelight)
- core/src/main/kotlin/ (ServerMain.kt, Main.kt, ClientMain.kt, discovery y las implementaciones WebSocket)
- settings.gradle.kts (muestra los módulos incluidos :core, :core-common, :androidApp, :linuxApp)

Si falta algo
- Haz una pregunta corta solo para políticas de equipo no documentadas (branch/PR/reglas de release). En lo demás, prefiere fuentes ejecutables (archivos Gradle, mains) sobre la prosa.
