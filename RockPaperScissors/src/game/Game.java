package game;

import menu.Frame;
import menu.buttons.EButtons;
import handlers.Panels;

import javax.swing.ImageIcon;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Random;

//Hlavna trieda, kde prebieha jadro a procesy hry ako pohyb, kolizie a vyhodnocovanie vitaza.
public class Game extends Panels implements KeyListener, ActionListener {

    //Moje triedy
    private final Timer timer;
    private final Pause pause;
    private final EndScreen endScreen;

    private String winner;
    private int speed = 1;

    //Premenne na meranie casu ktore sa posielaju do GameTimeru
    private final GameTimer gameTimer;
    private double elapsedTime;

    //Zoznam entit
    private final ArrayList<Entity> entities = new ArrayList<>();
    private final Stats statistics = new Stats();

    //Generovanie nahodnej premennej
    private final Random random = new Random(System.nanoTime());

    public Game(Frame pFrame, int[] pNumOfObj, String mapPath, String[] skinPaths) {
        super(pFrame, mapPath, skinPaths);
        super.addKeyListener(this);

        this.setupPanel(super.getFrame().getScreenWidth(), super.getFrame().getScreenHeight() - 20);
        super.getFrame().setExtendedState(java.awt.Frame.MAXIMIZED_BOTH);

        this.gameTimer = new GameTimer();
        this.pause = new Pause(super.getFrame(), this);
        this.endScreen = new EndScreen(super.getFrame(), super.getMapPath(), super.getSkinPaths(), this, pNumOfObj);

        this.timer = new Timer(0, this);
        this.timer.start();
        this.setupEntities(pNumOfObj[0], pNumOfObj[1], pNumOfObj[2]);

        if (!this.isVisible()) {
            this.setVisible(true);
        }
        this.requestFocus();
    }

    //Nastavuje spawn entit
    public void setupEntities(int numOfRocks, int numOfPapers, int numOfScissors) {
        for (int i = 0; i < numOfRocks; i++) {
            this.entities.add(new Entity("ROCK", this.setLocation(), this.speed, super.getSkinPaths()));
        }
        for (int i = 0; i < numOfPapers; i++) {
            this.entities.add(new Entity("PAPER", this.setLocation(), this.speed, super.getSkinPaths()));
        }
        for (int i = 0; i < numOfScissors; i++) {
            this.entities.add(new Entity("SCISSORS", this.setLocation(), this.speed, super.getSkinPaths()));
        }
    }

    //Metoda ktora opravuje, aby sa entity nespawnovali cez seba, resp. na rovnakom mieste
    public int[] setLocation() {
        int width = super.getFrame().getScreenWidth();
        int height = super.getFrame().getScreenHeight();
        int[] xy = { this.random.nextInt((width - 105) - 60) + 60 , this.random.nextInt((height - 105) - 60) + 60 };
        while (this.collisionForEntity(xy[0], xy[1])) {
            xy[0] = this.random.nextInt((width - 105) - 60) + 60;
            xy[1] = this.random.nextInt((height - 105) - 60) + 60;
        }
        return xy;
    }

    //Metoda ktora kontroluje kolizie entit pri spawne
    public boolean collisionForEntity(int x, int y) {
        for (Entity entity : this.entities) {
            if (Math.abs(x - entity.getX()) < 50 && Math.abs(y - entity.getY()) < 50) {
                return true;
            }
        }
        return false;
    }

    //Metoda ktora riesi pohyb objektov a ich odrazy
    private void moveEntities() {
        for (Entity entity : this.entities) {
            if (entity.getX() > super.getFrame().getScreenWidth() - ((super.getFrame().getScreenWidth() / 29.09) * 1.7)) {
                entity.setxDir(-this.speed);
            }
            if (entity.getX() < super.getFrame().getScreenWidth() / 29.09) {
                entity.setxDir(this.speed);
            }
            if (entity.getY() > super.getFrame().getScreenHeight() - ((super.getFrame().getScreenHeight() - 20) / 16.5) * 2.1) {
                entity.setyDir(-this.speed);
            }
            if (entity.getY() < (super.getFrame().getScreenHeight() - 20) / 16.2) {
                entity.setyDir(this.speed);
            }
            entity.updateSpeed(this.speed);
            entity.updateX();
            entity.updateY();
            this.statistics.addDistance();
            //Kontrola ci je v kolizii s inou entitou
            for (Entity otherEntity : this.entities) {
                if (entity != otherEntity && entity.jeVKoliziiS(otherEntity)) {
                    this.core(entity, otherEntity);
                    this.statistics.setTouches();
                }
            }
        }
    }

