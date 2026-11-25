import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.animation.AnimationTimer;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

public class Main extends Application {

    private Image[] enemyTankImages = new Image[2];
    private List<double[]> enemyTanks = new ArrayList<>();
    private List<Integer> enemyDirs = new ArrayList<>();
    private List<Long> enemyTimers = new ArrayList<>();
    private int playerLives = 3;
    private int playerScore = 0;
    private double playerX = 350;
    private double playerY = 450;
    private int spriteIndex = 0;
    private Image[] playerTankImages = new Image[2];
    private long lastMove = 0;
    private long lastFire = 0;
    private Set<KeyCode> pressedKeys = new HashSet<>();
    private int direction = 0;
    private Image wallImage;
    private Image bulletImage;
    private Image explosionImage;
    private Image smallExplosionImage;
    private List<double[]> wallsList = new ArrayList<>();
    private List<Bullet> bullets = new ArrayList<>();
    private List<Bullet> enemyBullets = new ArrayList<>();
    private List<Long> enemyFireTimers = new ArrayList<>();
    private List<Long> enemyFireIntervals = new ArrayList<>();
    private List<double[]> explosions = new ArrayList<>();
    List<Long> explosionTimes = new ArrayList<>();
    private static final int TILE = 16;
    private boolean paused = false;
    private boolean gameOver = false;
    private long gameOverTime = 0;

    // This section handles the logic for detecting and managing collisions between tanks on the game field
    private boolean isBlocked(double x, double y) {
        double tankWidth = playerTankImages[0].getWidth();
        double tankHeight = playerTankImages[0].getHeight();
        for (int i = 0; i < wallsList.size(); i++) {
            double[] wall = wallsList.get(i);
            double wx = wall[0];
            double wy = wall[1];
            if (x < wx + TILE && x + tankWidth > wx && y < wy + TILE && y + tankHeight > wy) {
                return true;
            }
        }
        return false;
    }

    // Bullet behaviors settings.
    class Bullet {
        double x, y;
        int dir;
        boolean active = true;

        Bullet(double x, double y, int dir) {
            this.x = x;
            this.y = y;
            this.dir = dir;
        }

        void update() {
            int bulletSpeed;
            if (enemyBullets.contains(this)) {
                bulletSpeed = 8;
            } else {
                bulletSpeed = 8;
            }
            if (dir == 0) {
                x = x + bulletSpeed;
            } else if (dir == 1) {
                y = y + bulletSpeed;
            } else if (dir == 2) {
                x = x - bulletSpeed;
            } else if (dir == 3) {
                y = y - bulletSpeed;
            }
            for (int w = 0; w < wallsList.size(); w++) {
                double[] wall = wallsList.get(w);
                double wx = wall[0];
                double wy = wall[1];
                if (x < wx + TILE && x + 8 > wx && y < wy + TILE && y + 8 > wy) {
                    explosions.add(new double[]{x - 16, y - 16});
                    explosionTimes.add(System.nanoTime());
                    active = false;
                    break;
                }
            }
        }
    }

    // Handles the logic for resetting the game, such as reinitializing player lives, score, tank positions, and any other necessary variables to start a new game session
    private void resetGame() {
        playerLives = 3;
        playerScore = 0;
        playerX = 352;
        playerY = 536;
        bullets.clear();
        enemyBullets.clear();
        explosions.clear();
        explosionTimes.clear();
        enemyTanks.clear();
        enemyDirs.clear();
        enemyTimers.clear();
        enemyFireTimers.clear();
        enemyFireIntervals.clear();
        int[][] spawnPoints = new int[8][2];
        spawnPoints[0][0] = 110; spawnPoints[0][1] = 50;
        spawnPoints[1][0] = 240; spawnPoints[1][1] = 50;
        spawnPoints[2][0] = 420; spawnPoints[2][1] = 50;
        spawnPoints[3][0] = 530; spawnPoints[3][1] = 50;
        spawnPoints[4][0] = 720; spawnPoints[4][1] = 50;
        spawnPoints[5][0] = 180; spawnPoints[5][1] = 110;
        spawnPoints[6][0] = 600; spawnPoints[6][1] = 110;
        spawnPoints[7][0] = 620; spawnPoints[7][1] = 120;
        for (int s = 0; s < spawnPoints.length; s++) {
            double[] spawn = new double[2];
            spawn[0] = spawnPoints[s][0];
            spawn[1] = spawnPoints[s][1];
            enemyTanks.add(spawn);
            int dirRandom = (int)(Math.random() * 4);
            enemyDirs.add(dirRandom);
            enemyTimers.add(System.nanoTime());
            enemyFireTimers.add(System.nanoTime());
            long interval = 400_000_000L + (long)(Math.random() * 500_000_000L);
            enemyFireIntervals.add(interval);
        }
        gameOver = false;
    }

