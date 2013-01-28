import model.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import static java.lang.StrictMath.PI;
import static java.lang.StrictMath.atan2;

public final class MyStrategy implements Strategy {
    private static final double MAX_DISTANCE_TO_BONUS = 600;
    private static final int EVADE_COOLDOWN = 50;
    private static final int EVADE_TIME = 100;
    private static final double SHELL_SPEED_NEAR = 13.25;
    private static final double SHELL_SPEED_MID = 13.25;
    private static final double SHELL_SPEED_FAR = 13.25;
    private static final double TURRET_LENGTH = 86D;
    private static final double ONE_DEGREE = PI / 180;
    public static final int CLOSE_TO_BORDER = 90;
    public static final double MATRIX_DENSITY = 25d;

    double minAngle = ONE_DEGREE / 2;
    double angleToTarget;
    static Long mainTarget;
    Long secondaryTarget;
    Unit pickUpTarget;
    static volatile ArrayList<Long> moveTargets = new ArrayList<Long>();
    Tank self;
    World world;
    Move move;
    private double lastPosX;
    private double lastPosY;
    private boolean stuck = false;
    private int stuckTick;
    HashMap<Long, Tank> tanks;

    private int lastTimeEvade;
    private ArrayList<Unit> obstacles;
    private static String maneuverArea;

    MyStrategy() {

    }

    @Override
    public void move(Tank self, World world, Move move) {
        move.setFireType(FireType.NONE);
        this.self = self;
        this.world = world;
        this.move = move;
        initCollections();
        move();
        // testShellPhysic();
        // testCoordinates();
        //testCoordinates2();
        // positionPrediction();
    }

    private void testCoordinates2() {
        Tank tank = tanks.get(mainTarget);
        double y = tank.getY();
        double x = tank.getX();
        System.out.println("tank x y: " + tank.getX() + " " + y);
        double radius = Math.sqrt(tank.getWidth() * tank.getWidth() + tank.getHeight() * tank.getHeight()) / 2;
        System.out.println("tank radius: " + radius);
        double angleToX1 = Math.asin(tank.getHeight() / tank.getWidth());
        System.out.println("tank angle 1: " + angleToX1 / ONE_DEGREE);
        double sumAngle = angleToX1 + tank.getAngle();

        double tanSquare = Math.pow(Math.tan(sumAngle), 2);
        double x1 = (-4 * y * Math.tan(sumAngle) + Math.sqrt(tanSquare * (1 + y * y) - y * y * radius * radius)) / (2 * (radius * radius - tanSquare)) + x;
        double x2 = (-4 * y * Math.tan(sumAngle) - Math.sqrt(tanSquare * (1 + y * y) - y * y * radius * radius)) / (2 * (radius * radius - tanSquare)) + x;
        System.out.println(" x1, x2 : " + x1 + " " + x2);
        System.out.println();
    }

    private void testCoordinates() {
        Tank tank = tanks.get(mainTarget);
        //  log("[testCoordinates] self x y " + tank.getX() + " " + tank.getY());
        //   log("[testCoordinates] angle " + tank.getAngle() / ONE_DEGREE);
        /*
        * 1 - - 4
        * |     |
        * 2 - - 3*/


        double x = tank.getX();
        double y = tank.getY();
        double h = tank.getHeight();
        double w = tank.getWidth();
        double x1 = x - w / 2;
        double x2 = x - w / 2;
        double x3 = x + w / 2;
        double x4 = x + w / 2;
        double y1 = y - h / 2;
        double y2 = y + h / 2;
        double y3 = y + h / 2;
        double y4 = y - h / 2;
        //TODO сделать циклы коллекции преферанс
  /*
        Position pos1 = getCornerCurrentPosition(tank, x1, y1);
        Position pos2 = getCornerCurrentPosition(tank, x2, y2);
        Position pos3 = getCornerCurrentPosition(tank, x3, y3);
        Position pos4 = getCornerCurrentPosition(tank, x4, y4);
       */
        Position pos1 = getTankPosition(tank, x1, y1);
        Position pos2 = getTankPosition(tank, x2, y2);
        Position pos3 = getTankPosition(tank, x3, y3);
        Position pos4 = getTankPosition(tank, x4, y4);
        // System.out.println("***");
       /*
        for (int i = -360; i < 360; i++) {
            System.out.println("i: = " + i + " pos: " + getDotPosition(tank, x1, y1, i));

        }                  */
        /*
        log("[testCoordinates] SELF x y  " + x + " " + y);
        log("[testCoordinates] STANDART x1 y1  " + x1 + " " + y1);
        log("[testCoordinates] CURRENT: " + getTankPosition(tank, x1, y1));
        log("***");
        */
              /*
        log("[testCoordinates] SELF x y  " + x + " " + y);
        log("[testCoordinates] STANDART x1 y1  " + x1 + " " + y1);
        log("[testCoordinates] CURRENT  x1 y1  " + (pos1.getX()) + " " + (pos1.getY()));
        log("[testCoordinates]  - - -");
        log("[testCoordinates] STANDART x2 y2  " + x2 + " " + y2);
        log("[testCoordinates] CURRENT  x2 y2  " + (pos2.getX()) + " " + (pos2.getY()));
        log("[testCoordinates]  - - -");
        log("[testCoordinates] STANDART x3 y3  " + x3 + " " + y3);
        log("[testCoordinates] CURRENT  x3 y3  " + (pos3.getX()) + " " + (pos3.getY()));
        log("[testCoordinates]  - - -");

        log("[testCoordinates] STANDART x4 y4  " + x4 + " " + y4);
        log("[testCoordinates] CURRENT  x4 y4  " + (pos4.getX()) + " " + (pos4.getY()));
                                                                          */
        // epick
        printPostionMatrix(pos1, pos2, pos3, pos4, new Position(x, y));

        // log("[testCoordinates] distanceToTopRightCorner " + distanceToCurrentCorner); // 54
       /* log("[testCoordinates] tank.getAngleTo(x, y) " + tank.getAngleTo(x, y)/ONE_DEGREE);
        log("[testCoordinates] angleToCurrentCorner " + angleToCurrentCorner);        // 33.69

*/
        // log("[testCoordinates]  ");
    }

