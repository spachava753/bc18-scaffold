import bc.*;

import java.security.SecureRandom;

public class Player {
    private static GameController gameController = null;
    private static Team myTeam = null;
    private static Team enemyTeam = null;
    private static SecureRandom random = new SecureRandom();

    public static void main (String[] args) {
        System.out.print("GameController connected");
        gameController = new GameController();

        myTeam = gameController.team();
        enemyTeam = Team.Blue == myTeam ? Team.Red : Team.Blue;
        System.out.println("My team is " + myTeam.name());
        System.out.println("Enemy team is " + enemyTeam.name());

        System.out.println("Queuing research");
        gameController.queueResearch(UnitType.Rocket);
        gameController.queueResearch(UnitType.Worker);
        gameController.queueResearch(UnitType.Knight);

        while (true) {
            System.out.print("pyround: " + gameController.round());
            System.out.println(" time left: " + gameController.getTimeLeftMs());

            //System.out.println("random direction: " + randomEnum(Direction.class));

            VecUnit vecUnits = gameController.myUnits();
            for (long i = 0; i < vecUnits.size(); i++) {

                // first factory logic
                Unit unit = vecUnits.get(i);

                if (unit.unitType() == UnitType.Factory) {

                    // get all the unit ids in the garrison
                    VecUnitID vecUnitID = unit.structureGarrison();
                    if (vecUnitID.size() > 0) {

                        Direction direction = randomEnum(Direction.class);
                        //System.out.print("The random direction was " + direction);

                        if (gameController.canUnload(unit.id(), direction)) {

                            gameController.unload(unit.id(), direction);
                            System.out.println("Unloaded a knight!");
                            continue;

                        }

                    } else if (gameController.canProduceRobot(unit.id(), UnitType.Knight)) {

                        gameController.produceRobot(unit.id(), UnitType.Knight);
                        System.out.println("Produced a Knight!");

                    }

                    Location location = unit.location();
                    if (location.isOnMap()) {

                        VecUnit nearbyUnits = gameController.senseNearbyUnits(location.mapLocation(), 2);
                        for (long x = 0; x < nearbyUnits.size(); x++) {
                            Unit nearbyUnit = nearbyUnits.get(x);

                            if (unit.unitType() == UnitType.Worker && gameController.canBuild(unit.id(), nearbyUnit.id())) {

                                gameController.build(unit.id(), nearbyUnit.id());
                                System.out.println("Building a Factory!");
                                continue;

                            } else if (nearbyUnit.team() != myTeam && gameController.isAttackReady(unit.id()) && gameController.canAttack(unit.id(), nearbyUnit.id())) {

                                gameController.attack(unit.id(), nearbyUnit.id());
                                System.out.println("Attacked a unit");
                                continue;

                            }
                        }

                    }
                }

                // there were no units around
                Direction randDir = randomEnum(Direction.class);

                if (gameController.karbonite() > bc.bcUnitTypeBlueprintCost(UnitType.Factory) && gameController.canBlueprint(unit.id(), UnitType.Factory, randDir)) {

                    gameController.blueprint(unit.id(), UnitType.Factory, randDir);
                    //System.out.println("Blueprinted a factory!!!");

                } else if (gameController.isMoveReady(unit.id()) && gameController.canMove(unit.id(), randDir)) {

                    gameController.moveRobot(unit.id(), randDir);
                    //System.out.println("Moved the unit");

                }
            }

            //System.out.println("---------------Finished Round-----------------");
            gameController.nextTurn();
        }
    }

    public static GameController getGC(){
        return gameController;
    }

    public static <T extends Enum<?>> T randomEnum(Class<T> clazz){
        int x = random.nextInt(clazz.getEnumConstants().length);
        return clazz.getEnumConstants()[x];
    }
}
