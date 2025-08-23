# JavaFX Scene Manager  

> 🖼️ Tame the complexity of JavaFX navigation.  
> A lightweight utility for managing **scenes, popups, and alerts** in JavaFX apps with clean, simple code.  

![JavaFX](https://img.shields.io/badge/JavaFX-Scene%20Manager-blue?logo=java&logoColor=white)  
![License](https://img.shields.io/badge/license-MIT-green.svg)  
![Maven](https://img.shields.io/badge/Maven-ready-orange)  

---

## ✨ What is this?  

Building a JavaFX app usually means juggling messy `FXMLLoader` code, custom dialogs, and UI threading headaches.  

This library gives you **simple, reliable tools** to:  

- 🚀 Load and swap **scenes** effortlessly.  
- 🪟 Create **popup windows** with custom titles, sizes, and icons.  
- ⚡ Show **alerts & confirmation dialogs** safely (no thread issues).  
- 📦 Build **dynamic UI lists** (e.g. product cards) from data.  

It handles the tricky stuff (threading, lifecycle, FXML) so **you focus on app logic**.  

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

📦 Installation

Add this project to your app (Maven/Gradle coordinates coming soon):

<dependency>
    <groupId>org.frost</groupId>
    <artifactId>sceneloader</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>

Or clone the repo and include it in your project directly.
📚 Documentation

    Full JavaDoc available in /docs

    See the /demo module for a working example

📝 License

This project is licensed under the MIT License.
You are free to use, copy, modify, and distribute it for any purpose.

See LICENSE

for details.

