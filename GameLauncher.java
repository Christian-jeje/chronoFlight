import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.ImageObserver;
import java.util.*;
import java.util.List;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;
import javax.swing.Timer;


/**
 * Full game with:
 * - Start/interface screen (coins, highest score, Exit button, level selection 1..10)
 * - Persistent save using ProgressSave (unlockedLevel, coins, highScore)
 * - Resized bird and tubes
 * - Timer countdown displayed beside score & timer power-up collectible
 * - Coin collectibles
 *
 * Place assets (bird.png, TubeBody.png, TubeTop.png, background.jpg, coin.png, timer_icon.png)
 * on the classpath or same directory as compiled classes so getResource works.
 */
public class GameLauncher {

    public static int WIDTH = 900;
    public static int HEIGHT = 600;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GamePanel game = new GamePanel();
            JFrame frame = new JFrame("Flappy Bird - Levels & Progress");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.add(game);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            game.requestFocusInWindow();
        });
    }
}

/* ---------- Progress data object is handled by ProgressSave.java (external) ---------- */

/* ---------- Image proxy & scaling utilities (keeps your ProxyImage/RealImage idea) ---------- */
interface IImage {
    ImageIcon loadImage(int desiredW, int desiredH);
}

class ProxyImage implements IImage {
    private final String src;
    private RealImage realImage;

    public ProxyImage(String src) {
        this.src = src;
    }

    @Override
    public ImageIcon loadImage(int desiredW, int desiredH) {
        if (realImage == null) {
            realImage = new RealImage(src);
        }
        return realImage.loadImage(desiredW, desiredH);
    }
}

class RealImage implements IImage {
    private final String src;
    private ImageIcon imageIcon;

    public RealImage(String src) {
        this.src = src;
    }

    @Override
    public ImageIcon loadImage(int desiredW, int desiredH) {
        if (imageIcon == null) {
            java.net.URL url = getClass().getResource(src);
            if (url == null) {
                // fallback placeholder
                imageIcon = new ImageIcon(new BufferedImage(Math.max(1, desiredW), Math.max(1, desiredH), BufferedImage.TYPE_INT_ARGB));
            } else {
                imageIcon = new ImageIcon(url);
            }
        }
        if (desiredW > 0 && desiredH > 0) {
            Image scaled = imageIcon.getImage().getScaledInstance(desiredW, desiredH, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        } else {
            return imageIcon;
        }
    }
}

/* ---------- Base class ---------- */
abstract class GameObject {
    protected int x, y;
    protected int dx = 0, dy = 0;
    protected int width, height;
    protected Image image;

    public GameObject(int x, int y) {
        this.x = x;
        this.y = y;
    }
    public int getX(){return x;}
    public int getY(){return y;}
    public int getWidth(){return width;}
    public int getHeight(){return height;}
    public Image getImage(){return image;}
    public void setDx(int dx){ this.dx = dx;}
    public Rectangle getBounds(){ return new Rectangle(x,y,width,height); }
    public abstract void tick();
    public abstract void render(Graphics2D g, ImageObserver obs);
}

/* ---------- Tube (body + optional top) ---------- */
class Tube extends GameObject {
    private ProxyImage bodyProxy, topProxy;
    private Image topImage;
    private boolean isTopDrawn = true;

    public Tube(int x, int y, int w, int h, boolean drawTop) {
        super(x, y);
        this.width = w;
        this.height = h;
        this.isTopDrawn = drawTop;
        bodyProxy = new ProxyImage("TubeBody.png");
        topProxy = new ProxyImage("TubeTop.png");
        this.image = bodyProxy.loadImage(width, height).getImage();
        if(isTopDrawn) {
            this.topImage = topProxy.loadImage(width, (int)(height*0.4)).getImage();
        }
    }

    @Override
    public void tick() {
        this.x -= dx;
    }

    @Override
    public void render(Graphics2D g, ImageObserver obs) {
        g.drawImage(image, x, y, obs);
        if (isTopDrawn && topImage != null) {
            int topH = topImage.getHeight(null);
            g.drawImage(topImage, x, y - topH + 2, obs);
        }
    }
}

/* ---------- Coin collectible ---------- */
class Coin extends GameObject {
    private ProxyImage proxy;
    public boolean collected = false;

