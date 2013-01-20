import model.*;

import java.util.HashMap;

import static java.lang.StrictMath.PI;

public final class MyStrategy implements Strategy {
    long meters;
    double minAgle = PI / 180;
    double angleToTarget;
    String targetName = "SmartGuy";
    Unit moveTarget;
    Tank self;
    World world;
    Move move;
    private double lastPosX;
    private double lastPosY;
    private boolean stuck = false;
    private int stuckTick;

    MyStrategy() {

    }

    @Override
    public void move(Tank self, World world, Move move) {
        this.self = self;
        this.world = world;
        this.move = move;
        HashMap<String, Tank> tanks = new HashMap<String, Tank>();
        for (Tank tank : world.getTanks()) {
            tanks.put(tank.getPlayerName(), tank);
        }

        if (targetName != null && tanks.get(targetName).getCrewHealth() == 0) {
            targetName = null;
        }
        for (Tank tank : tanks.values())
            if (!tank.isTeammate() && tank.getCrewHealth() != 0 && (targetName == null || tank.getPlayerName().equals(targetName))) {
                angleToTarget = self.getTurretAngleTo(tank);
                move.setTurretTurn(angleToTarget);
                if (Math.abs(angleToTarget) > minAgle) {
                    move.setFireType(FireType.NONE);
                } else {
                    move.setFireType(FireType.PREMIUM_PREFERRED);
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
            if (angle < minAngleToBonus) {
                minAngleToBonus = angle;
            }

            if (bonus.getType().equals(BonusType.MEDIKIT) && angle < minAngleToMedkit && self.getCrewHealth() != 100 ) {
                minAngleToMedkit = angle;
            }
        }
        if (bonusNoExists && moveTarget!=null) {
            System.out.println(" REMOVE MOVE TARGET: " + moveTarget.getId());
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
                moveTo(moveTarget);
                moved = true;
            } else if (angle == minAngleToBonus) {
                moveTarget = bonus;
                moveTo(moveTarget);
                moved = true;
            }
        }

        if (!moved) {
            moveBack();
        }


    }

    private void moveBack() {
        move.setLeftTrackPower(-0.6D);
        move.setRightTrackPower(-0.6D);
    }

    private void moveTo(Unit unit) {
        double angleToUnit = self.getAngleTo(unit);
       // System.out.println("move to" + unit.getId() + " type:" + ((Bonus) unit).getType() + " distance: " + self.getDistanceTo(unit) + " pos x  y" + unit.getX() + " " + unit.getY());
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

    private boolean unstuck() {
        if (stuck && world.getTick() - stuckTick < 30) {
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