    // Responsible for starting the game, including initializing player positions, enemy tanks, score, lives, and any other components required at the beginning of a new game
    @Override
    public void start(Stage primaryStage) {
        Pane root = new Pane();
        Canvas canvas = new Canvas(800, 600);
        root.getChildren().add(canvas);

        playerTankImages[0] = new Image(getClass().getResource("/assets/yellowTank1.png").toExternalForm());
        playerTankImages[1] = new Image(getClass().getResource("/assets/yellowTank2.png").toExternalForm());
        wallImage = new Image(getClass().getResource("/assets/wall.png").toExternalForm());
        smallExplosionImage = new Image(getClass().getResource("/assets/smallExplosion.png").toExternalForm());
        bulletImage = new Image(getClass().getResource("/assets/bullet.png").toExternalForm());
        explosionImage = new Image(getClass().getResource("/assets/explosion.png").toExternalForm());
        enemyTankImages[0] = new Image(getClass().getResource("/assets/whiteTank1.png").toExternalForm());
        enemyTankImages[1] = new Image(getClass().getResource("/assets/whiteTank2.png").toExternalForm());
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Handles the creation and placement of wall objects on the map, ensuring that they are properly set up to act as barriers and affect tank movement and bullet trajectories
        int x = 0;
        while (x < 800) {
            double[] wall1 = new double[2];
            wall1[0] = x; wall1[1] = 0;
            wallsList.add(wall1);
            double[] wall2 = new double[2];
            wall2[0] = x; wall2[1] = 600 - TILE;
            wallsList.add(wall2);
            x += TILE;
        }
        int y = TILE;
        while (y < 600 - TILE) {
            double[] wall3 = new double[2];
            wall3[0] = 0; wall3[1] = y;
            wallsList.add(wall3);
            double[] wall4 = new double[2];
            wall4[0] = 800 - TILE; wall4[1] = y;
            wallsList.add(wall4);
            y += TILE;
        }
        for (int xx = 280; xx <= 500; xx += TILE) {
            double[] wall = new double[2];
            wall[0] = xx; wall[1] = 335;
            wallsList.add(wall);
        }
        for (int yy = 300; yy <= 500; yy += TILE) {
            double[] wallA = new double[2];
            wallA[0] = 75; wallA[1] = yy;
            wallsList.add(wallA);
            double[] wallB = new double[2];
            wallB[0] = 90; wallB[1] = yy;
            wallsList.add(wallB);
        }
        for (int yy = 380; yy <= 500; yy += TILE) {
            double[] wallA = new double[2];
            wallA[0] = 170; wallA[1] = yy;
            wallsList.add(wallA);
            double[] wallB = new double[2];
            wallB[0] = 185; wallB[1] = yy;
            wallsList.add(wallB);
        }
        for (int yy = 300; yy <= 500; yy += TILE) {
            double[] wallA = new double[2];
            wallA[0] = 700; wallA[1] = yy;
            wallsList.add(wallA);
            double[] wallB = new double[2];
            wallB[0] = 685; wallB[1] = yy;
            wallsList.add(wallB);
        }
        for (int yy = 380; yy <= 500; yy += TILE) {
            double[] wallA = new double[2];
            wallA[0] = 615; wallA[1] = yy;
            wallsList.add(wallA);
            double[] wallB = new double[2];
            wallB[0] = 600; wallB[1] = yy;
            wallsList.add(wallB);
        }

        // This section is responsible for spawning enemy tanks onto the game field, setting their initial positions, directions, and any other properties needed for them to participate in gameplay
        int[][] spawnPoints = new int[8][2];
        spawnPoints[0][0] = 100; spawnPoints[0][1] = 50;
        spawnPoints[1][0] = 250; spawnPoints[1][1] = 50;
        spawnPoints[2][0] = 400; spawnPoints[2][1] = 50;
        spawnPoints[3][0] = 550; spawnPoints[3][1] = 50;
        spawnPoints[4][0] = 700; spawnPoints[4][1] = 50;
        spawnPoints[5][0] = 180; spawnPoints[5][1] = 110;
        spawnPoints[6][0] = 600; spawnPoints[6][1] = 110;
        spawnPoints[7][0] = 620; spawnPoints[7][1] = 120;
        for (int i = 0; i < spawnPoints.length; i++) {
            double[] spawn = new double[2];
            spawn[0] = spawnPoints[i][0];
            spawn[1] = spawnPoints[i][1];
            enemyTanks.add(spawn);
            int dirRandom = (int)(Math.random() * 4);
            enemyDirs.add(dirRandom);
            enemyTimers.add(System.nanoTime());
            enemyFireTimers.add(System.nanoTime());
            long interval = 400_000_000L + (long)(Math.random() * 500_000_000L);
            enemyFireIntervals.add(interval);
        }

        Scene scene = new Scene(root, 800, 600);
        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.P) {
                paused = !paused;
            } else if (event.getCode() == KeyCode.ESCAPE) {
                System.exit(0);
            } else if (event.getCode() == KeyCode.R) {
                resetGame();
            } else {
                pressedKeys.add(event.getCode());
            }
        });

        scene.setOnKeyReleased(event -> pressedKeys.remove(event.getCode()));
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (playerLives <= 0 && !gameOver) {
                    gameOver = true;
                    gameOverTime = now;
                    playerScore = 0;
                }
                if (gameOver) {
                    gc.clearRect(0, 0, 800, 600);
                    gc.fillText("GAME OVER", 340, 300);
                    gc.fillText("Restarting...", 350, 340);
                    if (now - gameOverTime > 2_000_000_000L) {
                        resetGame();
                    }
                    return;
                }
                if (paused) {
                    gc.clearRect(0, 0, 800, 600);
                    gc.fillText("PAUSED", 370, 300);
                    return;
                }
                boolean moved = false;
                if (now - lastMove > 50_000_000) {
                    if (pressedKeys.contains(KeyCode.RIGHT)) {
                        double nextX = playerX + 9;
                        if (!isBlocked(nextX, playerY)) {
                            playerX = nextX;
                            direction = 0;
                            moved = true;
                        }
                    }
                    if (pressedKeys.contains(KeyCode.DOWN)) {
                        double nextY = playerY + 10;
                        if (!isBlocked(playerX, nextY)) {
                            playerY = nextY;
                            direction = 1;
                            moved = true;
                        }
                    }
                    if (pressedKeys.contains(KeyCode.LEFT)) {
                        double nextX = playerX - 10;
                        if (!isBlocked(nextX, playerY)) {
                            playerX = nextX;
                            direction = 2;
                            moved = true;
                        }
                    }
                    if (pressedKeys.contains(KeyCode.UP)) {
                        double nextY = playerY - 10;
                        if (!isBlocked(playerX, nextY)) {
                            playerY = nextY;
                            direction = 3;
                            moved = true;
                        }
                    }
                    if (pressedKeys.contains(KeyCode.X) && now - lastFire > 600_000_000) {
                        double bx = playerX + playerTankImages[0].getWidth() / 2 - 4;
                        double by = playerY + playerTankImages[0].getHeight() / 2 - 4;
                        Bullet newBullet = new Bullet(bx, by, direction);
                        bullets.add(newBullet);
                        lastFire = now;
                    }
                    if (moved) {
                        if (spriteIndex == 0) {
                            spriteIndex = 1;
                        } else {
                            spriteIndex = 0;
                        }
                    }
                    lastMove = now;
                }
                gc.clearRect(0, 0, 800, 600);
                gc.fillText("Lives: " + playerLives, 20, 55);
                gc.fillText("Score: " + playerScore, 20, 30);
                for (int w = 0; w < wallsList.size(); w++) {
                    double[] wall = wallsList.get(w);
                    gc.drawImage(wallImage, wall[0], wall[1]);
                }
                int enemySprite = (int)((System.nanoTime() / 200_000_000) % 2);
                for (int i = 0; i < enemyTanks.size(); i++) {
                    double[] enemy = enemyTanks.get(i);
                    int dir = enemyDirs.get(i);
                    long lastChange = enemyTimers.get(i);
                    if (System.nanoTime() - lastChange > 1_750_000_000L) {
                        int newDir = (int)(Math.random() * 4);
                        while (newDir == dir) {
                            newDir = (int)(Math.random() * 4);
                        }
                        dir = newDir;
                        enemyDirs.set(i, dir);
                        enemyTimers.set(i, System.nanoTime());
                    }
                    double futureX = enemy[0];
                    double futureY = enemy[1];
                    if (dir == 0) {
                        futureX = futureX + 1.70;
                    } else if (dir == 1) {
                        futureY = futureY + 1.70;
                    } else if (dir == 2) {
                        futureX = futureX - 1.70;
                    } else if (dir == 3) {
                        futureY = futureY - 1.70;
                    }
                    boolean blocked = false;
                    double enemyWidth = enemyTankImages[enemySprite].getWidth();
                    double enemyHeight = enemyTankImages[enemySprite].getHeight();
                    for (int w = 0; w < wallsList.size(); w++) {
                        double[] wall = wallsList.get(w);
                        double wx = wall[0];
                        double wy = wall[1];
                        if (futureX < wx + TILE && futureX + enemyWidth > wx && futureY < wy + TILE && futureY + enemyHeight > wy) {
                            blocked = true;
                            break;
                        }
                    }
                    if (!blocked) {
                        enemy[0] = futureX;
                        enemy[1] = futureY;
                    }
                    double ex = enemy[0] + enemyTankImages[enemySprite].getWidth() / 2;
                    double ey = enemy[1] + enemyTankImages[enemySprite].getHeight() / 2;
                    gc.save();
                    gc.translate(ex, ey);
                    if (dir == 0) {
                        gc.rotate(0);
                    } else if (dir == 1) {
                        gc.rotate(90);
                    } else if (dir == 2) {
                        gc.rotate(180);
                    } else if (dir == 3) {
                        gc.rotate(-90);
                    }
                    gc.drawImage(enemyTankImages[enemySprite],
                            -enemyTankImages[enemySprite].getWidth() / 2,
                            -enemyTankImages[enemySprite].getHeight() / 2
                    );
                    gc.restore();

                    // Handles the logic for enemy tank shooting, including bullet creation, fire intervals, and aiming direction to ensure challenging gameplay for the player
                    long lastFireTime = enemyFireTimers.get(i);
                    long interval = enemyFireIntervals.get(i);
                    if (System.nanoTime() - lastFireTime > interval) {
                        double bx = enemy[0] + enemyTankImages[0].getWidth() / 2 - 4;
                        double by = enemy[1] + enemyTankImages[0].getHeight() / 2 - 4;
                        Bullet bullet = new Bullet(bx, by, dir);
                        enemyBullets.add(bullet);
                        enemyFireTimers.set(i, System.nanoTime());
                        long newInterval = 1_200_000_000L + (long)(Math.random() * 1_000_000_000L);
                        enemyFireIntervals.set(i, newInterval);
                    }
                }

                // Controls how the player's bullets are created, updated, and interact with other game objects throughout the gameplay
                List<Bullet> aliveBullets = new ArrayList<>();
                for (int j = 0; j < bullets.size(); j++) {
                    Bullet bullet = bullets.get(j);
                    bullet.update();
                    boolean hitEnemy = false;
                    for (int k = 0; k < enemyTanks.size(); k++) {
                        double[] enemy = enemyTanks.get(k);
                        double ex = enemy[0];
                        double ey = enemy[1];
                        double ew = enemyTankImages[0].getWidth();
                        double eh = enemyTankImages[0].getHeight();
                        if (bullet.x < ex + ew && bullet.x + 8 > ex &&
                                bullet.y < ey + eh && bullet.y + 8 > ey) {
                            explosions.add(new double[]{ex + ew / 2 - 16, ey + eh / 2 - 16});
                            explosionTimes.add(System.nanoTime());
                            bullet.active = false;
                            playerScore += 10;
                            enemyTanks.remove(k);
                            enemyDirs.remove(k);
                            enemyTimers.remove(k);
                            enemyFireTimers.remove(k);
                            enemyFireIntervals.remove(k);
                            int[][] spawnPoints = {
                                    {120, 50}, {270, 50}, {423, 50}, {523, 50}, {684, 50}, {184, 110}, {594, 110}
                            };
                            int rnd = (int) (Math.random() * spawnPoints.length);
                            double[] spawn = new double[2];
                            spawn[0] = spawnPoints[rnd][0];
                            spawn[1] = spawnPoints[rnd][1];
                            enemyTanks.add(spawn);
                            int dirRandom = (int) (Math.random() * 4);
                            enemyDirs.add(dirRandom);
                            enemyTimers.add(System.nanoTime());
                            enemyFireTimers.add(System.nanoTime());
                            long interval = 1_200_000_000L + (long) (Math.random() * 1_000_000_000L);
                            enemyFireIntervals.add(interval);

                            hitEnemy = true;
                            break;
                        }
                    }
                    if (!hitEnemy && bullet.active) {
                        gc.drawImage(bulletImage, bullet.x, bullet.y);
                        aliveBullets.add(bullet);
                    }
                }
                bullets = aliveBullets;

                // Manages enemy bullet behavior, including their creation, movement, and interactions with the player and obstacles
                List<Bullet> aliveEnemyBullets = new ArrayList<>();
                for (int j = 0; j < enemyBullets.size(); j++) {
                    Bullet bullet = enemyBullets.get(j);
                    bullet.update();
                    double px = playerX;
                    double py = playerY;
                    double pw = playerTankImages[0].getWidth();
                    double ph = playerTankImages[0].getHeight();
                    if (bullet.x < px + pw && bullet.x + 8 > px &&
                            bullet.y < py + ph && bullet.y + 8 > py) {
                        explosions.add(new double[]{px + pw / 2 - 16, py + ph / 2 - 16});
                        explosionTimes.add(System.nanoTime());
                        bullet.active = false;
                        playerLives--;
                        playerX = 352;
                        playerY = 536;
                    }
                    if (bullet.active) {
                        gc.drawImage(bulletImage, bullet.x, bullet.y);
                        aliveEnemyBullets.add(bullet);
                    }
                }
                enemyBullets = aliveEnemyBullets;

                // Explosion settings .
                for (int e = explosions.size() - 1; e >= 0; e--) {
                    double[] pos = explosions.get(e);
                    long expTime = explosionTimes.get(e);
                    if (System.nanoTime() - expTime < 600_000_000L) {
                        gc.drawImage(smallExplosionImage, pos[0], pos[1]);
                    } else {
                        explosions.remove(e);
                        explosionTimes.remove(e);
                    }
                }

                // Drawing tanks and makes rotates .
                gc.save();
                double px = playerX + playerTankImages[spriteIndex].getWidth() / 2;
                double py = playerY + playerTankImages[spriteIndex].getHeight() / 2;
                gc.translate(px, py);
                if (direction == 0) {
                    gc.rotate(0);
                } else if (direction == 1) {
                    gc.rotate(90);
                } else if (direction == 2) {
                    gc.rotate(180);
                } else if (direction == 3) {
                    gc.rotate(-90);
                }
                gc.drawImage(playerTankImages[spriteIndex],
                        -playerTankImages[spriteIndex].getWidth() / 2,
                        -playerTankImages[spriteIndex].getHeight() / 2
                );
                gc.restore();
            }
        }.start();

        primaryStage.setScene(scene);
        primaryStage.setTitle("Tank 2025");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}