import model.*;

import java.util.ArrayList;
import java.util.HashMap;

import static java.lang.StrictMath.PI;

public final class MyStrategy implements Strategy {
    private static final double MAX_DISTANCE_TO_BONUS = 700;
    private static final int EVADE_COOLDOWN = 300;
    private static final int EVADE_TIME = 100;
    double minAgle = PI / 180;
    double angleToTarget;
    String targetName;// = "SmartGuy";
    static Long mainTarget;
    Long secondaryTarget;
    Unit moveTarget;
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

    MyStrategy() {

    }

    @Override
    public void move(Tank self, World world, Move move) {
        this.self = self;
        this.world = world;
        this.move = move;
        initCollections();
        move();
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
            if (tank.isTeammate() && self.getId() != tank.getId()) {
                obstacles.add(tank);
            }
        }
    }

    private void move() {
        shootEnemy();
        pickUpBonus();
    }

    private void shootEnemy() {
        if ((mainTarget != null && !isAlive(tanks.get(mainTarget))) || mainTarget == null) {
            mainTarget = findNewTarget();
        }
        log("[shootEnemy] mainTarget: " + mainTarget);
        if (mainTarget != null && !checkObstacle(obstacles, tanks.get(mainTarget))) {
            shootTank(tanks.get(mainTarget));
        } else {
            log("[shootEnemy] dont shoot mainTarget! have Obstacles!");
            if (secondaryTarget == null) {
                secondaryTarget = findNewTarget();
            }
            shootTank(tanks.get(secondaryTarget));
        }
           /*

        for (Tank tank : tanks.values())
            if (!tank.isTeammate() && (tank.getCrewHealth() != 0 && tank.getHullDurability() != 0) && (mainTarget == null || ((Long) tank.getId()).equals(mainTarget))) {
                if (tank.getPlayerName().equals(targetName) || targetIsDead()) {
                    angleToTarget = self.getTurretAngleTo(tank);
                    move.setTurretTurn(angleToTarget);
                    if (Math.abs(angleToTarget) > minAgle) {
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

    private void shootTank(Tank tank) {
        if (tank == null) {
            return;
        }
        angleToTarget = self.getTurretAngleTo(tank);
        move.setTurretTurn(angleToTarget);
        if (Math.abs(angleToTarget) > minAgle) {
            move.setFireType(FireType.NONE);
        } else {
            move.setFireType(FireType.PREMIUM_PREFERRED);
        }
    }

    private boolean checkObstacle(ArrayList<Unit> units, Unit target) {
        for (Unit unit : units) {
            if (checkObstacle(unit, target)) {
                log("[checkObstacle] have obstacle!: " + unit.getId() + " type: " + unit.getClass());
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

    private void pickUpBonus() {
        //  log("[pickUpBonus] distance to 0x0 " + self.getDistanceTo(0, 0) + " self position " + self.getX() + " " + self.getY());
        Bonus[] bonuses = world.getBonuses();
        double minDistanceToBonus = 2000;
        double minAngleToBonus = 1000;
        double minAngleToMedkit = 1000;
        double maxAngleToBonus = 0;
        double maxAngleToMedkit = 0;
        boolean bonusNoExists = true;

        for (Bonus bonus : bonuses) {
            if (moveTarget != null && bonus.getId() == moveTarget.getId()) {
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
            if (angle < minAngleToBonus && !moveTargets.contains(bonus.getId())) {
                minAngleToBonus = angle;
            }

            if (bonus.getType().equals(BonusType.MEDIKIT) && angle < minAngleToMedkit && self.getCrewHealth() != 100 && !moveTargets.contains(bonus.getId())) {
                minAngleToMedkit = angle;
            }
            if (angle > maxAngleToBonus && !moveTargets.contains(bonus.getId())) {
                maxAngleToBonus = angle;
            }

            if (bonus.getType().equals(BonusType.MEDIKIT) && angle > maxAngleToMedkit && self.getCrewHealth() != 100 && !moveTargets.contains(bonus.getId())) {
                maxAngleToMedkit = angle;
            }
        }
        if (bonusNoExists && moveTarget != null) {
            moveTargets.remove(moveTarget.getId());
            moveTarget = null;
        }

        boolean moved = false;
        for (Bonus bonus : bonuses) {
            double distance = self.getDistanceTo(bonus);
            double angle = Math.abs(self.getAngleTo(bonus));
            if (distance > MAX_DISTANCE_TO_BONUS) {
                //        log("[pickUpBonus] bonus + " + bonus.getType() + " " + bonus.getId() + " too far!");
                continue;
            }
            if (moveTarget != null) {
                moveTo(moveTarget);
                moved = true;
            } else if ((180 - maxAngleToBonus) > minAngleToBonus && angle == minAngleToBonus) {
                moveTarget = bonus;
                moveTargets.add(moveTarget.getId());
                moveTo(moveTarget);
                moved = true;
            } else if ((180 - maxAngleToBonus) < minAngleToBonus && angle == maxAngleToBonus) {
                moveTarget = bonus;
                moveTargets.add(moveTarget.getId());
                moveTo(moveTarget);
                moved = true;
            }
            //TODO Приоритет на подбор медкитов
                    /*(bonus.getType().equals(BonusType.MEDIKIT) && angle == minAngleToMedkit) {
                moveTarget = bonus;
                moveTargets.add(moveTarget.getId());
                moveTo(moveTarget);
                moved = true;
            } else if (angle == minAngleToBonus) {
                moveTarget = bonus;
                moveTargets.add(moveTarget.getId());
                moveTo(moveTarget);
                moved = true;
            }            */
        }

        if (!moved) {
            moveBack();
        }
    }

    private boolean checkObstacles(Unit unit) {
        double distance = self.getDistanceTo(unit);
        for (Tank tank : tanks.values()) {
            if ((tank.isTeammate() || tank.getCrewHealth() == 0 || tank.getHullDurability() == 0) && self.getDistanceTo(tank) < distance && Math.abs(self.getTurretAngleTo(tank)) < minAgle * 4) {
                // log("obstacle on fire line");
                return false;
            }
            for (Obstacle obstacle : world.getObstacles()) {
                if (checkObstacle(obstacle, null)) {
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
        }
        return true;
    }

    private boolean checkObstacle(Unit unit, Unit target) {
    /*
    1   4
    2   3
    * */
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
        double y3 = y - h / 2;
        double y4 = y + h / 2;
        /*
        log("unit x: " + x + " y " + y + " h w " + h + " " + w);
        log("p1 " + x1 + " " + y1);
        log("p2 " + x2 + " " + y2);
        log("p3 " + x3 + " " + y3);
        log("p4 " + x4 + " " + y4);
        */
        double px1 = self.getX();
        double py1 = self.getY();
        double px2 = target.getX();
        double py2 = target.getY();
        //log(" self - " + px1 + " " + py1 + "   target - " + px2 + " " + py2);

        return checkSquare(x1, x2, x3, x4, y1, y2, y3, y4, px1, py1, px2, py2);
    }

    private boolean checkSquare(double x1, double x2, double x3, double x4, double y1, double y2, double y3, double y4, double px1, double py1, double px2, double py2) {
        if (segmentsIntersect(x1, y2, x2, y2, px1, py1, px2, py2) || segmentsIntersect(x2, y2, x3, y3, px1, py1, px2, py2) || segmentsIntersect(x3, y3, x4, y4, px1, py1, px2, py2) || segmentsIntersect(x4, y4, x1, y1, px1, py1, px2, py2)) {

            return true;
        }
        return false;
    }

    public boolean segmentsIntersect(double x1, double y1, double x2, double y2, double x3, double y3, double x4, double y4) {
        double d = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);
        if (d == 0) return false;
        double xi = ((x3 - x4) * (x1 * y2 - y1 * x2) - (x1 - x2) * (x3 * y4 - y3 * x4)) / d;
        double yi = ((y3 - y4) * (x1 * y2 - y1 * x2) - (y1 - y2) * (x3 * y4 - y3 * x4)) / d;
        if (xi < Math.min(x1, x2) || xi > Math.max(x1, x2)) return false;
        if (xi < Math.min(x3, x4) || xi > Math.max(x3, x4)) return false;
        if (yi < Math.min(y1, y2) || yi > Math.max(y1, y2)) return false;
        if (yi < Math.min(y3, y4) || yi > Math.max(y3, y4)) return false;
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
        System.out.println("tankId: " + self.getId() + " message:" + message);
    }

    private boolean targetIsDead() {
        for (Tank tank : world.getTanks()) {
            if (tank.getPlayerName().equals(targetName) && tank.getCrewHealth() != 0) {
                return false;
            }
        }
        return true;
    }

    private void moveBack() {
        move.setLeftTrackPower(-0.6D);
        move.setRightTrackPower(-0.6D);
    }

    private void moveTo(Unit unit) {
        double angleToUnit = self.getAngleTo(unit);
        // log("move to" + unit.getId() + " type:" + ((Bonus) unit).getType() + " distance: " + self.getDistanceTo(unit) + " pos x  y" + unit.getX() + " " + unit.getY());

        if (evade()) {
            return;
        }

        /*
        if (unstuck()) {
            return;
        } */
        //  log("[moveTo] tank: " + self.getId() + "Angle to unit: " + ((Bonus) unit).getType() + " : " + angleToUnit);

        if (Math.abs(angleToUnit) > PI / 2 && !checkEvadeArea()) {
            //      log("[moveToBonus] hang back");
            //     log("[moveTo] minAgle * 170 > angle.. : " + minAgle * 170);
            if ((minAgle * 170) < Math.abs(angleToUnit)) {
                move.setLeftTrackPower(-1D);
                move.setRightTrackPower(-1D);
                //       log("[moveToBonus] FULL BACK");

            } else {
                if (angleToUnit > 0) {
                    move.setLeftTrackPower(-1D);
                    move.setRightTrackPower(1D);
                    //            log("[moveToBonus] BACK LEFT");
                } else {
                    move.setLeftTrackPower(1D);
                    move.setRightTrackPower(-1D);
                    //         log("[moveToBonus] BACK RIGHT");
                }
            }
        } else {
            //    log("[moveToBonus] move forward");
            if (minAgle * 10 > Math.abs(angleToUnit)) {
                move.setLeftTrackPower(1D);
                move.setRightTrackPower(1D);
            } else {
                if (angleToUnit > 0) {
                    move.setLeftTrackPower(1D);
                    move.setRightTrackPower(-1D);
                    //           log("[moveToBonus] RIGHT");
                } else {
                    move.setLeftTrackPower(-1D);
                    move.setRightTrackPower(1D);
                    //          log("[moveToBonus] LEFT");
                }
            }
        }
    }

    private boolean evade() {
        if (world.getTick() - lastTimeEvade < EVADE_COOLDOWN && world.getTick() - lastTimeEvade > EVADE_TIME) {
            //    log("[evade] COOLDOWN");
            return false;
        }
        if (checkEvadeArea()) {
            return false;
        }
        for (Shell shell : world.getShells()) {
            if (Math.abs(shell.getAngleTo(self)) < minAgle * 2) {
                //      log("EVADE SHELL FAST FORWARD " + self.getId());
                move.setLeftTrackPower(1D);
                move.setRightTrackPower(1D);
                if (world.getTick() - lastTimeEvade > EVADE_TIME) {
                    lastTimeEvade = world.getTick();
                }
                unstuck();
                return true;
            }
        }
        for (Tank tank : world.getTanks()) {
            if (Math.abs(tank.getTurretAngleTo(self)) < minAgle * 2 && tank.getRemainingReloadingTime() < 80 && isAlive(tank)) {
                //  log("EVADE TURRENT FAST FORWARD " + self.getId() + " remaining reloading time()" + tank.getRemainingReloadingTime() + " max" + tank.getReloadingTime());
                move.setLeftTrackPower(1D);
                move.setRightTrackPower(1D);
                unstuck();
                return true;
            }
        }
        return false;
    }

    private boolean checkEvadeArea() {
        double x = self.getX();
        double y = self.getY();
        if (x > world.getWidth() - 150 || x < 150 || y > world.getHeight() - 150 || y < 150) {
            //   log("[checkEvadeArea] too close to border");
            return true;
        }

        return false;  //To change body of created methods use File | Settings | File Templates.
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
                //     log(String.valueOf(self.getDistanceTo(moveTarget)));
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
}