    private void printPostionMatrix(Position pos1, Position pos2, Position pos3, Position pos4, Position centr) {
        // System.out.println();
        /*
        log("Pos: " + getMatrixPos(pos1));
        log("Pos: " + getMatrixPos(pos2));
        log("Pos: " + getMatrixPos(pos3));
        log("Pos: " + getMatrixPos(pos4));
        log("centr: " + getMatrixPos(centr));
                           */
        for (int yi = 0; yi < Math.floor(world.getHeight() / MATRIX_DENSITY); yi++) {
            for (int xi = 0; xi < Math.floor(world.getWidth() / MATRIX_DENSITY); xi++) {
                if (yi >= floorPosY(centr) + 10) {
                    break;
                }
                if (yi <= floorPosY(centr) - 10) {
                    continue;
                }
                if (xi <= floorPosX(centr) - 10 || xi >= floorPosX(centr) + 15  /*
                       || xi > floorPosX(centr) + 10 || yi > floorPosY(centr) + 10*/) {
                    continue;
                }
                if (matrixCheckPosition(pos1, yi, xi)) {
                    System.out.print("A ");
                } else if (matrixCheckPosition(pos2, yi, xi)) {
                    System.out.print("B ");
                } else if (matrixCheckPosition(pos3, yi, xi)) {
                    System.out.print("C ");
                } else if (matrixCheckPosition(pos4, yi, xi)) {
                    System.out.print("D ");
                }
                if (matrixCheckPosition(centr, yi, xi)) {
                    System.out.print("@ ");
                } else {
                    System.out.print("  ");
                }
            }

            System.out.println();
        }

        //   System.out.println();
        //    System.out.println();
    }

    private String getMatrixPos(Position pos1) {
        return floorPosX(pos1) + " " + floorPosY(pos1);
    }

    private double floorPosY(Position pos1) {
        return Math.floor(pos1.getY() / MATRIX_DENSITY);
    }

    private double floorPosX(Position pos1) {
        return Math.floor(pos1.getX() / MATRIX_DENSITY);
    }

    private boolean matrixCheckPosition(Position pos1, double yi, double xi) {
        return xi == floorPosX(pos1) && yi == floorPosY(pos1);
    }

    private Position getCornerCurrentPosition(Tank tank, double oldX, double oldY) {

        double distanceToCorner = tank.getDistanceTo(oldX, oldY);
        double angleToCornerStatic = getAngle(tank.getX(), tank.getY(), oldX, oldY);
        double tankX = tank.getX();
        double tankY = tank.getY();
        double angleToCurrentCorner = (angleToCornerStatic / ONE_DEGREE + tank.getAngle() / ONE_DEGREE) * ONE_DEGREE;
        /*double currentX = Math.cos(angleToCurrentCorner) * 100 + distanceToCorner;
        double currentY = Math.sin(angleToCurrentCorner) * 100 + distanceToCorner;     */
        //  double currentX = distanceToCorner * Math.sin(angleToCurrentCorner*ONE_DEGREE);

        //   return new Position(currentX + tank.getX(), currentY + tank.getY());
        double rx = oldX - tankX;
        double ry = oldY - tankY;
        double alpha = angleToCurrentCorner - angleToCornerStatic;
        double c = Math.cos(alpha);
        double s = Math.sin(alpha);
        double currentX = tankX + rx * c - ry * s;
        double altCurrentY = tankY + rx * s - ry * c;

        double currentY = Math.sin(angleToCurrentCorner) * 100 + distanceToCorner;

        return new Position(currentX, (/*alpha > 0*/true ? currentY + oldY : altCurrentY));
    }

