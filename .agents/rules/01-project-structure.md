---
trigger: always_on
---

# Project Structure

This project is split into **API modules** and **Plugin modules** for better maintainability, shading, and reuse.

---

## API Modules

**Purpose:** Provide public interfaces, constants, and utility classes that can be consumed by plugins or other modules without including the full plugin logic.

- **Module:** `core-api`
- **Contents:**
  - API interfaces
  - Constants
  - Utility classes
- **Build Output:**
  - JAR artifact: `core-api-<version>.jar`
  - Sources JAR and Javadoc JAR (optional)
- **Dependencies:** `compileOnly` PaperMC API
- **Use Case:** Can be published to Maven for other plugins to depend on
- **Example package structure:**
```

org.yuemi.example.api
└─ ExampleApi.java

```

**Key Notes:**
- No server-specific logic
- Designed to be **shaded** into plugin modules when deployed
- Versioned independently from the plugin

---

## Plugin Modules

**Purpose:** Contain the actual plugin implementation for Minecraft servers. Includes `core-api` via shading, so only one JAR is required for deployment.

- **Module:** `core-plugin`
- **Contents:**
- Plugin main class (`onEnable`, `onDisable`)
- Event listeners
- Commands
- Internal plugin logic
- **Build Output:**
- Shaded JAR artifact: `core-plugin-<version>.jar` (includes `core-api`)
- **Dependencies:**
- `compileOnly` PaperMC API (server provides runtime API)
- `implementation(project(":core-api"))` (bundled inside JAR via Shadow plugin)
- **Example package structure:**
```

org.yuemi.example.plugin
└─ CorePlugin.java

```

**Key Notes:**
- Shading ensures `core-api` classes are included
- No sources or Javadoc included in the plugin JAR
- Deployable directly to a PaperMC server
- Plugin automatically uses the API from the shaded JAR

---

**Summary:**  

- **core-api** → reusable API module, versioned separately  
- **core-plugin** → main plugin JAR, shaded with API, server-ready  
- Shading avoids runtime errors and simplifies server deployment  
- Maintains clean separation of API and implementation for maintainability

