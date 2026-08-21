# Madgique's XRay — Fork 1.12.2

Fork du mod [Advanced XRay](https://github.com/AdvancedXRay/XRay-Mod) de MiKeY (MichaelHillcox).
Objectif : corriger les bugs de la version 1.12.2 et l'améliorer.

## Identité du mod (renommage fork)

- **Nom affiché** : `Madgique's XRay` (`Reference.MOD_NAME`)
- **MOD_ID** : `madgique-xray` (`Reference.MOD_ID`) — impacte : dossier assets `assets/madgique-xray/`,
  dossier config utilisateur `config/madgique-xray/`, fichier config Forge `config/madgique-xray.cfg`
  (`@Config name`), catégorie keybinds
- **Jar** : `archivesBaseName = "madgique-xray"` → `madgique-xray-<version>+mc1.12.2.jar`
- **Package Java** : `com.madgique.xray` (convention `com.madgique.<nom_du_mod>`, voir CLAUDE.md général
  des mods) — déplacé depuis `com.xray` lors du renommage
- **UPDATE_JSON** : chaîne vide (update check désactivé — le versions.json de l'auteur original ne
  correspond pas au fork)
- **mcmod.info** : url GitHub Madgique, authorList `["Madgique"]`, crédits aux auteurs originaux
- ⚠️ Le changement de MOD_ID réinitialise la config/keybinds des joueurs venant de l'ancien mod
  (dossier `config/xray/` orphelin)

## Contexte technique

- **Minecraft** : 1.12.2
- **Loader** : Forge (`net.minecraftforge:forge:${minecraft_version}-${forge_version}`)
- **Build** : Gradle 7.6.4 + ForgeGradle 5.1.x (`plugins { id 'net.minecraftforge.gradle' version '[5.1,5.2)' }`)
- **Mappings** : snapshot `20171003-1.12` (`mappings_channel`/`mappings_version` dans `gradle.properties`)
- **Java** : toolchain Java 8 (compile + runClient) ; le daemon Gradle tourne lui sous JDK 17
- **Side** : client uniquement (`clientSideOnly = true`, `@SideOnly(Side.CLIENT)`)

## Environnement de dev (résolu)

- **Migration ForgeGradle 3 → ForgeGradle 5** (FG3 incompatible avec les extensions VSCode modernes)
- **Wrapper Gradle 7.6.4** régénéré proprement (`gradlew wrapper --gradle-version 7.6.4`)
- **`gradle.properties`** : `mod_version=1.0.0` (numérotation fork, indépendante de l'original), `minecraft_version=1.12.2`, `forge_version=14.23.5.2860`, `mappings_channel=snapshot`, `mappings_version=20171003-1.12`
- **`settings.gradle` créé** : pluginManagement avec repo Maven Forge
- **`jcenter()` retiré** (service fermé) → `mavenCentral()` suffit
- **CurseGradle + publish saps.dev retirés** (credentials de l'auteur original, inutiles pour le fork) → seul `maven-publish` local reste
- **Group/vendor passés à `com.madgique`/`Madgique`** (fork)
- **Toolchain Java 8** : résolue via `org.gradle.java.installations.paths` dans `~/.gradle/gradle.properties` (chemin machine, non commité)
- **JDK** : build/run mod sous JDK 8 (`$JDK8_PATH`) ; daemon Gradle sous JDK 17 (`$JDK17_PATH`) ; language server VSCode sur JDK 21 (`$JDK21_PATH`, voir `.code-workspace`)
- **Chemins machine** : référencés via variables définies dans `.env.local` (gitigné, template dans `.env`)
- **`processResources` modernisé** : `filesMatching('mcmod.info')` au lieu des doubles `from()` (erreur "duplicate entry" Gradle 7)
- Build vérifié OK (`gradlew build` → compile + reobfJar)
- **runClient validé** : 13 mods chargés (XRay + JEI + HWYLA + Astral Sorcery + Thermal Series), menu principal atteint, arrêt propre

## Bug ForgeGradle #748 : Side.BUKKIT / jar recomp (résolu)

Le jar **binaire** mappé par FG5 pour 1.12.2 contient un enum `Side` avec une constante parasite `BUKKIT`
(injectée par mergetool 1.0.13 lors du merge client/server du pipeline MCP). `NetworkRegistry.newChannel`
itère sur `Side.values()` mais sa map ne connaît que CLIENT/SERVER → `NullPointerException` ligne 207 au
modConstruction de FML. Le jar **recomp** (recompilé depuis les sources décompilées, où les patches Forge
remplacent `Side`) n'a pas ce problème.

FG5 ne génère le recomp que si l'artefact classifier `sources` du module mappé est demandé (c'est ce que
fait un IDE à l'import). Contournement dans `build.gradle` :

- config `forgeSources` + dépendance `net.minecraftforge:forge:<version>_mapped_<mappings>_at_<hash>:sources`
  (la version exacte est lue dynamiquement depuis la dépendance `minecraft` après évaluation — FG y remplace
  la version par la version mappée suffixée du hash des ATs) ;
- task `generateForgeSources` qui résout cette config ;
- `runClient`/`runServer` dépendent de `generateForgeSources`.

Au premier lancement, la chaîne complète tourne : pipeline MCP (merge → AT → rename → decompile) → sources
renommées MCP → compilation ~9000 fichiers → `-recomp.jar`. Ensuite tout est en cache (~40 s).

## Access Transformers (dev)

`fg.deobf()` n'applique PAS les FMLAT embarqués des dépendances (cofh_at.cfg, jei_at.cfg) → les mods de test
crashent en `IllegalAccessError` (ex: `ItemSpade.EFFECTIVE_ON`, `TextureMap.initMissingImage()`).

Solution : `src/main/resources/META-INF/accesstransformer.cfg` centralise les ATs de CoFH Core + JEI.

**⚠️ Format obligatoire : noms SRG** (`field_150916_c`, `func_110569_e()V`). Le step AccessTransformer du
pipeline MCP opère sur le jar avant renommage MCP ; les noms MCP (`EFFECTIVE_ON`) sont ignorés silencieusement.
Les classes gardent leurs noms (`net.minecraft.item.ItemSpade`). Sources des entrées : `META-INF/cofh_at.cfg`
et `META-INF/jei_at.cfg` des jars dans `libs/`.

Toute modification de ce fichier change son hash → FG régénère pipeline + sources + recomp automatiquement
(la dépendance forgeSources suit le nouveau hash). Purger `build/fg_cache` si besoin.

## Daemon Gradle / JDK

Si `JAVA_HOME` machine pointe sur JDK 21+, Gradle 7.6.4 crash (`Unsupported class file major version 65`).
Forcer le daemon : `gradlew <task> "-Dorg.gradle.java.home=$JDK17_PATH"`.

## Structure

```
src/main/java/com/madgique/xray/
├── XRay.java              # Classe principale @Mod (preInit/postInit/events)
├── Configuration.java     # Config Forge (@Config)
├── reference/
│   ├── Reference.java     # Constantes (MOD_ID="madgique-xray", version, chemins GUI)
│   └── block/             # BlockData, SimpleBlockData, BlockInfo, BlockItem
├── store/
│   ├── BlockStore.java    # Logique de stockage des blocs trackés
│   ├── GameBlockStore.java# Cache des blocs du jeu
│   └── JsonStore.java     # Persistance JSON (config utilisateur)
├── xray/
│   ├── Controller.java    # État global (activation, blocs actifs)
│   ├── Events.java        # Events monde/render
│   ├── Render.java        # Rendu des outlines
│   └── RenderEnqueue.java # File de rendu (executor)
├── keybinding/            # KeyBindings, InputEvent
├── gui/                   # GuiOverlay, GuiSelectionScreen, GuiHelp, etc.
│   ├── manage/            # GuiAddBlock, GuiEdit, listes de blocs
│   └── utils/             # GuiBase, GuiSlider
└── utils/                 # WorldRegion, OutlineColor, Utils
```

Ressources : `src/main/resources/mcmod.info` (version injectée par `processResources`).

## Build & run

```bash
./gradlew build          # compile + reobfJar
./gradlew runClient      # lance un client de test
```

Le jar final passe par `reobfJar` (obfuscation vers les mappings MCP).

## Mods de test (`libs/`, non commités)

Environnement de test pour vérifier le XRay sur les minerais de mods (posés en créatif, monde superflat) :

- JEI `4.16.5.1029`
- HWYLA `1.8.26-B41`
- Astral Sorcery `1.10.27` (+ dépendance Baubles `1.5.2`)
- Thermal Series : RedstoneFlux `2.1.1.1` + CoFHCore `4.6.6.1` + CoFHWorld `1.4.0.1` + ThermalFoundation `2.6.7.1`

Chargés via `implementation fg.deobf("blank:<nom>:<version>")` dans `build.gradle` (repo flatDir `libs/`,
noms de fichiers normalisés). `fg.deobf` remappe SRG→MCP à la résolution (cache
`~/.gradle/caches/forge_gradle/deobf_dependencies/`) mais n'applique pas leurs FMLAT — voir section
Access Transformers.
Les minerais apparaissent dans la GUI du mod via `GameBlockStore.populate()` (postInit).

## Points connus / à investiguer

- Dernier commit branche : `d832282 fix: issues with layout and block store`
- Logo mcmod.info : toujours l'ancien logo (`assets/madgique-xray/logo-small.jpg`) — nouveau logo à créer
- README.md réécrit façon Madgique (template fix-cobblemon-pokemon-experience), mention fork + crédits

## Conventions

- Répondre en français (voir CLAUDE.md global utilisateur)
- Java 8 strict : pas de syntaxe post-Java 8 (var, records, switch expressions…)
- Mappings MCP snapshot 20171003 : noms de méthodes/champs style SRG-mapped (ex: `Minecraft.getMinecraft()`, `world.getChunkFromChunkCoords(...)`)