    private Position getDotPosition(Tank tank, double oldX, double oldY, int angle) {

        double distanceToCorner = tank.getDistanceTo(oldX, oldY);
        double angleToCornerStatic = getAngle(tank.getX(), tank.getY(), oldX, oldY);
        double angleToCurrentCorner = (angleToCornerStatic / ONE_DEGREE + tank.getAngle() / ONE_DEGREE) * ONE_DEGREE;
        double tankX = tank.getX();
        double tankY = tank.getY();
        double currentAngle = angle * ONE_DEGREE;
        /*double currentX = Math.cos(angleToCurrentCorner) * 100 + distanceToCorner;
        double currentY = Math.sin(angleToCurrentCorner) * 100 + distanceToCorner;     */
        //  double currentX = distanceToCorner * Math.sin(angleToCurrentCorner*ONE_DEGREE);

        //   return new Position(currentX + tank.getX(), currentY + tank.getY());
                                        /*
        double rx = oldX - tankX;
        double ry = oldY - tankY;
        double alpha = currentAngle;
        double c = Math.cos(alpha);
        double s = Math.sin(alpha);   */
        // double currentX = tankX + rx * c - ry * s;
        // double currentY = tankY + rx * s - ry * c;
        /*      WORK
        double currentX = Math.round (Math.cos(currentAngle) * distanceToCorner);
        double currentY =Math.round(Math.sin(currentAngle) * distanceToCorner);
          */
        double currentX = Math.round(Math.cos(angleToCurrentCorner) * distanceToCorner);
        double currentY = Math.round(Math.sin(angleToCurrentCorner) * distanceToCorner);
        return new Position(currentX, currentY);
    }

    private Position getTankPosition(Unit unit, double oldX, double oldY) {

        double distanceToCorner = unit.getDistanceTo(oldX, oldY);
        double angleToCornerStatic = getAngle(unit.getX(), unit.getY(), oldX, oldY);
        double angleToCurrentCorner = (angleToCornerStatic / ONE_DEGREE + unit.getAngle() / ONE_DEGREE) * ONE_DEGREE;
        double tankX = unit.getX();
        double tankY = unit.getY();
        //  double currentAngle = angle * ONE_DEGREE;
        /*double currentX = Math.cos(angleToCurrentCorner) * 100 + distanceToCorner;
        double currentY = Math.sin(angleToCurrentCorner) * 100 + distanceToCorner;     */
        //  double currentX = distanceToCorner * Math.sin(angleToCurrentCorner*ONE_DEGREE);

        //   return new Position(currentX + unit.getX(), currentY + unit.getY());
                                        /*
        double rx = oldX - tankX;
        double ry = oldY - tankY;
        double alpha = currentAngle;
        double c = Math.cos(alpha);
        double s = Math.sin(alpha);   */
        // double currentX = tankX + rx * c - ry * s;
        // double currentY = tankY + rx * s - ry * c;
        /*      WORK
        double currentX = Math.round (Math.cos(currentAngle) * distanceToCorner);
        double currentY =Math.round(Math.sin(currentAngle) * distanceToCorner);
          */
        double currentX = Math.round(Math.cos(angleToCurrentCorner) * distanceToCorner);
        double currentY = Math.round(Math.sin(angleToCurrentCorner) * distanceToCorner);
        return new Position(tankX + currentX, currentY + tankY);
    }

    private double getAngleToCurrentCorner(Tank tank, double x, double y) {
        return tank.getAngleTo(x, y) + tank.getAngle();
    }

    public double getAngle(double centralX, double centralY, double cornerX, double cornerY) {
        double absoluteAngleTo = atan2(cornerY - centralY, cornerX - centralX);
        double relativeAngleTo = absoluteAngleTo;

        while (relativeAngleTo > PI) {
            relativeAngleTo -= 2.0D * PI;
        }

        while (relativeAngleTo < -PI) {
            relativeAngleTo += 2.0D * PI;
        }

        return relativeAngleTo;
    }

    private void positionPrediction() {

        Position pos = getPredictPosition(mainTarget);
        if (self.getRemainingReloadingTime() >= self.getReloadingTime() - 1) {
            log("[positionPrediction] SHOT");
        }/*
        log("[positionPrediction] distance " + distanceToTarget + " tick elapsed: " + elapsedTimeToContact);
        log("[positionPrediction] time contact: " + (world.getTick() + elapsedTimeToContact));


        log("[positionPrediction]! Predicted tankPosition:! x:" + x + " y:" +
                y);
        log("[positionPrediction] tank current pos x:" + target.getX() + " y:" + target.getY());
        log("[positionPrediction] current tick: " + world.getTick());
        */
    }

    private Position getPredictPosition(Long targetId) {
        Tank target = tanks.get(targetId);
        double distanceToTarget = self.getDistanceTo(target);

        double elapsedTimeToContact = (distanceToTarget - TURRET_LENGTH) / SHELL_SPEED_NEAR;
        double predictX = target.getX() + target.getSpeedX() * elapsedTimeToContact;
        double predictY = target.getY() + target.getSpeedY() * elapsedTimeToContact;
        return new Position(predictX, predictY);
    }

    private void testShellPhysic() {
        if (self.getRemainingReloadingTime() >= self.getReloadingTime() - 1 || false) {
            for (Shell shell : world.getShells()) {
                if (shell.getPlayerName().equals(self.getPlayerName())) {
                    Tank target = tanks.get(mainTarget);
                    double elapsedTicks = (target.getX() - shell.getX()) / shell.getSpeedX();
                    log("[shellSpeedTesting] id: " + shell.getId() + " speedX speedY " + shell.getSpeedX() + " " + shell.getSpeedY());
                 /*   log("[shellSpeedTesting] tick elapsed: " + elapsedTicks + ", tick contact: " + (world.getTick() + elapsedTicks));
                    log("[shellSpeedTesting] absolute shell speed: " + shell.getDistanceTo(target) / elapsedTicks);
                    log("[shellSpeedTesting] current tick: " + world.getTick());
                    log("[shellSpeedTesting] current SHELL distaste: " + shell.getDistanceTo(target));
                    log("[shellSpeedTesting] current SELF distaste: " + self.getDistanceTo(target));
                    log("[shellSpeedTesting] turret lengths: " + (self.getDistanceTo(target) - shell.getDistanceTo(target)));
                       */
                    log("[shellSpeedTesting] w h" + shell.getWidth() + " " + shell.getHeight());
                    log("[shellSpeedTesting] *********************************");
                    log("[shellSpeedTesting] *********************************");
                    log("[shellSpeedTesting] *********************************");
                    log("[shellSpeedTesting] *********************************");
                    log("[shellSpeedTesting] *********************************");
                    log("[shellSpeedTesting] *********************************");
                    log("[shellSpeedTesting] *********************************");
                }
            }
        }
    }

