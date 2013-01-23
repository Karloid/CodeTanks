import model.*;

import java.util.ArrayList;
import java.util.HashMap;

import static java.lang.StrictMath.PI;

public final class MyStrategy implements Strategy {
    long meters;
    double minAgle = PI / 180;
    double angleToTarget;
    String targetName;// = "SmartGuy";
    static Long targetId;
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

    MyStrategy() {

    }

    @Override
    public void move(Tank self, World world, Move move) {
        this.self = self;
        this.world = world;
        this.move = move;
        tanks = new HashMap<Long, Tank>();
        for (Tank tank : world.getTanks()) {
            tanks.put(tank.getId(), tank);
        }

        if (targetId != null && (tanks.get(targetId).getCrewHealth() == 0 || tanks.get(targetId).getHullDurability() == 0)) {
            targetId = null;
        }
        for (Tank tank : tanks.values())
            if (!tank.isTeammate() && (tank.getCrewHealth() != 0 && tank.getHullDurability() != 0) && (targetId == null || ((Long) tank.getId()).equals(targetId))) {
                if (tank.getPlayerName().equals(targetName) || targetIsDead()) {
                    angleToTarget = self.getTurretAngleTo(tank);
                    move.setTurretTurn(angleToTarget);
                    if (Math.abs(angleToTarget) > minAgle) {
                        move.setFireType(FireType.NONE);
                    } else {
                        targetId = tank.getId();
                        if (noTeammateOnFireLine(tank)) {
                            move.setFireType(FireType.PREMIUM_PREFERRED);
                        } else {
                            move.setFireType(FireType.NONE);
                        }
                    }
                }
            }
        Bonus[] bonuses = world.getBonuses();
        double minDistanceToBonus = 2000;
        double minAngleToBonus = 1000;
        double minAngleToMedkit = 1000;
        boolean bonusNoExists = true;

        for (Bonus bonus : bonuses) {
            if (moveTarget != null && bonus.getId() == moveTarget.getId()) {
                bonusNoExists = false;
            }
            double distance = self.getDistanceTo(bonus);
            double angle = Math.abs(self.getAngleTo(bonus));
            if (distance < minDistanceToBonus) {
                minDistanceToBonus = distance;
            }
            if (!moveTargets.contains(bonus.getId())) {
                // System.out.println("Bonus claim teammate: " + bonus.getId());
            }
            if (angle < minAngleToBonus && !moveTargets.contains(bonus.getId())) {
                minAngleToBonus = angle;
            }

            if (bonus.getType().equals(BonusType.MEDIKIT) && angle < minAngleToMedkit && self.getCrewHealth() != 100 && !moveTargets.contains(bonus.getId())) {
                minAngleToMedkit = angle;
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
            if (moveTarget != null) {
                moveTo(moveTarget);
                moved = true;
            } else if (bonus.getType().equals(BonusType.MEDIKIT) && angle == minAngleToMedkit) {
                moveTarget = bonus;
                moveTargets.add(moveTarget.getId());
                moveTo(moveTarget);
                moved = true;
            } else if (angle == minAngleToBonus) {
                moveTarget = bonus;
                moveTargets.add(moveTarget.getId());
                moveTo(moveTarget);
                moved = true;
            }
        }

        if (!moved) {
            moveBack();
        }


    }

    private boolean noTeammateOnFireLine(Unit unit) {
        double distance = self.getDistanceTo(unit);
        for (Tank tank : tanks.values()) {
            if ((tank.isTeammate() || tank.getCrewHealth() == 0 || tank.getHullDurability() == 0) && self.getDistanceTo(tank) < distance && Math.abs(self.getTurretAngleTo(tank)) < minAgle * 4) {
                System.out.println("No fire!");
                return false;
            }
        }
        return true;
    }

    private boolean targetIsDead() {
        for (Tank tank : world.getTanks()) {
            if (tank.getPlayerName().equals(targetName) && tank.getCrewHealth() != 0) {
                return false;
            }
        }
        return true;  //To change body of created methods use File | Settings | File Templates.
    }

    private void moveBack() {
        move.setLeftTrackPower(-0.6D);
        move.setRightTrackPower(-0.6D);
    }

    private void moveTo(Unit unit) {
        double angleToUnit = self.getAngleTo(unit);
        // System.out.println("move to" + unit.getId() + " type:" + ((Bonus) unit).getType() + " distance: " + self.getDistanceTo(unit) + " pos x  y" + unit.getX() + " " + unit.getY());
        if (evade()) {
            return;
        }
        if (unstuck()) {
            return;
        }

        if (minAgle * 10 > Math.abs(angleToUnit)) {
            move.setLeftTrackPower(1D);
            move.setRightTrackPower(1D);
        } else {
            if (angleToUnit > 0) {
                move.setLeftTrackPower(1D);
                move.setRightTrackPower(-1D);
            } else {
                move.setLeftTrackPower(-1D);
                move.setRightTrackPower(1D);
            }
        }
    }

    private boolean evade() {
        for (Shell shell : world.getShells()) {
            if (Math.abs(shell.getAngleTo(self)) < minAgle * 2) {
                System.out.println("EVADE SHELL FAST FORWARD " + self.getId());
                move.setLeftTrackPower(1D);
                move.setRightTrackPower(1D);
                unstuck();
                return true;
            }
        }
        for (Tank tank : world.getTanks()) {
            if (Math.abs(tank.getTurretAngleTo(self)) < minAgle * 2 && tank.getRemainingReloadingTime() < 80 && isAlive(tank)) {
                System.out.println("EVADE TURRENT FAST FORWARD " + self.getId() + " remaining reloading time()" + tank.getRemainingReloadingTime() + " max" + tank.getReloadingTime());
                move.setLeftTrackPower(1D);
                move.setRightTrackPower(1D);
                unstuck();
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
                System.out.println("Distance: " + delta);
                System.out.println(self.getDistanceTo(moveTarget));
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