    public Coin(int x, int y, int size) {
        super(x, y);
        this.width = size;
        this.height = size;
        proxy = new ProxyImage("coin.png");
        this.image = proxy.loadImage(size, size).getImage();
    }

    @Override
    public void tick() {
        this.x -= dx;
    }

    @Override
    public void render(Graphics2D g, ImageObserver obs) {
        if (!collected) g.drawImage(image, x, y, obs);
    }
}

/* ---------- Timer power-up collectible ---------- */
class TimerPower extends GameObject {
    private ProxyImage proxy;
    public boolean collected = false;

    public TimerPower(int x, int y, int size) {
        super(x, y);
        this.width = size;
        this.height = size;
        proxy = new ProxyImage("timer_icon.png");
        this.image = proxy.loadImage(size, size).getImage();
    }

    @Override
    public void tick() {
        this.x -= dx;
    }

    @Override
    public void render(Graphics2D g, ImageObserver obs) {
        if (!collected) g.drawImage(image, x, y, obs);
    }
}

/* ---------- Bird ---------- */
class Bird extends GameObject {
    private ProxyImage proxyImage;
    public Bird(int x, int y, int birdW, int birdH) {
        super(x, y);
        if (proxyImage == null) {
            proxyImage = new ProxyImage("bird.png");
        }
        this.image = proxyImage.loadImage(birdW, birdH).getImage();
        this.width = birdW;
        this.height = birdH;
        // position correction to center
        this.x -= width/2;
        this.y -= height/2;
        this.dy = 2;
    }

    @Override
    public void tick() {
        if (dy < 8) dy += 1;
        this.y += dy;
        checkWindowBorder();
    }

    public void jump() {
        if (dy > 0) dy = 0;
        dy -= 14;
    }

    private void checkWindowBorder() {
        if (this.x > GameLauncher.WIDTH - width) this.x = GameLauncher.WIDTH - width;
        if (this.x < 0) this.x = 0;
        if (this.y > GameLauncher.HEIGHT - 50 - height) this.y = GameLauncher.HEIGHT - 50 - height;
        if (this.y < 0) this.y = 0;
    }

    @Override
    public void render(Graphics2D g, ImageObserver obs) {
        g.drawImage(image, x, y, obs);
    }

    @Override
    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }
}

/* ---------- TubeColumn with level support, coins and timer power-ups ---------- */
class TubeColumn {
    private int base = GameLauncher.HEIGHT - 60;
    private List<Tube> tubes;
    private List<Coin> coins;
    private List<TimerPower> timers;
    private Random random;
    private int points = 0;
    private int speed = 5;
    private int changeSpeed = speed;
    private int level = 1; // influences speed & gap

    public TubeColumn(int level) {
        this.level = level;
        tubes = new ArrayList<>();
        coins = new ArrayList<>();
        timers = new ArrayList<>();
        random = new Random();
        initTubes();
    }

    private void initTubes() {
        tubes.clear();
        coins.clear();
        timers.clear();

        int last = base;
        int randWay = random.nextInt(6); // random gap position factor
        int tubeWidth = 80; // base width
        int gapSize = Math.max(140 - (level*8), 80); // higher level -> smaller gap (harder)
        int startX = GameLauncher.WIDTH + 50;

        // Create a vertical stack of tube pieces leaving a gap
        // We'll create top half and bottom half as two Tube objects to allow top decoration.
        // For simplicity create a few sequences across screen.
        for (int i = 0; i < 5 + level; i++) {
            // compute top tube (y coordinate for top piece)
            int gapY = 120 + randWay*40 + i*30 - level*2 + random.nextInt(40);
            if (gapY < 80) gapY = 80;
            int topHeight = gapY - 100;
            if (topHeight < 40) topHeight = 40;
            Tube top = new Tube(startX + i*280, topHeight - 10, tubeWidth, topHeight, true);
            top.setDx(speed + level/2);
            // bottom tube placed below gap
            Tube bottom = new Tube(startX + i*280, gapY + gapSize, tubeWidth, GameLauncher.HEIGHT - (gapY+gapSize) - 60, false);
            bottom.setDx(speed + level/2);

            tubes.add(top);
            tubes.add(bottom);

            // occasionally add a coin inside the gap (to the right x coordinate)
            if (random.nextDouble() < 0.6) {
                int coinSize = 24;
                int coinX = startX + i*280 + tubeWidth + 40;
                int coinY = gapY + gapSize/2 - coinSize/2 + random.nextInt(30) - 15;
                Coin c = new Coin(coinX, coinY, coinSize);
                c.setDx(speed + level/2);
                coins.add(c);
            }
            // occasionally add a timer power-up
            if (random.nextDouble() < 0.18) {
                int ts = 28;
                int tx = startX + i*280 + tubeWidth + 10 + random.nextInt(60);
                int ty = gapY + 20 + random.nextInt(Math.max(10, gapSize-40));
                TimerPower tp = new TimerPower(tx, ty, ts);
                tp.setDx(speed + level/2);
                timers.add(tp);
            }
        }
    }