    private void initCollections() {
        tanks = new HashMap<Long, Tank>();
        for (Tank tank : world.getTanks()) {
            tanks.put(tank.getId(), tank);
        }
        obstacles = new ArrayList<Unit>();
        for (Unit unit : world.getObstacles()) {
            obstacles.add(unit);
        }
        for (Tank tank : world.getTanks()) {
            if ((tank.isTeammate() || !isAlive(tank)) && self.getId() != tank.getId()) {
                obstacles.add(tank);
            }
        }
        for (Bonus bonus : world.getBonuses()) {
            obstacles.add(bonus);
        }
    }

    private void move() {

        shootEnemy();
        // maneuver();
        if (!pickUpBonus()) {
            maneuver();
        }
    }

    private void maneuver() {
        //log("[maneuver] do maneuver");
        if (maneuverArea == null) {
            if (self.getX() > world.getWidth() / 2) {
                maneuverArea = "RIGHT_SIDE";
            } else {
                maneuverArea = "LEFT_SIDE";
            }
        }

        double pointX;
        if (maneuverArea.equals("RIGHT_SIDE")) {
            pointX = world.getWidth() - 100;
        } else {
            pointX = 100;
        }
        double pointY = world.getHeight() / (self.getTeammateIndex() + 1.3);
        if (self.getDistanceTo(pointX, pointY) > 250) {
            log("[maneuver] move to : " + pointX + " " + pointY);
            moveTo(pointX, pointY);
        } else {
            turnSideTo(mainTarget);
            //turnTo(mainTarget);
        }
    }

    private void turnSideTo(Long mainTarget) {
        if (evade()) {

            return;
        }
        if (mainTarget == null) {
            return;
        }
        double angleToUnit = self.getAngleTo(tanks.get(mainTarget)) / ONE_DEGREE;
        log("");
        log("90  - angleToUnit : " + (90 - angleToUnit));
        log(" angleToUnit : " + angleToUnit);
        if (angleToUnit > 0) {
            if (Math.abs(90 - angleToUnit) > 10) {
                if (angleToUnit > 90) {
                    turnRight();
                    log("TURN TO RIGHT " + angleToUnit);
                } else {
                    turnLeft();
                    log("TURN TO LEFT " + angleToUnit);
                }
            } else {
                stopMove();
            }
        } else {
            if (Math.abs(90 + angleToUnit) > 10) {
                if (angleToUnit > -90) {
                    turnLeft();
                    log("TURN TO RIGHT " + angleToUnit);
                } else {
                    turnRight();
                    log("TURN TO LEFT " + angleToUnit);
                }
            } else {
                stopMove();
            }
        }
    }

    private void stopMove() {
        move.setLeftTrackPower(0D);
        move.setRightTrackPower(0D);
    }

    private void turnRight() {
        move.setLeftTrackPower(0.8D);
        move.setRightTrackPower(-1D);
    }

    private void turnTo(Long mainTarget) {
        if (mainTarget == null) {
            return;
        }
        double angleToUnit = self.getAngleTo(tanks.get(mainTarget)) / ONE_DEGREE;
        if (Math.abs(angleToUnit) > minAngle) {
            if (angleToUnit < 0) {
                turnLeft();
                log("TURN TO LEFT " + angleToUnit);
            } else {
                turnRight();
                log("TURN TO RIGHT" + angleToUnit);
            }
        }
    }

    private void turnLeft() {
        move.setLeftTrackPower(-1D);
        move.setRightTrackPower(0.8D);
    }

    private void shootEnemy() {
        if ((mainTarget != null && !isAlive(tanks.get(mainTarget))) || mainTarget == null) {
            mainTarget = findNewTarget();
        }
        //log("[shootEnemy] mainTarget: " + mainTarget);
        if (mainTarget != null && shootTank(tanks.get(mainTarget))) {
            return;
        }
        //   log("[shootEnemy] dont shoot mainTarget! have Obstacles!");
        if (secondaryTarget == null || !isAlive(tanks.get(secondaryTarget))) {
            secondaryTarget = findNewTarget();
        }
        if (!shootTank(tanks.get(secondaryTarget))) {
            turnTurret(mainTarget);
        }

           /*

        for (Tank tank : tanks.values())
            if (!tank.isTeammate() && (tank.getCrewHealth() != 0 && tank.getHullDurability() != 0) && (mainTarget == null || ((Long) tank.getId()).equals(mainTarget))) {
                if (tank.getPlayerName().equals(targetName) || targetIsDead()) {
                    angleToTarget = self.getTurretAngleTo(tank);
                    move.setTurretTurn(angleToTarget);
                    if (Math.abs(angleToTarget) > minAngle) {
                        move.setFireType(FireType.NONE);
                    } else {
                        mainTarget = tank.getId();
                        if (checkObstacles(tank)) {
                            move.setFireType(FireType.PREMIUM_PREFERRED);
                        } else {
                            move.setFireType(FireType.NONE);
                        }
                    }
                }
            }
            */
    }