    //Jadro funkcionality simulatora, riesi co sa stane pri kolizii objektov a meni dane entity na cielove
    private void core(Entity entity1, Entity entity2) {
        if (entity1.getEntityType() == 'R' && entity2.getEntityType() == 'S') {
            entity2.setEntity("ROCK");
            entity1.collisionHandler(entity2);
            this.statistics.setKills(1, 0, 0);
        } else if (entity1.getEntityType() == 'P' && entity2.getEntityType() == 'R') {
            entity2.setEntity("PAPER");
            entity1.collisionHandler(entity2);
            this.statistics.setKills(0, 1, 0);
        } else if (entity1.getEntityType() == 'S' && entity2.getEntityType() == 'P') {
            entity2.setEntity("SCISSORS");
            entity1.collisionHandler(entity2);
            this.statistics.setKills(0, 0, 1);
        }
    }

    //cele GUI
    protected void drawScreen(Graphics2D g2d) {
        g2d.drawImage(new ImageIcon(super.getMapPath()).getImage(), 0, 0, super.getFrame().getScreenWidth(), super.getFrame().getScreenHeight() - 20, null);

        for (Entity entity : this.entities) {
            g2d.drawImage(entity.getImage(), entity.getX(), entity.getY(), null);
        }
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial Bold", Font.BOLD, 25));
        g2d.drawString(("SPEED: " + this.speed), super.getFrame().getScreenWidth() / 2 - 50, super.getFrame().getScreenHeight() - 35);
        g2d.drawImage(new ImageIcon("assets/BUTTONS/downArr.png").getImage(), super.getFrame().getScreenWidth() / 2 - 100, super.getFrame().getScreenHeight() - 60, 35, 35, null);
        if (this.speed > 9) {
            g2d.drawImage(new ImageIcon("assets/BUTTONS/upArr.png").getImage(), super.getFrame().getScreenWidth() / 2 + 90, super.getFrame().getScreenHeight() - 60, 35, 35, null);
        } else {
            g2d.drawImage(new ImageIcon("assets/BUTTONS/upArr.png").getImage(), super.getFrame().getScreenWidth() / 2 + 80, super.getFrame().getScreenHeight() - 60, 35, 35, null);
        }
    }

    //GameLoop a "win" podmienky
    //Obnovuje sa kazdy tick a diriguje movement, zaroven kontroluje kolizie a kontroluje ci su splnene podmienky na ukoncenie hry
    @Override
    public void actionPerformed(ActionEvent e) {
        if (!this.winCondition()) {
            this.moveEntities();
            this.repaint();
        } else {
            this.endGame();
        }
    }

    //4 metody ktore spolupracuju pri kontrole ci su splnene vsetky podmienky na vyhru
    private boolean winCondition() {
        if (this.areAllRock()) {
            this.winner = "ROCK";
            return true;
        }
        if (this.areAllPaper()) {
            this.winner = "PAPER";
            return true;
        }
        if (this.areAllScissors()) {
            this.winner = "SCISSORS";
            return true;
        }
        return false;
    }

    private boolean areAllRock() {
        for (Entity entity : this.entities) {
            if (entity.getEntityType() == 'P' || entity.getEntityType() == 'S') {
                return false;
            }
        }
        return true;
    }
    private boolean areAllPaper() {
        for (Entity entity : this.entities) {
            if (entity.getEntityType() == 'R' || entity.getEntityType() == 'S') {
                return false;
            }
        }
        return true;
    }
    private boolean areAllScissors() {
        for (Entity entity : this.entities) {
            if (entity.getEntityType() == 'P' || entity.getEntityType() == 'R') {
                return false;
            }
        }
        return true;
    }

    private void endGame() {
        this.timer.stop();
        this.gameTimer.stopTimer(this.speed);
        this.elapsedTime = this.gameTimer.getTotalTime();
        this.endScreen.paintEndScreen((Graphics2D)this.getGraphics(), this.winner, this.statistics);
    }

    //Funkcie keyboard inputu
    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        //Pauza -> trieda Options
        if (code == KeyEvent.VK_ESCAPE) {
            if (this.timer.isRunning()) {
                this.pause.optionsMenu();
                this.timer.stop();
            } else {
                this.pause.optionsMenu();
                this.timer.start();
            }
        }
        //Nastavovanie rychlosti simulacie
        if (code == KeyEvent.VK_UP) {
            if (this.speed < 20) {
                this.speed++;
                this.gameTimer.update(this.speed);
            }
        }
        if (code == KeyEvent.VK_DOWN) {
            if (this.speed > 1) {
                this.speed--;
                this.gameTimer.update(this.speed);
            }
        }
        if (code == KeyEvent.VK_Q) {
            this.winner = "ROCK";
            this.endGame();
        }
    }
    @Override
    public void keyReleased(KeyEvent e) {
    }
    @Override
    public void keyTyped(KeyEvent e) {
    }

    public Timer getTimer() {
        return this.timer;
    }

    public double getElapsedTime() {
        return this.elapsedTime;
    }


    @Override
    protected void setupButtons() {

    }
    @Override
    public void onButtonClick(EButtons button) {

    }
}