    public void tick() {
        Iterator<Tube> ti = tubes.iterator();
        while(ti.hasNext()) {
            Tube t = ti.next();
            t.tick();
            if (t.getX()+t.getWidth() < 0) ti.remove();
        }
        Iterator<Coin> ci = coins.iterator();
        while (ci.hasNext()) {
            Coin c = ci.next();
            c.tick();
            if (c.getX() + c.getWidth() < 0 || c.collected) ci.remove();
        }
        Iterator<TimerPower> pti = timers.iterator();
        while (pti.hasNext()) {
            TimerPower p = pti.next();
            p.tick();
            if (p.getX() + p.getWidth() < 0 || p.collected) pti.remove();
        }

        if (tubes.isEmpty()) {
            this.points += 1;
            if (changeSpeed == points) {
                this.speed += 1;
                changeSpeed += 5;
            }
            // create new columns when empty
            initTubes();
        }
    }

    public void render(Graphics2D g, ImageObserver obs) {
        for (Tube t : tubes) t.render(g, obs);
        for (Coin c : coins) c.render(g, obs);
        for (TimerPower tp : timers) tp.render(g, obs);
    }

    public List<Tube> getTubes() { return tubes; }
    public List<Coin> getCoins() { return coins; }
    public List<TimerPower> getTimers() { return timers; }

    public int getPoints() { return points; }
    public void setPoints(int p) { this.points = p; }

    public void setSpeed(int s) {
        this.speed = s;
        for (Tube t : tubes) t.setDx(s + level/2);
        for (Coin c : coins) c.setDx(s + level/2);
        for (TimerPower tp : timers) tp.setDx(s + level/2);
    }
}

/* ---------- Controller (space to jump) ---------- */
class Controller {
    public void controllerReleased(Bird bird, KeyEvent kevent) {
        if (kevent.getKeyCode() == KeyEvent.VK_SPACE) bird.jump();
    }
}

/* ---------- Game Panel (main) ---------- */
class GamePanel extends JPanel implements ActionListener {
    private boolean isRunning = false;
    private ProxyImage bgProxy;
    private Image background;
    private Bird bird;
    private TubeColumn tubeColumn;
    private int score = 0;
    private int highScore = 0;
    private int coins = 0;
    private Timer gameTimer;
    private Controller controller;
    private int birdW = 48, birdH = 36; // resized bird
    private int currentLevel = 1;
    private ProgressSave.Progress progress;

    // interface states
    private boolean onMenu = true;
    private long levelTimeSeconds = 60; // each level timer initial (can be changed per level)
    private long timeLeft = levelTimeSeconds;
    private boolean timerFrozen = false; // when timer power-up collected, we add time

    public GamePanel() {
        setPreferredSize(new Dimension(GameLauncher.WIDTH, GameLauncher.HEIGHT));
        setFocusable(true);
        setDoubleBuffered(true);
        controller = new Controller();
        addKeyListener(new GameKeyAdapter());
        // load progress
        progress = ProgressSave.load();
        this.coins = progress.coins;
        this.highScore = progress.highScore;
        this.currentLevel = Math.min(progress.unlockedLevel, 10);

        bgProxy = new ProxyImage("background.jpg");
        background = bgProxy.loadImage(GameLauncher.WIDTH, GameLauncher.HEIGHT).getImage();

        gameTimer = new Timer(15, this);
        gameTimer.start();
    }

    private void startLevel(int level) {
        this.currentLevel = level;
        this.isRunning = true;
        this.onMenu = false;
        this.score = 0;
        int bw = birdW;
        int bh = birdH;
        this.bird = new Bird(GameLauncher.WIDTH / 2, GameLauncher.HEIGHT / 2, bw, bh);
        this.tubeColumn = new TubeColumn(level);
        this.timeLeft = Math.max(15, 60 - level*3); // higher levels shorter time
        this.timerFrozen = false;
    }