    private void turnTurret(Long mainTarget) {
        if (mainTarget == null) return;
        move.setTurretTurn(self.getTurretAngleTo(tanks.get(mainTarget)));
    }

    /*
   private boolean checkObstacles(Unit unit) {
       double distance = self.getDistanceTo(unit);
       for (Tank tank : tanks.values()) {
           if ((tank.isTeammate() || tank.getCrewHealth() == 0 || tank.getHullDurability() == 0) && self.getDistanceTo(tank) < distance && Math.abs(self.getTurretAngleTo(tank)) < minAngle * 4) {
               // log("obstacle on fire line");
               return false;
           }
           for (Obstacle obstacle : world.getObstacles()) {
               if (checkObstacle(obstacle, null, null)) {
                   return false;
               }
               ;
           }
            /*
           for (Bonus bonus : world.getBonuses()) {
               if (checkObstacle(bonus)) {
                   return false;
               };
           } */
    /*    }
        return true;
    }
     */
    private boolean checkObstacle(Unit unit, double targetX, double targetY) {
    /*
    1   4
    2   3
    * */

        double px1 = self.getX();
        double py1 = self.getY();
        double px2 = targetX;
        double py2 = targetY;
        Position pos1 = new Position();
        Position pos2 = new Position();
        Position pos3 = new Position();
        Position pos4 = new Position();

        getUnitCorner(unit, pos1, pos2, pos3, pos4);
        /*
        if (unit instanceof Obstacle){
            printCheckObstaclesDebugMessages(unit, px1, py1, px2, py2, x, y, h, w, x1, x2, x3, x4, y1, y2, y3, y4, pos1, pos2, pos3, pos4);
        }
                    */
        //log(" self - " + px1 + " " + py1 + "   target - " + px2 + " " + py2);
        //  return checkSquare(x1, x2, x3, x4, y1, y2, y3, y4, px1, py1, px2, py2);

        return checkSquare(pos1.getX(), pos2.getX(), pos3.getX(), pos4.getX(), pos1.getY(), pos2.getY(), pos3.getY(), pos4.getY(), px1, py1, px2, py2);
    }

    private void getUnitCorner(Unit unit, Position pos1, Position pos2, Position pos3, Position pos4) {
        double x = unit.getX();
        double y = unit.getY();
        double h = unit.getHeight();
        double w = unit.getWidth();
        double x1 = x - w / 2;
        double x2 = x - w / 2;
        double x3 = x + w / 2;
        double x4 = x + w / 2;
        double y1 = y - h / 2;
        double y2 = y + h / 2;
        double y3 = y + h / 2;
        double y4 = y - h / 2;
        pos1.setCoordinates(getTankPosition(unit, x1, y1));
        pos2.setCoordinates(getTankPosition(unit, x2, y2));
        pos3.setCoordinates(getTankPosition(unit, x3, y3));
        pos4.setCoordinates(getTankPosition(unit, x4, y4));
        // log("[getUnitCorner] + "  + pos1.getX());
    }

    private void printCheckObstaclesDebugMessages(Unit unit, double px1, double py1, double px2, double py2, double x, double y, double h, double w, double x1, double x2, double x3, double x4, double y1, double y2, double y3, double y4, Position pos1, Position pos2, Position pos3, Position pos4) {
        log("unit " + unit.getClass());
        log("RESULT: " + checkSquare(pos1.getX(), pos2.getX(), pos3.getX(), pos4.getX(), pos1.getY(), pos2.getY(), pos3.getY(), pos4.getY(), px1, py1, px2, py2));
        log("unit x: " + x + " y " + y + " h w " + h + " " + w);
        /*
        log("old " + x1 + " " + y1);
        log("old " + x2 + " " + y2);
        log("old " + x3 + " " + y3);
        log("old " + x4 + " " + y4);
        log("++++ ");
              */
        log("p1 " + pos1.getX() + " " + pos1.getY());
        log("p2 " + pos2.getX() + " " + pos2.getY());
        log("p3 " + pos3.getX() + " " + pos3.getY());
        log("p4 " + pos4.getX() + " " + pos4.getY());
        System.out.println();
        //printPostionMatrix(pos1, pos2, pos3, pos4, new Position(x, y));
    }

