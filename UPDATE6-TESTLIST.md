# Update 6 - test list (BetterMarkerMap 6.0.0)

Migrated 2026-08-18, branch `update-6`. Server target: `>=0.6.0-pre.0 <0.7.0`.

**1 compile error, 0 deprecation/removal warnings.** Small diff, but the one error is a permission
decision, and this plugin's whole job is permissions - so it is worth reading before deploying.

---

## 1. `canGeneratePermission()` removed - and here the fix is `requireNoPermission()`

`MarkerSelectorCommand` (`/marker`) overrode it to return `false`. **Deleting the override would
have broken the command.** A command that declares nothing gets a node of its own on 0.6:

> // A command that declares nothing gets a permission of its own, so that leaving the
> // question open does not leave the command open.
> `if (permission == null && !openToEveryone) { permission = PermissionQuery.of(generatePermission()); }`

So `/marker` would have ended up gated behind `gillodaby.better_markermap.command.marker` - a node
nobody holds and this plugin never documents - **on top of** the check it already performs.

`requireNoPermission()` is the replacement the 0.6 javadoc scopes to exactly this shape:

> Declares that anyone may run this command. [...] Say it for a command that hands out the first
> permission on a server, **or one that guards itself by some other means, and say why.**

`/marker` guards itself in `execute()` with `markerFilter.canUseMarkerUi(...)`, i.e.
`bettermarkermap.marker.ui` - the node this plugin documents in `permissions.json` and that server
owners actually grant.

**`/markermap` deliberately did NOT get the same treatment.** It never overrode
`canGeneratePermission()` and has no internal check, so it keeps its generated node exactly as on
0.5. Only the command that was already self-guarding was declared open.

> Worth knowing: this is the opposite call from the eight Better-* minigames migrated the same day.
> None of those self-guarded at the framework layer, so for them `requireNoPermission()` would have
> *opened* commands that 0.5 already gated. The deciding question is never the API - it is whether
> the command performs its own check.

## 2. The generated permission node changed - because this plugin's name has a space

This affects **only** plugins whose manifest `Name` contains a space, and this one does
(`"Name": "Better MarkerMap"`):

| | `PluginBase` base permission |
|---|---|
| 0.5 | `(group + "." + name).toLowerCase()` -> `gillodaby.better markermap` |
| 0.6 | `.toLowerCase(Locale.ROOT).replace(' ', '_')` -> `gillodaby.better_markermap` |

0.6 ships a test for it (`basePermissionReplacesSpacesWithUnderscores`, expecting
`robotmonkey.my_mod`).

**No code change was needed**, because every node this plugin enforces is a hardcoded
`bettermarkermap.*` literal in `BetterMarkerMapPermissions` - none of them derives from
`getBasePermission()`, which the plugin never calls. The change only affects the node the engine
auto-generates for `/markermap`.

Note the old value was **not a valid permission node**: the pattern is
`^-?\w[\w-]*(\.[\w*][\w*-]*)*$` (identical in 0.5 and 0.6) and `\w` excludes spaces. So
`gillodaby.better markermap.command.markermap` could never match a grant on 0.5 either. On 0.6 the
node becomes valid for the first time - which is why `/markermap` access is worth re-testing rather
than assumed unchanged.

## 3. Manifest

`Version` 5.0.0 -> 6.0.0 (also in `plugin.json`, which still said 1.0.0 - stale and now aligned).
`ServerVersion` `">=0.5.0 <0.6.0"` -> `">=0.6.0 <0.7.0"`; the old range **excluded** 0.6, so
the plugin would not have loaded at all. (It was first set to `">=0.6.0-pre.0 <0.7.0"` while the
server was still on a `0.6.0-pre.N` build; `0.6.0` is a final release, so the plain range matches.)

---

## Manual checks on a live server

### 1. `/marker` - the command that changed
- [ ] A player **with** `bettermarkermap.marker.ui` runs `/marker` -> the icon selector opens.
      *(If it is refused, `requireNoPermission()` did not take and the engine node is gating it.)*
- [ ] A player **without** it -> refused with the plugin's own message naming
      `bettermarkermap.marker.ui`, **not** an engine "no permission" reply. The message source is
      the tell: an engine refusal means a node was generated after all.
- [ ] Picking an icon applies it and persists across a relog.

### 2. `/markermap` - the command whose generated node changed name
- [ ] Run it as an operator -> it lists the permission nodes.
- [ ] As a plain player: note whether it is allowed or refused, and compare with the behaviour you
      had on 0.5. If it used to work and no longer does, grant
      `gillodaby.better_markermap.command.markermap` (**underscore**, not a space).

### 3. The plugin's own permission surface (unchanged code, but the point of the mod)
Every node below is a hardcoded literal, so none of them moved. Re-test the ones you actually use:
- [ ] `bettermarkermap.create.personal` / `.create.shared` gate marker creation.
- [ ] `bettermarkermap.remove.own` removes only your own; `bettermarkermap.admin` removes anyone's.
- [ ] `bettermarkermap.teleport.marker` gates teleporting to a marker.
- [ ] `bettermarkermap.limit.personal.<tier>` / `.limit.shared.<tier>` enforce their tier, and
      `.limit.unlimited` overrides both.
- [ ] `bettermarkermap.marker.use.<image_key>` gates a single image; `.marker.use.any` gates all.
- [ ] `bettermarkermap.bypass.all` bypasses every check above.

### 4. Load
- [ ] Loads with no `ServerVersion` complaint (the old exact range excluded 0.6).
- [ ] Both `.ui` pages open without a disconnect.
- [ ] The marker PNGs still resolve - `processResources` copies them into
      `Common/UI/Custom/Textures/BetterMarkerMap/MapMarkers`, and runtime texture paths are
      sandboxed to `Common/UI/Custom/`, so a broken icon means the copy step did not run.

---

## Known-unchanged

- The 2 `.ui` files and all assets: untouched; no asset uses a key removed in 0.6, no
  `FlexWeight` inside `Anchor:(...)`, no underscore element ids.
- `MarkerPermissionPacketFilter` and the rest of the enforcement path compiled clean on 0.6 with
  `-Xlint:deprecation -Xlint:removal` already enabled - no deprecated API in use.
- The repo now carries a Gradle wrapper (`gradlew`, `gradlew.bat`, `gradle/wrapper/`, Gradle
  9.2.0), so `./gradlew build` is the supported way to build it and no system Gradle is required.
- The Hytale server API is no longer a jar you place next to the checkout: `build.gradle` resolves
  `com.hypixel.hytale:Server:0.6.2` as `compileOnly` from `https://maven.hytale.com/release`.
