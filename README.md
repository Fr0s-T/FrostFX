# JavaFX Scene Manager

 **Tame the complexity of JavaFX navigation.**  
A lightweight utility for managing scenes, popups, and alerts in JavaFX apps with clean, simple code.

![JavaFX](https://img.shields.io/badge/JavaFX-Scene%20Manager-blue?logo=java&logoColor=white)  
![License](https://img.shields.io/badge/license-MIT-green.svg)  
![Maven](https://img.shields.io/badge/Maven-ready-orange)

---

## ✨ What is this?

Building a JavaFX app usually means juggling messy `FXMLLoader` code, custom dialogs, and UI threading headaches.

This library gives you simple, reliable tools to:

- 🚀 Load and swap **scenes** effortlessly.
- 🪟 Create **popup windows** with custom titles, sizes, and icons.
- ⚡ Show **alerts & confirmation dialogs** safely (no thread issues).
- 📦 Build **dynamic UI lists** (e.g. product cards) from data.

It handles the tricky stuff (threading, lifecycle, FXML) so you can focus on app logic.

---

## 🚀 Quick Start

### 1. Setup in your `Main` class

```java
@Override
public void start(Stage primaryStage) {
    // Give the manager a reference to your main window
    SceneLoader.setPrimaryStage(primaryStage);

    // (Optional) Set a default icon for all your popups and alerts
    Image appIcon = new Image("/icon.png");
    AlertUtilitie.setAppIcon(appIcon);

    // Start your app!
    SceneLoader.loadScene("/welcome.fxml", null);
}

2. Load a new scene from anywhere

SceneLoader.loadScene("/dashboard.fxml", null);

3. Show a confirmation dialog

boolean shouldDelete = AlertUtilitie.showConfirmation(
    "Delete Item",
    "Are you sure you want to delete this item?"
);

if (shouldDelete) {
    // Delete the item!
}
```
## 📦 Installation (Source)

Since this library is new, the easiest way to use it is to include the source directly in your project.

    Download the Source

    git clone https://github.com/Fr0s-T/JavaFXSceneManager

    Copy the Source
    Copy the org.frost package directory (from src/main/java) into your own project's source folder.

    That's it! You can now use SceneLoader and AlertUtilitie anywhere in your app.

(Maven Central deployment is a goal for the future!)


## 📚 Documentation

    Documentation is available in the source code via JavaDoc.

    A more in-depth guide is coming soon.

    See the /demo module for a working example.

## License
This project is licensed under the MIT License © 2025 Fr0s-T.  
If you use this library, attribution is required by including the copyright notice in your distributions.  
See the [LICENSE](LICENSE) file for details.

## 💡 Found it useful? Give it a star! ⭐

## 💬 Questions or suggestions? Open an Issue!