# Focused

> Lock yourself to one window. Get things done.

Focused is a Windows productivity app created for my own use. It locks your desktop to a single chosen window for a set amount of time — like iPhone's guided access, but for your PC.

# UseCase:
> Lets say you're working on task, which can be whatever (Ms office, coding, following a tutorial, reading a pdf).
>  You might struggle with switching tab here and there your attention get distributed accross multiple stuff. Focused help to just lock yourself in one windows for a set amount of time.

---

## Architecture

```
src/main/java/com/focused/
├── Main.java           
├── core/               
│   └── Session.java    //model for a focus session
├── controll/
|    └── SessionController.java //controller
├── platform/       
│   └── WindowManager.java //model for the communication with Win32.dll API
├── ui/                 
│   └── MainView.java // view using JavaFX
└── config/             
    └── AppConfig.java // Settings
```
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
