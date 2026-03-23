# Focused

> Lock yourself to one window. Get things done.

Focused is a Windows productivity app that locks your desktop to a single chosen window for a set amount of time — like iPhone's guided access, but for your PC.

---

## Status

🚧 **In active development** — Session 1 complete (project skeleton).

| Session | What gets built | Status |
|---------|----------------|--------|
| 1 | Project skeleton, architecture, GitHub setup | ✅ Done |
| 2 | Window detection & locking (Windows API) | 🔜 Next |
| 3 | Session timer & state machine | ⏳ Planned |
| 4 | JavaFX UI | ⏳ Planned |

---

## Architecture

```
src/main/java/com/focused/
├── Main.java           Entry point only — starts JavaFX
├── core/               Pure logic — no OS, no UI dependencies
│   └── Session.java    Domain model for a focus session
├── platform/           All Windows API code lives here
│   └── WindowManager.java
├── ui/                 All JavaFX code lives here
│   └── MainView.java
└── config/             Settings and persistence
    └── AppConfig.java
```

**Rule:** `core/` never imports from `platform/` or `ui/`. Logic is always testable in isolation.

---

## Requirements

- Windows 10 / 11
- Java 21+
- Maven 3.8+

---

## How to run (dev)

```bash
git clone https://github.com/YOUR_USERNAME/focused.git
cd focused
mvn javafx:run
```

## How to build a distributable JAR

```bash
mvn package
java -jar target/focused.jar
```

---

## License

MIT — see [LICENSE](LICENSE).