    private void backToMenu() {
        this.isRunning = false;
        this.onMenu = true;
        // save progress
        progress.coins = coins;
        if (highScore > progress.highScore) progress.highScore = highScore;
        progress.unlockedLevel = Math.max(progress.unlockedLevel, currentLevel);
        ProgressSave.save(progress);
    }

    private void endGame() {
        this.isRunning = false;
        if (this.tubeColumn.getPoints() > highScore) {
            this.highScore = this.tubeColumn.getPoints();
        }
        // unlock next level if passed and not max
        if (this.tubeColumn.getPoints() >= requiredPointsToPass(currentLevel)) {
            progress.unlockedLevel = Math.max(progress.unlockedLevel, Math.min(10, currentLevel + 1));
        }
        progress.coins = coins;
        progress.highScore = highScore;
        ProgressSave.save(progress);
        // show menu
        onMenu = true;
    }

    private int requiredPointsToPass(int level) {
        // simple threshold per level (can be tuned)
        return 3 + level; // pass after collecting (or surviving) some tube cycles
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Toolkit.getDefaultToolkit().sync();
        if (isRunning) {
            bird.tick();
            tubeColumn.tick();
            checkCollision();
            score++;
            // decrease timer (timing with ticks: 1000ms ~= 1000/15 ticks ; approximate)
            if (!timerFrozen) {
                // decrement roughly every 1000ms
                long ticksPassed = Math.max(1, 15);
                // we have 15ms per tick -> subtract 15ms from timeLeft*1000
                // simpler: count frames and reduce when enough frames passed
                // implement with system time:
            }
            // precise timer using system time
        }
        repaint();
    }

    private long lastTimeUpdate = System.currentTimeMillis();

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        // background
        g2.drawImage(background, 0, 0, null);

