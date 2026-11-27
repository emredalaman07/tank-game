# Tank Game (JavaFX)

2D top-down arcade tank game built in Java with JavaFX (`Canvas` and `AnimationTimer`) on an `800x600` tile-based arena.

## Gameplay

- You start at the bottom of the map with **3 lives**.
- **8 enemy tanks** patrol the arena, changing direction periodically and firing at randomized intervals.
- Destroying an enemy tank awards **+10 points** (tracked alongside session **High Score**) and respawns a new enemy at one of the upper spawn points.
- Tile walls (`16x16`) block both tank movement and bullets, triggering impact explosions, while tank collisions and hits trigger full tank explosions.
- Losing all 3 lives displays your final score on the Game Over screen before automatically resetting the match.

## Controls

| Key | Action |
|---|---|
| `Arrow Keys` | Move tank (`UP`, `DOWN`, `LEFT`, `RIGHT`) |
| `X` | Fire (`600 ms` reload cooldown) |
| `P` | Pause / Resume |
| `R` | Reset current game |
| `ESC` | Exit game |

## Running the Project

Requires **JDK 21** (or 17+). Sprite assets are included under `TankGame/assets/`.

### Option 1: Maven (Recommended)
```bash
mvn javafx:run
```

### Option 2: Direct `javac` / `java` with JavaFX SDK
```bash
javac --module-path /path/to/javafx-sdk/lib --add-modules javafx.controls TankGame/Main.java
java --module-path /path/to/javafx-sdk/lib --add-modules javafx.controls -cp TankGame Main
```
