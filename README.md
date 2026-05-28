# CalendarOPW

A high-performance distributed calendar focused on discipline, routines, productivity, and real-time synchronization across multiple devices.

Built with modern Kotlin technologies including Jetpack Compose Multiplatform, WebSockets, Coroutines, and a reactive architecture designed for speed and reliability.

---

## ✨ Features

- 🔄 Real-time synchronization between devices using WebSockets
- 📡 Automatic reconnection system for unstable networks
- 💻 Cross-platform support:
  - Android
- Linux
  - Linux (linuxApp module)
  - Windows
  - Desktop JVM
- 🔔 High-priority notifications designed to never be missed
- ⚡ Low-latency event propagation inside local networks
- 🧠 Smart event persistence and recovery system
- 📅 Distributed calendar architecture
- 🛜 Offline-ready synchronization queue
- 🎯 Focused on routines, study sessions, training, and productivity
- 🔋 Optimized background execution to keep reminders active
- 🧩 Modular architecture for scalability
- 🎨 Modern UI built with Compose Multiplatform
- 🔐 Local-first approach with future encrypted sync support
- 🚀 Lightweight and fast startup
- 🧵 Fully asynchronous networking using Kotlin Coroutines
- 📦 Shared business logic using Kotlin Multiplatform

---

## 🧠 Philosophy

CalendarOPW is not just a calendar.

It is designed for people who want structure, consistency, and precision in their daily lives.

The system prioritizes important routines such as:
- study sessions
- workout plans
- productivity blocks
- habit tracking
- scheduled goals

Notifications are treated as critical events instead of passive reminders.

The goal is simple:

> Never lose track of your routine again.

---

## 🛠 Tech Stack

- Kotlin
- Compose Multiplatform
- Kotlin Coroutines
- WebSockets
- Kotlin Serialization
- Gradle Kotlin DSL
- SQLite
- JVM
- Android SDK

---

## 🌐 Synchronization System

CalendarOPW uses a distributed synchronization model over local networks.

Features include:
- automatic peer reconnection
- event broadcasting
- state recovery after connection loss
- low-overhead message transport
- real-time updates across devices

This allows Android phones and desktop systems to stay synchronized instantly without requiring heavy cloud infrastructure.

### Architecture Diagram