        if (onMenu) {
            drawMenu(g2);
        } else if (isRunning) {
            tubeColumn.render(g2, this);
            bird.render(g2, this);
            // draw HUD: points from tubeColumn, coins, level, timer, highscore
            g2.setColor(Color.black);
            g2.setFont(new Font("MV Boli", Font.BOLD, 24));
            g2.drawString("Score: " + tubeColumn.getPoints(), 10, 40);

            g2.drawString("Level: " + currentLevel, 10, 75);

            g2.drawString("Coins: " + coins, 10, 110);
            g2.drawString("High Score: " + highScore, GameLauncher.WIDTH - 240, 40);

            // timer: compute time left (ms)
            long now = System.currentTimeMillis();
            if (!timerFrozen) {
                long delta = now - lastTimeUpdate;
                if (delta >= 200) { // update every 200ms for smoother decrement
                    timeLeft -= delta / 1000;
                    lastTimeUpdate = now;
                }
            } else {
                lastTimeUpdate = now;
            }
            if (timeLeft <= 0) {
                // time over -> end level
                endGame();
            }
            // Draw timer icon and time
            ProxyImage timerProxy = new ProxyImage("timer_icon.png");
            ImageIcon ti = timerProxy.loadImage(28,28);
            g2.drawImage(ti.getImage(), GameLauncher.WIDTH/2 - 40, 10, this);
            g2.drawString(String.format("%02d", Math.max(0, timeLeft)), GameLauncher.WIDTH/2, 30);
        } else {
            // game not running but not menu (rare), show prompt
            g2.setColor(Color.black);
            g2.setFont(new Font("MV Boli", Font.BOLD, 40));
            g2.drawString("Press Enter to Start", GameLauncher.WIDTH/2 - 240, GameLauncher.HEIGHT/2);
        }
    }

    private void drawMenu(Graphics2D g2) {
        g2.setColor(new Color(0,0,0,140));
        g2.fillRect(0, 0, GameLauncher.WIDTH, GameLauncher.HEIGHT);
        g2.setColor(Color.white);
        g2.setFont(new Font("MV Boli", Font.BOLD, 40));
        g2.drawString("Flappy - Levels", GameLauncher.WIDTH/2 - 160, 80);

        g2.setFont(new Font("MV Boli", Font.PLAIN, 22));
        g2.drawString("Coins: " + coins, 30, 140);
        g2.drawString("High Score: " + highScore, 30, 170);

        // Exit button (drawn)
        g2.setColor(Color.lightGray);
        g2.fillRoundRect(GameLauncher.WIDTH - 140, GameLauncher.HEIGHT - 70, 120, 40, 10, 10);
        g2.setColor(Color.black);
        g2.setFont(new Font("MV Boli", Font.BOLD, 20));
        g2.drawString("Exit", GameLauncher.WIDTH - 90, GameLauncher.HEIGHT - 42);

        // Level grid 1..10
        int cols = 5;
        int rows = 2;
        int startX = 120;
        int startY = 140;
        int boxW = 110;
        int boxH = 70;
        int gap = 20;

        g2.setFont(new Font("MV Boli", Font.BOLD, 20));
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int idx = r*cols + c + 1;
                int x = startX + c*(boxW + gap);
                int y = startY + r*(boxH + gap);
                // locked?
                boolean locked = idx > progress.unlockedLevel;
                // draw background box
                g2.setColor(locked ? Color.darkGray : Color.white);
                g2.fillRoundRect(x, y, boxW, boxH, 12, 12);
                g2.setColor(locked ? Color.gray : Color.black);
                g2.drawRoundRect(x, y, boxW, boxH, 12, 12);
                g2.setColor(locked ? Color.lightGray : Color.black);
                g2.drawString("Level " + idx, x + 15, y + 40);
            }
        }

        // Instructions
        g2.setColor(Color.white);
        g2.setFont(new Font("MV Boli", Font.PLAIN, 14));
        g2.drawString("Click a level to play. You must finish lower levels to unlock higher ones.", 100, 340);
        g2.drawString("In-game: SPACE to jump. Collect coins and timer power-ups.", 100, 360);

        // detect mouse over for clicks - add a listener
        // ensure listener added once
        if (mouseListenerAdded == false) {
            addMenuMouseListener(startX, startY, boxW, boxH, gap, cols, rows);
            mouseListenerAdded = true;
        }
    }

    private boolean mouseListenerAdded = false;

    private void addMenuMouseListener(int startX, int startY, int boxW, int boxH, int gap, int cols, int rows) {
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Point p = e.getPoint();
                // check exit click area
                Rectangle exitRect = new Rectangle(GameLauncher.WIDTH - 140, GameLauncher.HEIGHT - 70, 120, 40);
                if (exitRect.contains(p)) {
                    // save and exit
                    progress.coins = coins;
                    progress.highScore = highScore;
                    progress.unlockedLevel = Math.max(progress.unlockedLevel, currentLevel);
                    ProgressSave.save(progress);
                    System.exit(0);
                }

                // check level boxes
                for (int r = 0; r < rows; r++) {
                    for (int c = 0; c < cols; c++) {
                        int idx = r*cols + c + 1;
                        int x = startX + c*(boxW + gap);
                        int y = startY + r*(boxH + gap);
                        Rectangle rect = new Rectangle(x, y, boxW, boxH);
                        if (rect.contains(p)) {
                            if (idx <= progress.unlockedLevel) {
                                // start game at this level
                                startLevel(idx);
                            } else {
                                // locked: small feedback
                                JOptionPane.showMessageDialog(GamePanel.this, "Level " + idx + " is locked. Finish earlier levels first.");
                            }
                        }
                    }
                }
            }
        });
    }

    private void checkCollision() {
        // check tube collision
        Rectangle rectBird = bird.getBounds();
        for (Tube t : new ArrayList<>(tubeColumn.getTubes())) {
            if (rectBird.intersects(t.getBounds())) {
                endGame();
                return;
            }
        }
        // coins
        for (Coin c : new ArrayList<>(tubeColumn.getCoins())) {
            if (rectBird.intersects(c.getBounds())) {
                c.collected = true;
                coins += 1;
            }
        }
        // timers
        for (TimerPower tp : new ArrayList<>(tubeColumn.getTimers())) {
            if (rectBird.intersects(tp.getBounds())) {
                tp.collected = true;
                // add time bonus and freeze for a short duration
                timeLeft += 8; // add seconds
                // optional small freeze effect
                timerFrozen = false;
            }
        }
    }

    class GameKeyAdapter extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            if (onMenu) {
                // Enter starts the currently selected level if unlocked
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    startLevel(currentLevel);
                }
            } else {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    // go back to menu and save
                    backToMenu();
                } else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    controller.controllerReleased(bird, e);
                }
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            // nothing
        }
    }
}