    private boolean shootTank(Tank tank) {
        if (tank == null) {
            return false;
        }
        //TODO исправить ошибку с тем что мы целимся не туда куда стрелям, для этого нужно отрефакторить методы проверки препятствий для поддрежки координат
        Position predictPos = getPredictPosition(tank);
        if (checkObstacle(obstacles, predictPos)) {
            //  log("[shootTank]SHOOT CANCEL " + predictPos);
            move.setFireType(FireType.NONE);
            //    log("[shootEnemy] OBSTACLES set NONE " + checkObstacle(obstacles, predictPos));
            return false;
        }
        //  log("[shootTank] TARGET CLEAR TO SHOOT " + predictPos);
        // angleToTarget = self.getTurretAngleTo(tank);
        angleToTarget = self.getTurretAngleTo(predictPos.x, predictPos.y);
        move.setTurretTurn(angleToTarget);
        if (Math.abs(angleToTarget) > minAngle) {
            move.setFireType(FireType.NONE);
            //     log("[shootEnemy] set NONE CHECK OBSTACLES: " + checkObstacle(obstacles, predictPos));
        } else {
            move.setFireType(FireType.PREMIUM_PREFERRED);
            //       log("[shootEnemy] set PREMIUM_PREFERRED + CHECK OBSTACLES: " + checkObstacle(obstacles, predictPos));
            //  drawSnapshot(obstacles, predictPos, new Position(tank.getX(), tank.getY()));
        }
        return true;
    }

