/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hittest;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import static javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;

public class HitTest {
    public static void main(String[] args) {
        new HitTest();
    }

    public HitTest() {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                JFrame frame = new JFrame();
                frame.add(new TestPane());
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        });
    }

    enum PlayerDirectionMovement {
        LEFT, RIGHT
    }

    public class TestPane extends JPanel {

        private Player player = new Player();
        private Point playerPoint;
        private Line2D lineOfSight;

        private PlayerDirectionMovement playerDirectionMovement;

        private List<Entity> entities = new ArrayList<>(128);
        private List<Entity> sightedEntities = new ArrayList<>(128);
        private List<Entity> unsightedEntities = new ArrayList<>(128);

        public TestPane() {
            setBackground(Color.BLACK);
            playerPoint = new Point(getWidth() / 2, getHeight() - (player.getSize().height / 2));
            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    playerPoint = new Point(getWidth() / 2, getHeight() - (player.getSize().height / 2));
                }
            });

            setupKeyBindings();
            makeBlocks();
            makeEnemies();

            lineOfSight = new Line2D.Double(0, 0, 0, 0);

            Timer timer = new Timer(5, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Point2D viewPoint = getPointOnCircle(player.getAngleOfView(), Math.max(getWidth(), getHeight()));
                    viewPoint.setLocation(viewPoint.getX() + playerPoint.getX(), viewPoint.getY() + playerPoint.getY());
//                    lineOfSight = new Line2D.Double(new Point2D.Double(playerPoint.getX(), playerPoint.getY()), viewPoint);
                    lineOfSight.setLine(playerPoint, viewPoint);

                    updateEnemies();
                    updatePlayerLookDirection();
                    updateCanSee();

                    unsightedEntities.clear();
                    unsightedEntities.addAll(entities);
                    unsightedEntities.removeAll(sightedEntities);

                    repaint();
                }
            });
            timer.start();
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(1920, 1080);
        }

        protected void setupKeyBindings() {
            InputMap im = getInputMap(WHEN_IN_FOCUSED_WINDOW);
            ActionMap am = getActionMap();

            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0, false), "Pressed.lookLeft");
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0, false), "Pressed.lookRight");
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0, true), "Release.lookLeft");
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0, true), "Release.lookRight");

            PlayerDirectionAction.Observer playerDirectionObserver = new PlayerDirectionAction.Observer() {
                @Override
                public void playerDidLookInDirection(PlayerDirectionMovement direction) {
                    playerDirectionMovement = direction;
                }
            };

            am.put("Pressed.lookLeft", new PlayerDirectionAction(PlayerDirectionMovement.LEFT, playerDirectionObserver));
            am.put("Pressed.lookRight", new PlayerDirectionAction(PlayerDirectionMovement.RIGHT, playerDirectionObserver));
            am.put("Release.lookLeft", new PlayerDirectionAction(null, playerDirectionObserver));
            am.put("Release.lookRight", new PlayerDirectionAction(null, playerDirectionObserver));
        }

        protected void makeBlocks() {
            Random rnd = new Random();

            int height = getPreferredSize().height;
            int rowCount = (height / 100) - 1;

            int[][] rowRanges = new int[rowCount][2];
            int yPos = 50;
            for (int row = 0; row < rowCount; row++) {
                rowRanges[row] = new int[]{yPos, yPos + 50};
                yPos += 100;
            }

            int width = getPreferredSize().width;

            for (int row = 0; row < rowRanges.length; row++) {
                for (int col = 0; col < 2; col++) {
                    int x = rnd.nextInt(width / 4) + ((width / 2) * col);
                    int y = rowRanges[row][0];
                    entities.add(new FixedEntity(new Rectangle2D.Double(x, y, 50, 50)));
                }
            }
        }

        protected void makeEnemies() {
            Random rnd = new Random();

            int height = getPreferredSize().height;
            int rowCount = (height / 100) - 1;
            int[][] rowRanges = new int[rowCount][2];
            int yPos = 0;
            for (int row = 0; row < rowCount; row++) {
                rowRanges[row] = new int[]{yPos, yPos + 50};
                yPos += 100;
            }

            int width = getPreferredSize().width;

            Dimension size = new Dimension(5, 5);
            int targetEnimies = 15000;
            int entitiesPerRow = targetEnimies / rowRanges.length;
            for (int row = 0; row < rowRanges.length; row++) {
                for (int index = 0; index < entitiesPerRow; index++) {
                    int x = rnd.nextInt(width - size.width);
                    int y = rowRanges[row][0] + rnd.nextInt(rowRanges[row][1] - rowRanges[row][0] - size.height);
                    FixedRateEnemyEnity enity = new FixedRateEnemyEnity(new Point2D.Double(x, y), size, rnd.nextBoolean() ? FixedRateEnemyEnity.Direction.LEFT : FixedRateEnemyEnity.Direction.RIGHT);
                    entities.add(enity);
                }
            }
        }

        protected void updatePlayerLookDirection() {
            if (playerDirectionMovement == null) {
                return;
            }

            if (playerDirectionMovement == PlayerDirectionMovement.LEFT) {
                player.adjustAngleOfViewBy(-0.5);
            }
            if (playerDirectionMovement == PlayerDirectionMovement.RIGHT) {
                player.adjustAngleOfViewBy(0.5);
            }
        }

        protected void updateEnemies() {
            for (Entity entity : entities) {
                if (entity instanceof EnimeyEnity) {
                    ((EnimeyEnity) entity).update(getSize());
                }
            }
        }

        protected void updateCanSee() {
            sightedEntities.clear();
            for (Entity enity : entities) {
                Rectangle2D bounds = enity.getBounds();
                if (lineOfSight.intersects(bounds)) {
                    sightedEntities.add(enity);
                }
            }

            if (sightedEntities.isEmpty()) {
                return;
            }
            Collections.sort(sightedEntities, new Comparator<Entity>() {
                @Override
                public int compare(Entity lhs, Entity rhs) {
                    Point2D p1 = new Point2D.Double(lhs.getBounds().getCenterX(), lhs.getBounds().getCenterY());
                    Point2D p2 = new Point2D.Double(rhs.getBounds().getCenterX(), rhs.getBounds().getCenterY());
                    double d1 = p1.distance(playerPoint);
                    double d2 = p2.distance(playerPoint);
                    if ((int) Math.ceil(d1) == (int) Math.ceil(d2)) {
                        return 0;
                    } else if (d1 < d2) {
                        return -1;
                    } else {
                        return 1;
                    }
                }
            });

            int matchIndex = -1;
            for (int index = 0; index < sightedEntities.size(); index++) {
                if (sightedEntities.get(index) instanceof FixedEntity) {
                    matchIndex = index;
                    break;
                }
            }
            if (matchIndex == -1) {
                return;
            }
            // Trim off the remaining entities, as they aren't useful
            sightedEntities = sightedEntities.subList(0, matchIndex + 1);
        }

        protected Point2D getPointOnCircle(double degress, double radius) {
            double rads = Math.toRadians(degress - 90); // 0 becomes the top

            // Calculate the outter point of the line
            double xPosy = Math.cos(rads) * radius;
            double yPosy = Math.sin(rads) * radius;

            return new Point2D.Double(xPosy, yPosy);
        }

        private Instant lastPaintCycle;
        private int lastCycleFrameCount = 0;
        private int fps = -1;

        private Shape hitEnemyHighlight = new Ellipse2D.Double(0, 0, 10, 10);
        private Shape hitFixedHighlight = new Rectangle2D.Double(0, 0, 50, 50);

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (lastPaintCycle == null) {
                lastPaintCycle = Instant.now();
            }

            Graphics2D g2d = (Graphics2D) g.create();

            for (Entity entity : unsightedEntities) {
                entity.paint(g2d);
            }

            for (Entity entity : sightedEntities) {
                double x = entity.getBounds().getCenterX();
                double y = entity.getBounds().getCenterY();

                Graphics2D g2 = (Graphics2D) g2d.create();
                g2.setColor(Color.RED);
                if (entity instanceof FixedEntity) {
                    g2.translate(x - 25, y - 25);
                    g2.fill(hitFixedHighlight);
                } else {
                    g2.translate(x - 5, y - 5);
                    g2.fill(hitEnemyHighlight);
                }
                g2.dispose();
            }

            player.paint(g2d, playerPoint);
            g2d.setColor(Color.RED);
            g2d.draw(lineOfSight);

            Duration duration = Duration.between(lastPaintCycle, Instant.now());
            if (duration.toMillis() >= 1000) {
                lastPaintCycle = Instant.now();
                fps = lastCycleFrameCount;
                lastCycleFrameCount = 0;
            } else {
                lastCycleFrameCount++;
            }

            g2d.setColor(Color.WHITE);
            FontMetrics fm = g2d.getFontMetrics();
            String text = fps < 0 ? "---" : Integer.toString(fps);
            int x = getWidth() - fm.stringWidth(text) - 32;
            int y = getHeight() - (fm.getHeight() * 2) - 32;
            g2d.drawString(text, x, y + fm.getAscent());

            text = Integer.toString(sightedEntities.size());
            y = getHeight() - fm.getHeight() - 32;
            g2d.drawString(text, x, y + fm.getAscent());

            g2d.dispose();
        }
    }

    public class QuickEllipse extends Ellipse2D.Double {
        public QuickEllipse(Rectangle2D bounds) {
            super(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
        }
    }

    public class PlayerDirectionAction extends AbstractAction {
        public interface Observer {
            public void playerDidLookInDirection(PlayerDirectionMovement direction);
        }

        private Observer observer;
        private PlayerDirectionMovement direction;

        public PlayerDirectionAction(PlayerDirectionMovement direction, Observer observer) {
            this.direction = direction;
            this.observer = observer;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            getObserver().playerDidLookInDirection(getDirection());
        }

        public PlayerDirectionMovement getDirection() {
            return direction;
        }

        public Observer getObserver() {
            return observer;
        }
    }

    public class Player {
        private double angleOfView = 0;
        private Shape shape = new Ellipse2D.Double(0, 0, 10, 10);

        private static final double MAX_VIEW_ANGLE = 45.0;
        private static final double MIN_VIEW_ANGLE = -45.0;

        public double getAngleOfView() {
            return angleOfView;
        }

        public Dimension getSize() {
            return shape.getBounds().getSize();
        }

        public void adjustAngleOfViewBy(double degrees) {
            angleOfView += degrees;

            if (angleOfView < MIN_VIEW_ANGLE) {
                angleOfView = MIN_VIEW_ANGLE;
            } else if (angleOfView > MAX_VIEW_ANGLE) {
                angleOfView = MAX_VIEW_ANGLE;
            }
        }

        public void paint(Graphics2D g2d, Point location) {
            g2d = (Graphics2D) g2d.create();
            g2d.translate(location.x - (getSize().width / 2), location.y - (getSize().height / 2));
            g2d.setColor(Color.BLUE);
            g2d.fill(shape);
            g2d.dispose();
        }
    }

    public interface Entity {
        public Rectangle2D getBounds();

        public void paint(Graphics2D g2d);
    }

    public interface EnimeyEnity extends Entity {
        public void update(Dimension viewableArea);
    }

    public class FixedEntity implements Entity {

        private Shape shape;

        public FixedEntity(Rectangle2D bounds) {
            shape = new Ellipse2D.Double(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
        }

        @Override
        public Rectangle2D getBounds() {
            return shape.getBounds2D();
        }

        @Override
        public void paint(Graphics2D g2d) {
            g2d = (Graphics2D) g2d.create();
            g2d.setColor(Color.MAGENTA);
            g2d.fill(shape);
            g2d.dispose();
        }
    }

    public class FixedRateEnemyEnity implements EnimeyEnity {

        enum Direction {
            LEFT, RIGHT;

            public Direction toggle() {
                switch (this) {
                    case LEFT:
                        return RIGHT;
                    case RIGHT:
                        return LEFT;
                }

                return LEFT;
            }
        }

        private Point2D location;
        private Dimension size;
        private Direction direction;

        private Shape shape;

        public FixedRateEnemyEnity(Point2D location, Dimension size, Direction initialDirection) {
            this.location = location;
            this.size = size;
            this.direction = initialDirection;

            this.shape = new Rectangle2D.Double(0, 0, size.getWidth(), size.getHeight());
        }

        @Override
        public void update(Dimension viewableArea) {
            double delta = 1;
            if (direction == Direction.LEFT) {
                delta *= -1;
            }

            double x = location.getX() + delta;
            if (x + size.getWidth() > viewableArea.width) {
                x = viewableArea.width - size.getWidth();
                direction = direction.toggle();
            } else if (x < 0) {
                x = 0;
                direction = direction.toggle();
            }

            location.setLocation(x, location.getY());
        }

        @Override
        public Rectangle2D getBounds() {
            return new Rectangle2D.Double(location.getX(), location.getY(), size.width, size.height);
        }

        @Override
        public void paint(Graphics2D g2d) {
            g2d = (Graphics2D) g2d.create();
            g2d.setColor(Color.GREEN);
            g2d.translate(location.getX(), location.getY());
            g2d.fill(shape);
            g2d.dispose();
        }

    }
}
