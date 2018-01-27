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
            System.out.print("round: " + gameController.round());
            System.out.println(" time left: " + gameController.getTimeLeftMs());

            VecUnit vecUnits = gameController.myUnits();
            for (long i = 0; i < vecUnits.size(); i++) {

                // first factory logic
                Unit currentUnit = vecUnits.get(i);

                if (currentUnit.unitType() == UnitType.Factory) {

                    // get all the unit ids in the garrison
                    VecUnitID vecUnitID = currentUnit.structureGarrison();
                    if (vecUnitID.size() > 0) {

                        Direction direction = randomEnum(Direction.class);
                        //System.out.print("The random direction was " + direction);

                        if (gameController.canUnload(currentUnit.id(), direction)) {

                            gameController.unload(currentUnit.id(), direction);
                            System.out.println("Unloaded a knight!");
                            continue;

                        }

                    } else if (gameController.canProduceRobot(currentUnit.id(), UnitType.Knight)) {

                        gameController.produceRobot(currentUnit.id(), UnitType.Knight);
                        System.out.println("Produced a Knight!");

                    }
                }

                Location location = currentUnit.location();
                if (location.isOnMap()) {

                    VecUnit nearbyUnits = gameController.senseNearbyUnits(location.mapLocation(), 2);
                    System.out.println("Size of nearbyUnits: " + nearbyUnits.size() + " | current unit type: " + currentUnit.unitType());
                    for (long x = 0; x < nearbyUnits.size(); x++) {
                        Unit nearbyUnit = nearbyUnits.get(x);

                        if (currentUnit.unitType() == UnitType.Worker && gameController.canBuild(currentUnit.id(), nearbyUnit.id())) {

                            gameController.build(currentUnit.id(), nearbyUnit.id());
                            System.out.println("Building a Factory!");
                            continue;

                        } else if (nearbyUnit.team() != myTeam && gameController.isAttackReady(currentUnit.id()) && gameController.canAttack(currentUnit.id(), nearbyUnit.id())) {

                            gameController.attack(currentUnit.id(), nearbyUnit.id());
                            System.out.println("Attacked a unit");
                            continue;

                        }
                    }

                }

                // there were no units around
                Direction randDir = randomEnum(Direction.class);

                if (gameController.karbonite() > bc.bcUnitTypeBlueprintCost(UnitType.Factory) && gameController.canBlueprint(currentUnit.id(), UnitType.Factory, randDir)) {

                    gameController.blueprint(currentUnit.id(), UnitType.Factory, randDir);
                    System.out.println("Blueprinted a factory!!!");

                } else if (gameController.isMoveReady(currentUnit.id()) && gameController.canMove(currentUnit.id(), randDir)) {

                    gameController.moveRobot(currentUnit.id(), randDir);
                    System.out.println("Moved the unit");

                }
            }

            System.out.println("---------------Finished Round-----------------");
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