```
                    LINUX (linuxApp)

  ┌──────────────┐   ┌──────────────────┐   ┌─────────────────────────┐
  │ LinuxDesktop │   │  LinuxMain.kt    │   │  LnxWebSocketClient     │
  │    App.kt    │   │                  │   │                         │
  │  (Compose)   │   │ syncEngine       │   │  ┌───────────────────┐  │
  │              │   │ .createEvent()───┼──►│  │ Resend loop (c/2s)│  │
  │ onSave ──────┼──►│                  │   │  ├───────────────────┤  │
  │              │   │                  │   │  │ Heartbeat (c/5s)  │  │
  │              │   │ .addChangeList.◄─┼───┤  ├───────────────────┤  │
  └──────────────┘   └─────────┬────────┘   │  │ SYNC_REQUEST on   │  │
                               │            │  │ connect           │  │
                               │            └────────┬──────────────┘  │
                               ▼                     │                 │
                        ┌──────────────┐             │                 │
                        │  SyncEngine  │◄────────────┘                 │
                        │   (core)     │     (incoming frames)         │
                        │              │                               │
                        │ handleEvent  │  ┌────────────────────────┐   │
                        │ Create() ────┼─►│ LnxWebSocketServer     │   │
                        │              │  │ (Ktor Netty :8080)     │   │
                        │ broadcast()──┼─►│ /sync                  │   │
                        │              │  │                        │   │
                        │ handleSyncRe │  │ on packet:             │   │
                        │ quest() ─────┼─►│  SessionRegistry       │   │
                        │              │  │  .register()           │   │
                        └──────┬───────┘  │  syncEngine            │   │
                               │          │  .applyPacket()        │   │
                               │          └────────────────────────┘   │
                               ▼                                       │
                        ┌──────────────┐  ┌──────────────────┐        │
                        │  PendingPacket│  │ PacketResender   │        │
                        │    Store      │  │ (c/ 5s)          │        │
                        │ (core-common) │  │ read pending ────┼──►     │
                        │              │  │ send to sessions  │        │
                        │ persist to   │  └──────────────────┘        │
                        │ pending_pack │                              │
                        │ ets/*.json   │                              │
                        └──────────────┘                              │
                                                                      │
  Discovery:                                                          │
  ┌─────────────────┐    ┌────────────────┐                           │
  │ LnxDiscovery    │    │ LnxDiscovery   │                           │
  │ Broadcaster     │    │ Listener       │                           │
  │ (c/5s UDP :9999)───►│ (UDP :9999      │                           │
  │ "DISCOVER_SERVER│    │  10s timeout)  │                           │
  │ :8080"          │    │  → connect to  │                           │
  └─────────────────┘    │  discovered IP │                           │
                         └────────────────┘                           │
                         ───────┬───────────────────────────────────────
                               │  UDP broadcast (255.255.255.255:9999)
                               │  WebSocket (ws://<ip>:8080/sync)
                               ▼
                     ANDROID (androidApp)

  ┌─────────────┐   ┌──────────────────┐   ┌─────────────────────────┐
  │ MainAct.kt  │   │ SyncForeground   │   │ AndroidWebSocketClient  │
  │ (Compose)   │   │  Service.kt      │   │                         │
  │             │   │                  │   │  ┌───────────────────┐  │
  │ onSave ─────┼──►│ syncEngine       │   │  │ Resend job (c/2s) │  │
  │             │   │ .createEvent()   │   │  ├───────────────────┤  │
  │             │   │                  │   │  │ Heartbeat (c/5s)  │  │
  │ .addChange. ◄───┤ .addChangeList.  │   │  ├───────────────────┤  │
  └─────────────┘   └─────────┬────────┘   │  │ SYNC_REQUEST on   │  │
                              │            │  │ connect           │  │
                              │            └────────┬──────────────┘  │
                              ▼                     │                 │
                       ┌──────────────┐             │                 │
                       │  SyncEngine  │◄────────────┘                 │
                       │   (core)     │     (incoming frames)         │
                       │              │                               │
                       │ handleEvent  │  ┌────────────────────────┐   │
                       │ Create() ────┼─►│ AndroidWebSocket       │   │
                       │              │  │  Server                │   │
                       │ broadcast()──┼─►│ (Ktor Netty :8080)     │   │
                       │              │  │ /sync                  │   │
                       │ handleSyncRe │  │                        │   │
                       │ quest() ─────┼─►│ on packet:             │   │
                       │              │  │  SessionRegistry       │   │
                       └──────┬───────┘  │  .register()           │   │
                              │          │  syncEngine            │   │
                              ▼          │  .applyPacket()        │   │
                       ┌─────────────┐   └────────────────────────┘   │
                       │ SQLite DB   │                                │
                       │ (eventRepo) │                                │
                       │ calendar.db │                                │
                       └─────────────┘                                │
                                                                      │
  Discovery:                                                          │
  ┌──────────────────┐  ┌────────────────┐                            │
  │ AndroidDiscovery │  │ AndroidDiscovery│                           │
  │ Broadcaster      │  │ Listener        │                           │
  │ (c/5s UDP :9999)─┼──►│ (UDP :9999      │                           │
  │                  │  │  10s timeout)   │                           │
  └──────────────────┘  │  → connect to   │                           │
                        │ discovered IP   │                           │
                        └────────────────┘                            │

                    SHARED MODULES (core)

  core-common (JVM):
    Event, Device, SyncPacket
    PacketSerializer
    PendingPacketStore (file-backed)
    PacketDeduplicator
    DeviceManager

  core (JVM):
    SyncEngine
      ├ applyPacket()
      ├ createEvent()
      ├ broadcast() → SessionRegistry
      ├ handleSyncRequest()
      ├ handleSyncResponse()
      └ addChangeListener()
    EventRepository (interface)
    DeviceRegistry
    SessionRegistry
    PacketResender
    WebSocketServer (interface)
    WebSocketClient (interface)


            PACKET FLOW (creating an event on Linux)

  User      LinuxMain    SyncEngine    WS Server    PendStore
   │           │            │             │            │
   │ onSave    │            │             │            │
   ├──────────►│            │             │            │
   │           │ createEv() │             │            │
   │           ├───────────►│             │            │
   │           │            │ create Ev+  │            │
   │           │            │ SyncPacket  │            │
   │           │            │ applyPacket │            │
   │           │            ├─handleEvC() │            │
   │           │            │ store local  │            │
   │           │            │ broadcast()─┼───────────►│
   │           │            │             │           │
   │           │            │             │ if sess:  │
   │           │            │             │ send      │
   │           │            │             │ if NO:    │
   │           │            │             │ add pend  │
   │           │            │ notifChange │            │
   │           │            ├────┐        │            │
   │           │            │ UI │        │            │
   │           ◄────────────┤ upd│        │            │
   ◄───────────┤            │    │        │            │
   │           │            │    │        │            │
   │  Event on │            │    │        │            │
   │  screen   │            │    │        │            │
   │           │            │    │        │            │
   │           │            │    │        │ (via TCP)  │
   │           │            │    │        │            │
   ▼           ▼            ▼    ▼        ▼            ▼

                     NETWORK (TCP :8080)
                  ws://<android>:8080/sync
                         │
                         ▼
  WS Client   WS Server   SyncEngine   SQLite   UI
  (Android)  (Android)
      │           │           │          │       │
      │ Frame.Txt │           │          │       │
      ◄───────────┤           │          │       │
      │           │ deser     │          │       │
      │           │ regSess   │          │       │
      │           │ applyPkt()│          │       │
      │           ├──────────►│          │       │
      │           │           │ deser ev │       │
      │           │           │ store    │       │
      │           │           ├─────────►│       │
      │           │           │ insert   │       │
      │           │           │ persist  │       │
      │           │           │ bcast()─►│(Lx ses)│
      │           │           │ re-envía │       │
      │           │           │ notifChg │       │
      │           │           ├──────────┼──────►│
      │           │           │          │       │
      │           │           │          │update │
      │           │           │          │events │
      ▼           ▼           ▼          ▼       ▼
```

---

## 📱 Notification System

The notification engine is designed to maximize visibility and reliability.

Capabilities:
- persistent reminders
- repeated alerts
- high-priority notification channels
- background-safe scheduling
- synchronized multi-device alerts

Perfect for:
- gym routines
- study planning
- deep work sessions
- strict daily schedules

---

## 🧱 Planned Features

- ☁️ Optional cloud synchronization
- 🔐 End-to-end encrypted sync
- 👥 Shared collaborative calendars
- 📊 Productivity analytics
- 🤖 AI-assisted scheduling
- 🗂 Routine templates
- 🧭 Smart focus mode
- ⌚ Wearable device support
- 🌙 Adaptive themes
- 🔊 Sound-based critical alerts

---

## 🚀 Getting Started

Clone the repository:

```bash
git clone git@github.com:SLAYER-CODE/CalendarOPW.git
```
Run the project:

```bash
./gradlew run
```
Build:

```bash
./gradlew build
```

## 📂 Project Goals

This project aims to explore:

- distributed systems
- real-time synchronization
- reactive UI architectures
- cross-platform Kotlin development
- local-first applications
- resilient networking systems

---

## 📜 License

MIT License

---

## 👨‍💻 Author

Developed by SLAYER-CODE.
