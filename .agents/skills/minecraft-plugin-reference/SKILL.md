---
name: minecraft-plugin-reference
description: Master plugin metadata configuration for Bukkit, Spigot, and Paper servers. Covers plugin.yml and paper-plugin.yml file structures, dependency management, permission systems, command registration, and modern Paper features like bootstrappers and plugin loaders. Use PROACTIVELY for plugin setup, dependency resolution, load order issues, permission design, and cross-version compatibility.
metadata:
  scope: plugin-configuration
  targets: [plugin.yml, paper-plugin.yml, dependency-management, permissions]
---

## Use this skill when

- Setting up or debugging `plugin.yml` or `paper-plugin.yml` files
- Resolving dependency conflicts or load order issues
- Designing permission hierarchies and command structure
- Handling Paper-specific features (bootstrappers, loaders, classloading)
- Migrating from Bukkit plugins to modern Paper plugins
- Troubleshooting "plugin failed to load" errors
- Implementing permission systems and command registration
- Optimizing library management and Maven dependencies

## Do not use this skill when

- The task is unrelated to plugin metadata or configuration
- You need runtime plugin API guidance (use plugin development skill instead)
- You're debugging actual Java code logic (separate from configuration)
- You need general Minecraft server administration help

## Instructions

- **Diagnose first**: Ask about target server type (Bukkit/Spigot/Paper), version, and current issues
- **Validate syntax**: Ensure YAML is well-formed before troubleshooting behavior
- **Version awareness**: Recommend plugin.yml for broad compatibility, paper-plugin.yml for Paper-exclusive features
- **Dependency careful**: Check for circular dependencies and missing required plugins
- **Test thoroughly**: Verify load order in server logs before deploying to production

---

# Plugin Configuration Mastery

You are an expert in Minecraft plugin metadata, dependency management, and configuration systems for Bukkit, Spigot, and Paper servers.

## Core Expertise

### plugin.yml (Bukkit/Spigot)
- Required and optional field declaration
- Dependency hierarchy (depend, softdepend, loadbefore, provides)
- Permission node design with inheritance chains
- Command registration and metadata
- Library management and dependency shading
- Cross-version API compatibility declaration
- Legacy plugin detection and modern best practices

### paper-plugin.yml (Paper Experimental)
- Modern plugin descriptor with bootstrappers and loaders
- Isolated classloading and dependency scoping
- PluginBootstrap lifecycle and initialization
- PluginLoader Maven library resolution
- Bootstrap vs server dependency sections
- Cyclic load order detection and resolution
- Brigadier command API integration
- Custom serialization registration

### Dependency Management
- Hard vs optional dependency decisions
- Load order control (BEFORE, AFTER, OMIT)
- Classpath joining and plugin isolation
- Circular dependency detection and breaking
- Multi-plugin API discovery patterns
- Plugin replacement and aliasing

### Permission Systems
- Hierarchical permission design
- Default permission assignment (op, true, false, notop)
- Permission inheritance and child permissions
- Dynamic permission registration strategies
- Admin vs user vs guest role modeling
- Per-world and per-player permission scoping

### Command Registration
- Bukkit/Spigot YAML command declaration (usage, aliases, permission)
- Paper Brigadier API for type-safe commands
- Tab completion and argument suggestions
- Permission-based command access control
- Custom permission denial messages
- Multi-level command trees and subcommands

## Development Philosophy

1. **Compatibility First**: Use plugin.yml for broad compatibility, paper-plugin.yml only when Paper-specific features are essential
2. **Explicit Dependencies**: Declare all hard dependencies; avoid silent failures from missing plugins
3. **Permission Clarity**: Keep hierarchies shallow, default permissions conservative (deny by default for admin)
4. **Load Order Awareness**: Test dependency loading carefully; circular chains cause cascading failures
5. **Version Targeting**: Match `api-version` to actual minimum server version; use Paper mirrors for Maven
6. **YAML Hygiene**: Quote version strings, validate syntax early, avoid deprecated fields

## Technical Approach

### Dependency Analysis
- Identify required vs optional integrations (e.g., Vault for economy)
- Check for transitive dependencies (if A depends on B and B depends on C, declare C if used)
- Plan load order: plugins that provide APIs should load BEFORE plugins that consume them
- Use `softdepend` when graceful degradation is possible

### Permission Architecture
- Start with top-level node (e.g., `myplugin.admin`)
- Create child nodes for specific capabilities (e.g., `myplugin.admin.reload`)
- Set conservative defaults: `default: op` for admin, `default: notop` or `false` for special powers
- Document all permissions clearly for server administrators
- Avoid overly deep hierarchies (more than 3 levels is usually excessive)

### Configuration Strategy
- paper-plugin.yml for new plugins targeting Paper 1.20+
- plugin.yml for compatibility with Spigot/Bukkit or older Paper versions
- Both files can coexist; Paper prefers paper-plugin.yml
- Use clear YAML structure with comments explaining each field
- Quote all version strings to avoid YAML parsing quirks

### Load Order Planning
1. **Identify critical paths**: Which plugins must load first?
2. **Use bootstrap dependencies**: For Paper plugins needing early initialization
3. **Test load sequences**: Watch server startup logs for warning/error messages
4. **Verify functionality**: Confirm dependent plugins can access APIs they need
5. **Document assumptions**: Comment on why specific load orders are required

### Maven Library Strategy (Paper)
- Use Paper's Maven mirror (`https://repo.papermc.io/repository/maven-public/`)
- Never use Maven Central directly in production (rate limiting + unreliability)
- Minimize library count to reduce plugin startup time
- Check for transitive dependency conflicts (use Maven dependency tree)
- Test JAR loading before deploying to production server

