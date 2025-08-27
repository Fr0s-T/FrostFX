# JavaFX Scene Manager Framework

Tame JavaFX complexity with professional-grade scene management.  
A lightweight, thread-safe framework for managing scenes, components, and windows in JavaFX applications.

![JavaFX](https://img.shields.io/badge/JavaFX-Architecture%20Ready-blue?logo=java&logoColor=white)
![Thread-Safe](https://img.shields.io/badge/Thread-Safe%20%E2%9C%85-green)
![License](https://img.shields.io/badge/license-MIT-brightgreen.svg)

---

## 🚀 Why This Exists

Building JavaFX applications often means wrestling with:

- "Not on JavaFX application thread" errors ⚡
- Manual stage and scene management headaches 🤕
- Boilerplate code for simple UI operations 📦
- No standard way to handle multi-window applications 🪟

**This framework solves all that.**

---

## ✨ Features

### 🏗️ Professional Architecture
- Multi-stage management with isolated registries
- Thread-safe operations throughout
- Lifecycle hooks for analytics and preloading
- Clean separation of concerns

### 🎯 Progressive API Design
```java
// Simple enough for beginners:
SceneManager.loadScene("/home.fxml", null);

// Powerful enough for experts:
SceneManager.FrameLoader().loadScene(
    "/dashboard.fxml", 
    dashboardController, 
    customStage, 
    WindowMode.FULLSCREEN_UNDECORATED
);
```
### ⚡ Thread Safety Built-In
````java
// Works from ANY thread - no more Platform.runLater()!
CompletableFuture.supplyAsync(() -> {
    SceneManager.CardLoader().loadCard("user", "contentPanel");
    return processData();
});
````
### 🪟 Multi-Window Support
```java
// Manage multiple stages effortlessly
Stage settingsStage = new Stage();
SceneManager.StageManager().registerSecondaryStage("settings", settingsStage);
SceneManager.FrameLoader().loadScene("/settings.fxml", null, settingsStage);
```
## 🚀 Quick Start
### 1. Initialize in Main.java
```java
@Override
public void start(Stage primaryStage) {
    SceneManager.init(primaryStage);
    SceneManager.FrameLoader().loadScene("/welcome.fxml", null);
}
```
### 2. Load Scenes from Anywhere
```java


// Simple scene loading
SceneManager.FrameLoader().loadScene("/dashboard.fxml", dashboardController);

// Advanced with window modes
SceneManager.FrameLoader().loadScene(
    "/game.fxml", 
    gameController, 
    WindowMode.FULLSCREEN_UNDECORATED
);
```
### 3. Dynamic Component Loading
```java
// Load cards into panels
UserController ctrl = SceneManager.CardLoader().loadCard(
    "userCard", 
    "contentPanel", 
    UserController.class
);

// Bulk data loading
SceneManager.CardLoader().loadCardsInto(
    "userList", 
    users, 
    "/cards/user.fxml", 
    (controller, user) -> ((UserController)controller).setUser(user)
);
```
## 📦 Installation
Option 1: Source Integration (Recommended)
```manifest
git clone https://github.com/Fr0s-T/JavaFXSceneManager
cp -r src/main/java/org/frost /your-project/src/
```

Option 2: Maven (Coming Soon!)
```manifest
<dependency>
    <groupId>org.frost</groupId>
    <artifactId>javafx-scene-manager</artifactId>
    <version>2.0</version>
</dependency>
```
## 🎯 Advanced Usage
### Multi-Stage Applications
```java


// Create admin panel stage
Stage adminStage = new Stage();
SceneManager.StageManager().registerSecondaryStage("admin", adminStage);

// Load with specific window mode
SceneManager.FrameLoader().loadScene(
    "/admin.fxml", 
    adminController, 
    adminStage, 
    WindowMode.UNDECORATED
);
```
### Lifecycle Hooks
```java


SceneManager.FrameLoader().addSceneLoaderListener(
    new SceneLoader.FrameLoaderListener() {
        @Override
        public void onBeforeSceneLoad(String fxmlPath) {
            System.out.println("Loading: " + fxmlPath);
        }
        
        @Override
        public void onAfterSceneLoad(String fxmlPath, Object controller) {
            System.out.println("Loaded: " + fxmlPath);
        }
    }
);
```
## 🏗️ Architecture Overview
```text
SceneManager (Coordinator)
├── FrameLoader (Scene navigation) 
├── CardLoader (Component management)
├── PopupLoader (Dialog windows)
├── AlertUtilities (User feedback)
└── StageManager (Multi-window support)
```
Each component is independently usable but designed to work together seamlessly.

## 🚀 Performance Benefits

    Zero boilerplate for common operations

    Automatic thread safety - no more manual Platform.runLater()

    Memory efficient - per-stage isolation and cleanup

    Fast development - intuitive API reduces coding time

## 📚 Documentation

    JavaDoc: Comprehensive documentation in source

    Demo Module: Working example application(comming soon)

    Architecture Guide: Deep dive into design patterns (coming soon)

## 🤝 Contributing

This is a young framework with big ambitions! Ideas and contributions welcome:

    📖 Improve documentation

    🐛 Report issues

    💡 Suggest features

    🔧 Submit pull requests


## ⭐ Why Developers Love This

    "Finally, a JavaFX framework that doesn't make me fight the platform!"
    "The thread safety alone saved me countless hours of debugging!"
    "I can actually focus on my app logic instead of UI plumbing!"

## 💬 Get Help

    📝 Open an Issue

    💡 Request a Feature

    🎯 Check the Demo

## ⭐ Ready to stop fighting JavaFX and start building amazing applications?

## 👉 Get Started Today — and star the repo if this saves your sanity! ⭐

P.S. Your web dev friends might be jealous of your thread-safe UI superpowers! 😉

P.P.S. Yes, it actually handles multi-threaded UI updates properly — something web devs can only dream about! 🚀

## 📜 License

MIT License © 2025 Fr0s-T – See LICENSE

for details.
Attribution required – Please include copyright.