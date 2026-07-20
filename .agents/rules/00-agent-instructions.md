---
trigger: always_on
---

# AI Agent Instructions

This repository is a **Minecraft server plugin project** built for **PaperMC** using the **Paper API**.  
AI Agent should follow the rules below when generating code, configs, or suggestions.

---

## 1. Platform & Tooling

- **Minecraft Server Platform**: PaperMC
- **API**: Paper API
- **Java Version**: **Java 21 (LTS)**
- **Gradle Version**: **Gradle 8.13**
- **Build System**: Gradle (Kotlin DSL)

Target runtime:
- Modern Minecraft (1.21+)
- JVM: Java 21

---

## 2. Project Type

- This is a **Minecraft server plugin**, NOT:
  - a standalone application
  - a client-side mod
  - a Fabric/Forge mod
- Uses **PaperMC API**, not Spigot-only APIs unless compatible
- All server interaction must follow Paper API best practices

---

## 3. Code Style & Naming Rules (STRICT)

### Java Classes
- **PascalCase**
- One public class per file
- File name **must match** the class name

Example:
```java
public class ExampleFile {
}
````

---

### Variables & Methods

* **camelCase**
* Descriptive, readable names
* No abbreviations unless well-known

Examples:

```java
private Player targetPlayer;
private int maxPlayerCount;

public void registerCommands() {
}
```

---

### Constants

* **UPPER_SNAKE_CASE**
* `static final`

Example:

```java
private static final int MAX_RETRIES = 3;
```

---

## 4. Packages

* Base package:
  `org.yuemi.<module>`

Examples:

```
org.yuemi.core
org.yuemi.api
org.yuemi.plugin
```

Do **not** use default packages.

---

## 5. Formatting & Linting

* 4 spaces indentation (no tabs)
* UTF-8 encoding
* No trailing whitespace
* Braces on the same line

Example:

```java
if (player != null) {
    player.sendMessage("Hello");
}
```

---

## 6. PaperMC Best Practices

* Prefer **Paper API** over Spigot when available
* Avoid blocking the main server thread
* Use async tasks where appropriate
* Respect lifecycle methods:

  * `onEnable()`
  * `onDisable()`

Example:

```java
@Override
public void onEnable() {
    registerListeners();
}
```

---

## 7. Gradle & Build Rules

* Gradle Kotlin DSL (`build.gradle.kts`)
* Versions and names come from `gradle.properties`
* No hardcoded artifact names
* Java toolchain must be Java 21

---

## 8. What AI Agent SHOULD Do

* Generate clean, idiomatic Java 21 code
* Follow PaperMC API patterns
* Respect naming and formatting rules
* Keep code minimal and maintainable

---

## 9. What AI Agent MUST NOT Do

* Do not suggest Fabric, Forge, or client mod APIs
* Do not use outdated Bukkit-only APIs when Paper alternatives exist
* Do not use Java versions below 21
* Do not generate snake_case variables
* Do not mismatch class names and file names

---

## 10. Mindset

This repository values:

* **Consistency**
* **Correctness**
* **Maintainability**
* **Production-ready code**