    private void drawSnapshot(ArrayList<Unit> obstacles, Position predictPos, Position currentPosition) {
        log("[drawSnapshot] " + predictPos);
        if (self.getRemainingReloadingTime() != 0 && self.getRemainingReloadingTime() + 3 < self.getReloadingTime()) {
            return;
        }

        BufferedImage snapshot = new BufferedImage((int) world.getWidth(), (int) world.getHeight(), BufferedImage.TYPE_INT_RGB);
        setBackgroundColor(snapshot, Color.gray);
        drawObstacles(obstacles, snapshot);
        drawDot(currentPosition, snapshot, Color.darkGray, 5);
        drawDot(predictPos, snapshot, Color.red, 3);
        drawDot(new Position(self.getX(), self.getY()), snapshot, Color.green, 5);
        String fileName = "shoots/" + world.getTick() + ".png";
        try {
            ImageIO.write(snapshot, "png", new File(fileName));
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Image save to " + fileName);
    }

    private void drawObstacles(ArrayList<Unit> obstacles, BufferedImage image) {
        for (Unit unit : obstacles) {
            Position pos1 = new Position();
            Position pos2 = new Position();
            Position pos3 = new Position();
            Position pos4 = new Position();
            getUnitCorner(unit, pos1, pos2, pos3, pos4);
            int cornerSize = 3;
            drawDot(pos1, image, Color.orange, cornerSize);
            drawDot(pos2, image, Color.orange, cornerSize);
            drawDot(pos3, image, Color.orange, cornerSize);
            drawDot(pos4, image, Color.orange, cornerSize);
            drawDot(new Position(unit.getX(), unit.getY()), image, Color.orange, 4);
        }
    }

    private void drawDot(Position position, BufferedImage image, Color color, int dotSize) {
        for (int x = -dotSize; x <= dotSize; x++) {
            for (int y = -dotSize; y <= dotSize; y++) {
                try {

                    image.setRGB((int) position.getX() + x, (int) position.getY() + y, color.getRGB());
                } catch (Exception e) {
                }
            }
        }
    }

    private void setBackgroundColor(BufferedImage image, Color color) {
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                image.setRGB(x, y, color.getRGB());
            }
        }
    }

    private Position getPredictPosition(Unit tank) {
        return getPredictPosition(tank.getId());
    }

    private boolean checkObstacle(ArrayList<Unit> units, Position position) {
        for (Unit unit : units) {
            if (checkObstacle(unit, position.getX(), position.getY())) {
                return true;
            }
        }
        return false;
    }

    private boolean checkObstacle(ArrayList<Unit> units, Unit target) {
        for (Unit unit : units) {
            if (checkObstacle(unit, target.getX(), target.getY())) {
                return true;
            }
        }
        return false;
    }

    private boolean isAlive(Tank tank) {
        if (tank.getCrewHealth() > 0 && tank.getHullDurability() > 0) {
            return true;
        }
        return false;
    }

    private Long findNewTarget() {
        Long tankId = null;
        for (Tank tank : tanks.values()) {
            if (!tank.isTeammate() && isAlive(tank) && !checkObstacle(obstacles, tank) && !tank.getPlayerName().equals("EmptyPlayer")) {
                if (tankId == null) {
                    tankId = tank.getId();
                } else {
                    if (Math.abs(self.getTurretAngleTo(tank)) < Math.abs(self.getTurretAngleTo(tanks.get(tankId)))) {
                        tankId = tank.getId();
                    }
                }
            }
        }
        return tankId;
    }

    private boolean pickUpBonus() {
        //  log("[pickUpBonus] distance to 0x0 " + self.getDistanceTo(0, 0) + " self position " + self.getX() + " " + self.getY());
        Bonus[] bonuses = world.getBonuses();
        double minDistanceToBonus = 2000;
        double minAngleToBonus = 1000;
        double minAngleToMedkit = 1000;
        double maxAngleToBonus = 0;
        double maxAngleToMedkit = 0;
        boolean bonusNoExists = true;

        for (Bonus bonus : bonuses) {
            if (pickUpTarget != null && bonus.getId() == pickUpTarget.getId()) {
                bonusNoExists = false;
            }
            double distance = self.getDistanceTo(bonus);
            double angle = Math.abs(self.getAngleTo(bonus));
            if (distance > MAX_DISTANCE_TO_BONUS) {
                continue;
            }
            if (distance < minDistanceToBonus) {
                minDistanceToBonus = distance;
            }
            if (!moveTargets.contains(bonus.getId())) {
                // log("Bonus claim teammate: " + bonus.getId());
            }
            if (angle < minAngleToBonus && !moveTargets.contains(bonus.getId()) &&
                    !(bonus.getType().equals(BonusType.REPAIR_KIT) && self.getHullDurability() == self.getHullMaxDurability())) {
                minAngleToBonus = angle;
            }

            if (angle > maxAngleToBonus && !moveTargets.contains(bonus.getId()) &&
                    !(bonus.getType().equals(BonusType.REPAIR_KIT) && self.getHullDurability() == self.getHullMaxDurability())) {
                maxAngleToBonus = angle;
            }

    /*     if (bonus.getType().equals(BonusType.MEDIKIT) && angle < minAngleToMedkit && self.getCrewHealth() != 100 && !moveTargets.contains(bonus.getId())) {
                minAngleToMedkit = angle;
            }
            if (bonus.getType().equals(BonusType.MEDIKIT) && angle > maxAngleToMedkit && self.getCrewHealth() != 100 && !moveTargets.contains(bonus.getId())) {
                maxAngleToMedkit = angle;
            }*/
        }
        if (bonusNoExists && pickUpTarget != null) {
            moveTargets.remove(pickUpTarget.getId());
            pickUpTarget = null;
        }

        boolean moved = false;
        for (Bonus bonus : bonuses) {
            double distance = self.getDistanceTo(bonus);
            double angle = Math.abs(self.getAngleTo(bonus));
            if (distance > MAX_DISTANCE_TO_BONUS) {
                //        log("[pickUpBonus] bonus + " + bonus.getType() + " " + bonus.getId() + " too far!");
                continue;
            }
            if (pickUpTarget != null) {
                moveTo(pickUpTarget);
                moved = true;
            } else if ((180 - maxAngleToBonus) > minAngleToBonus && angle == minAngleToBonus) {
                pickUpTarget = bonus;
                moveTargets.add(pickUpTarget.getId());
                moveTo(pickUpTarget);
                moved = true;
            } else if ((180 - maxAngleToBonus) < minAngleToBonus && angle == maxAngleToBonus) {
                pickUpTarget = bonus;
                moveTargets.add(pickUpTarget.getId());
                moveTo(pickUpTarget);
                moved = true;
            }
            //TODO Приоритет на подбор медкитов
                    /*(bonus.getType().equals(BonusType.MEDIKIT) && angle == minAngleToMedkit) {
                pickUpTarget = bonus;
                moveTargets.add(pickUpTarget.getId());
                moveTo(pickUpTarget);
                moved = true;
            } else if (angle == minAngleToBonus) {
                pickUpTarget = bonus;
                moveTargets.add(pickUpTarget.getId());
                moveTo(pickUpTarget);
                moved = true;
            }            */
        }
        return moved;
    }

    private boolean checkSquare(double x1, double x2, double x3, double x4, double y1, double y2, double y3, double y4, double px1, double py1, double px2, double py2) {
        if (segmentsIntersect(x1, y1, x2, y2, px1, py1, px2, py2) || segmentsIntersect(x2, y2, x3, y3, px1, py1, px2, py2) || segmentsIntersect(x3, y3, x4, y4, px1, py1, px2, py2) || segmentsIntersect(x4, y4, x1, y1, px1, py1, px2, py2)) {

            return true;
        }
        return false;
    }

    public boolean segmentsIntersect(double x1, double y1, double x2, double y2, double x3, double y3, double x4, double y4) {
        double d = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);
        if (d == 0) return false;
        double xi = Math.floor(((x3 - x4) * (x1 * y2 - y1 * x2) - (x1 - x2) * (x3 * y4 - y3 * x4)) / d);
        double yi = Math.floor(((y3 - y4) * (x1 * y2 - y1 * x2) - (y1 - y2) * (x3 * y4 - y3 * x4)) / d);
        if (xi < Math.min(x1, x2) - 15 || xi > Math.max(x1, x2) + 15) return false;
        if (xi < Math.min(x3, x4) - 15 || xi > Math.max(x3, x4) + 15) return false;
        if (yi < Math.min(y1, y2) - 15 || yi > Math.max(y1, y2) + 15) return false;
        if (yi < Math.min(y3, y4) - 15 || yi > Math.max(y3, y4) + 15) return false;
        /*Косяки с вычислениями в double*/
       /* log("[segmentsIntersect] OBSTACLE FIND!");
        log("[segmentsIntersect] x1 y1 x2 y2 x3 x4 : " + x1 + " " + y1 + " " + x2 + " " + y2 + " " + x3 + " " + y3 + " " + x4 + " " + y4);

        log("[segmentsIntersect]xi, yi : " + xi + " " + yi);
        */
        return true;
    }

    public double checkPoint(double x, double y, double x1, double x2, double y1, double y2) {
        return (y1 - y2) * x + (x1 - x2) * y + (x1 * y2 - x2 * y1);
    }

    private void log(String message) {
        //  System.out.println(world.getTick() + " " + self.getTeammateIndex() + " message:" + message);
    }

    private void moveBack() {
        move.setLeftTrackPower(-0.6D);
        move.setRightTrackPower(-0.6D);
    }

    private void moveTo(Unit unit) {
        moveTo(unit.getX(), unit.getY());
    }

    private void moveTo(double x, double y) {
        double angleToUnit = self.getAngleTo(x, y);
        // log("move to" + unit.getId() + " type:" + ((Bonus) unit).getType() + " distance: " + self.getDistanceTo(unit) + " pos x  y" + unit.getX() + " " + unit.getY());

        if (evade()) {
            return;
        }


        if (unstuck()) {
            return;
        }
        //  log("[moveTo] tank: " + self.getId() + "Angle to unit: " + ((Bonus) unit).getType() + " : " + angleToUnit);

        if (Math.abs(angleToUnit) > PI / 2 && !checkEvadeArea()) {
            //      log("[moveToBonus] hang back");
            //     log("[moveTo] minAngle * 170 > angle.. : " + minAngle * 170);
            if ((minAngle * 170) < Math.abs(angleToUnit)) {
                move.setLeftTrackPower(-1D);
                move.setRightTrackPower(-1D);
                //       log("[moveToBonus] FULL BACK");

            } else {
                if (angleToUnit > 0) {
                    move.setLeftTrackPower(0D);
                    move.setRightTrackPower(1D);
                    //            log("[moveToBonus] BACK LEFT");
                } else {
                    move.setLeftTrackPower(1D);
                    move.setRightTrackPower(0D);
                    //         log("[moveToBonus] BACK RIGHT");
                }
            }
        } else {
            //    log("[moveToBonus] move forward");
            if (minAngle * 10 > Math.abs(angleToUnit)) {
                move.setLeftTrackPower(1D);
                move.setLeftTrackPower(1D);
            } else {
                if (angleToUnit > 0) {
                    move.setLeftTrackPower(1D);
                    move.setRightTrackPower(0D);
                    //           log("[moveToBonus] RIGHT");
                } else {
                    move.setLeftTrackPower(0D);
                    move.setRightTrackPower(1D);
                    //          log("[moveToBonus] LEFT");
                }
            }
        }
    }

    private boolean evade() {
        if (world.getTick() - lastTimeEvade < EVADE_COOLDOWN && world.getTick() - lastTimeEvade > EVADE_TIME) {
            log("[evade] COOLDOWN");
            return false;
        }
        if (checkEvadeArea()) {
            return false;
        }
        for (Shell shell : world.getShells()) {
            if (Math.abs(shell.getAngleTo(self)) < minAngle * 2) {
                log("EVADE SHELL FAST FORWARD " + self.getId());
                doEvade();
                if (world.getTick() - lastTimeEvade > EVADE_TIME) {
                    lastTimeEvade = world.getTick();
                }
                unstuck();
                return true;
            }
        }

        for (Tank tank : world.getTanks()) {
            if (Math.abs(tank.getTurretAngleTo(self)) < minAngle * 2 && tank.getRemainingReloadingTime() < 65 && isAlive(tank)) {
                log("EVADE TURRENT FAST FORWARD " + self.getId() + " remaining reloading time()" + tank.getRemainingReloadingTime() + " max" + tank.getReloadingTime());
                doEvade();
                unstuck();
                return true;
            }
        }
        return false;
    }

    private void doEvade() {
        move.setLeftTrackPower(1D);
        move.setRightTrackPower(1D);
            // bottom
        if (self.getY() > (world.getHeight() / 3) * 2) {
            if (self.getAngle() < 0) {
                move.setLeftTrackPower(1D);
                move.setRightTrackPower(1D);
            } else {
                move.setLeftTrackPower(-1D);
                move.setRightTrackPower(-1D);
            }
            // top
        } else if (self.getY() < (world.getHeight() / 3) * 1) {
            if (self.getAngle() < 0) {
                move.setLeftTrackPower(-1D);
                move.setRightTrackPower(-1D);
            } else {
                move.setLeftTrackPower(1D);
                move.setRightTrackPower(1D);
            }
        }
    }

    private boolean checkEvadeArea() {
        double x = self.getX();
        double y = self.getY();
        if (x > world.getWidth() - CLOSE_TO_BORDER || x < CLOSE_TO_BORDER || y > world.getHeight() - CLOSE_TO_BORDER || y < CLOSE_TO_BORDER) {
            //   log("[checkEvadeArea] too close to border");
            return true;
        }

        return false;
    }

    private boolean unstuck() {
        if (stuck && world.getTick() - stuckTick < 100) {
            moveBack();
            lastPosX = 0;
            lastPosY = 0;
            return true;
        }
        if (world.getTick() % 100 == 0) {
            double delta = self.getDistanceTo(lastPosX, lastPosY);

            lastPosX = self.getX();
            lastPosY = self.getY();
            if (delta < 10 && delta != 0) {
                //    log("[unstuck]Distance: " + delta);
                //     log(String.valueOf(self.getDistanceTo(pickUpTarget)));
                stuck = true;
                stuckTick = world.getTick();
                moveBack();
                return true;
            } else {
                stuck = false;
            }
        }

        return false;
    }

    @Override
    public TankType selectTank(int tankIndex, int teamSize) {
        return TankType.MEDIUM;
    }

    private class Point {
        public Point(double x, double y) {
        }
    }

    private class Position {
        private double x;
        private double y;

        public Position(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public Position() {
            this.x = 0;
            this.y = 0;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public String toString() {
            return " x: " + this.getX() + " y:" + this.getY();
        }

        public void setCoordinates(Position position) {
            this.x = position.getX();
            this.y = position.getY();
        }
    }
}