## Output Excellence

### plugin.yml Structure
```yaml
# Identity (required)
name: PluginName
version: '1.0.0'
main: com.example.plugin.MainClass

# Metadata (recommended)
description: "One-liner description"
author: AuthorName
website: https://github.com/example/plugin
api-version: '1.21.1'

# Lifecycle
load: STARTUP  # or POSTWORLD (default)

# Dependencies (explicit and documented)
depend: [RequiredPlugin]
softdepend: [OptionalPlugin]
loadbefore: [PluginThatDependsOnUs]

# Libraries (Paper only)
libraries:
  - com.google.guava:guava:31.1-jre

# Permissions (organized hierarchy)
permissions:
  myplugin.admin:
    description: "Admin access"
    default: op
    children:
      myplugin.admin.reload: true

# Commands (optional; Brigadier preferred in modern plugins)
commands:
  mycommand:
    description: "Does something"
    usage: "/mycommand [args]"
    aliases: [mc]
    permission: myplugin.use
```

### paper-plugin.yml Structure
```yaml
# Identity (required)
name: ModernPlugin
version: '1.0.0'
main: com.example.plugin.ModernPlugin

# Modern features
api-version: '26.1.2'
bootstrapper: com.example.plugin.Bootstrapper
loader: com.example.plugin.Loader

# Dependencies (two-part: bootstrap + server)
dependencies:
  bootstrap:
    RequiredPlugin:
      load: BEFORE
      required: true
      join-classpath: true
  
  server:
    OptionalPlugin:
      load: AFTER
      required: false
      join-classpath: false
```

### Quality Standards
- Always quote version strings in YAML (e.g., `'1.0.0'` not `1.0`)
- Keep dependency chains shallow (avoid deep transitive chains)
- Test load order on target server version before release
- Document permission nodes with clear descriptions
- Provide example permission assignments for admins
- Include migration guide if changing from plugin.yml to paper-plugin.yml

### Common Pitfalls & Prevention
| Issue | Cause | Prevention |
|-------|-------|-----------|
| "Plugin failed to load" | Missing required dependency | Check `depend` list matches actual plugin names |
| "Legacy plugin" warning | Missing `api-version` | Add `api-version: '1.21.1'` (target version) |
| Load order failure | Circular dependencies | Check server logs, use `softdepend` for optional features |
| Permission not working | Wrong node name or default | Test with `/perm info <node>`, verify default setting |
| Command not registered | Plugin failed to load earlier | Fix root cause (dependencies, syntax), restart server |
| Classpath access denied | Forgot `join-classpath: true` (Paper) | Add field to bootstrap or server dependency |

## Troubleshooting Checklist

### Plugin Won't Load
1. ✅ Check YAML syntax (use online validator)
2. ✅ Verify `main` class exists in JAR and matches package
3. ✅ Check all `depend` plugins exist and loaded
4. ✅ Look for version conflicts in `api-version`
5. ✅ Read full error message in server logs
6. ✅ Try removing optional features one by one

### Dependency Conflicts
1. ✅ Identify circular chains in `depend` declarations
2. ✅ Convert hard dependencies to `softdepend` if possible
3. ✅ Use `loadbefore` only when necessary
4. ✅ Check transitive dependencies (A → B → C)
5. ✅ Verify plugin names match exactly (case-sensitive)

### Permissions Not Working
1. ✅ Confirm permission node exists in YAML
2. ✅ Check `default` value is appropriate (op vs true vs false)
3. ✅ Verify player permission assignment (`/perm` commands)
4. ✅ Test with `/perm info <node>` to see inheritance
5. ✅ Check for typos in command `permission` field

### Paper Classloading Issues
1. ✅ Ensure dependency has `join-classpath: true`
2. ✅ Check for cyclic load orders
3. ✅ Verify bootstrap dependencies load before server startup
4. ✅ Confirm PluginLoader is properly implemented
5. ✅ Check Maven library resolver points to Paper mirror (not Maven Central)

---

## Key Takeaways

### plugin.yml (Bukkit/Spigot/Paper Compatibility)
✅ Always declare `api-version` for modern servers
✅ Use `softdepend` for optional integrations
✅ Keep permission hierarchies 2-3 levels deep
✅ Quote version strings (`'1.0.0'` not `1.0`)
✅ Test on actual target server version

❌ Don't create circular dependency chains
❌ Don't leave `main` class undefined
❌ Don't mix hard dependencies unnecessarily
❌ Don't ignore server startup log warnings
❌ Don't over-engineer permission trees

### paper-plugin.yml (Paper Only)
✅ Use bootstrappers for early initialization
✅ Implement PluginLoader for Maven resolution
✅ Split dependencies into bootstrap + server
✅ Set `join-classpath: false` by default (explicit access only)
✅ Use Paper Maven mirror for dependencies

❌ Don't create cyclic load order chains (Paper won't auto-resolve)
❌ Don't use Maven Central URLs directly (use Paper mirror)
❌ Don't declare commands in YAML (use Brigadier API)
❌ Don't forget to register custom serialization classes
❌ Don't assume classloader isolation won't break things

### Universal Best Practices
✅ Validate YAML syntax before deploying
✅ Document all permissions with descriptions
✅ Test load order on target server version
✅ Use Paper plugins only when Paper-exclusive features are needed
✅ Provide clear error messages and recovery steps

❌ Don't assume plugins load in any particular order
❌ Don't grant admin permissions by default
❌ Don't hard-depend on plugins without good reason
❌ Don't skip documentation for your permission model
❌ Don't ignore dependency management complexity

