# Focused

> Lock yourself to one window. Get things done.

Focused is a Windows productivity app that locks your desktop to a single chosen window for a set amount of time — like iPhone's guided access, but for your PC.

# UseCase:
> Lets say you're working on task, which can be whatever (Ms office, coding, following a tutorial, reading a pdf).
>  You might struggle with switching tab here and there your attention get distributed accross multiple stuff. Focused help to just lock yourself in one windows for a set amount of time.

---

## Architecture

```
src/main/java/com/focused/
├── Main.java           Entry point only — starts JavaFX
├── core/               Pure logic — no OS, no UI dependencies
│   └── Session.java    Domain model for a focus session
├── platform/           All Windows API 
│   └── WindowManager.java
├── ui/                 All JavaFX code 
│   └── MainView.java
└── config/             Settings and persistence
    └── AppConfig.java
```

**Rule from John Ousterhout:** `core/` imports from `platform/` or `ui/`. Each componnent is testable in isolation.

---

## Requirements

- Windows 10 / 11
- Java 21+
- Maven 3.8+

---

## How to run (dev)

```bash
git clone https://github.com/romaric111/focused.git
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
