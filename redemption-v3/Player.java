import bc.*;

import java.security.SecureRandom;
import java.util.*;

public class Player {
    private static GameController gameController = null;
    private static Team myTeam = null;
    private static Team enemyTeam = null;
    private static SecureRandom random = new SecureRandom();

    private static Map<Integer, Unit> factoryMap = new HashMap<>();
    private static Map<Integer, Unit> builtFactoryMap = new HashMap<>();
    private static Map<Integer, Unit> workerMap = new HashMap<>();
    private static Map<Integer, Unit> knightMap = new HashMap<>();

    public static void main (String[] args) {
        factoryMap = Collections.synchronizedMap(factoryMap);
        builtFactoryMap = Collections.synchronizedMap(builtFactoryMap);
        workerMap = Collections.synchronizedMap(workerMap);
        knightMap = Collections.synchronizedMap(knightMap);

        //System.out.print("GameController connected");
        gameController = new GameController();

        myTeam = gameController.team();
        enemyTeam = Team.Blue == myTeam ? Team.Red : Team.Blue;
        //System.out.println("My team is " + myTeam.name());
        //System.out.println("Enemy team is " + enemyTeam.name());

        //System.out.println("Queuing research");
        gameController.queueResearch(UnitType.Rocket);
        gameController.queueResearch(UnitType.Worker);
        gameController.queueResearch(UnitType.Knight);
        gameController.queueResearch(UnitType.Worker);
        gameController.queueResearch(UnitType.Worker);
        gameController.queueResearch(UnitType.Worker);
        gameController.queueResearch(UnitType.Knight);
        gameController.queueResearch(UnitType.Knight);
        gameController.queueResearch(UnitType.Rocket);
        gameController.queueResearch(UnitType.Rocket);
        gameController.queueResearch(UnitType.Ranger);
        gameController.queueResearch(UnitType.Ranger);
        gameController.queueResearch(UnitType.Ranger);

        while (true) {
            //System.out.print("round: " + gameController.round());
            //System.out.println(" time left: " + gameController.getTimeLeftMs());

            VecUnit vecUnits = gameController.myUnits();
            for (long i = 0; i < vecUnits.size(); i++) {
                Unit currentUnit = vecUnits.get(i);

                // update the unit list
                updateMaps(currentUnit);



                // first factory logic
                if (currentUnit.unitType() == UnitType.Factory) {

                    // get all the unit ids in the garrison
                    VecUnitID vecUnitID = currentUnit.structureGarrison();
                    if (vecUnitID.size() > 0) {

                        if (!builtFactoryMap.containsKey(currentUnit.id())) {
                            builtFactoryMap.put(currentUnit.id(), currentUnit);
                        }

                        Direction direction = randomEnum(Direction.class);
                        ////System.out.print("The random direction was " + direction);

                        if (gameController.canUnload(currentUnit.id(), direction)) {

                            gameController.unload(currentUnit.id(), direction);
                            //System.out.println("Unloaded a knight!");
                            continue;

                        }

                    } else if (gameController.canProduceRobot(currentUnit.id(), UnitType.Knight)) {

                        gameController.produceRobot(currentUnit.id(), UnitType.Knight);
                        //System.out.println("Produced a Knight!");

                        if (!builtFactoryMap.containsKey(currentUnit.id())) {
                            builtFactoryMap.put(currentUnit.id(), currentUnit);
                        }
                    }
                }


                // replicating code
                if (workerMap.size() < 20) {
                    if (currentUnit.unitType() == UnitType.Worker) {
                        for (Direction direction : Direction.values()) {
                            if (gameController.canReplicate(currentUnit.id(), direction)) {
                                gameController.replicate(currentUnit.id(), direction);
                                break;
                            }
                        }
                    }
                }


                Location location = currentUnit.location();
                if (location.isOnMap()) {

                    VecUnit nearbyUnits = gameController.senseNearbyUnits(location.mapLocation(), 2);
                    //System.out.println("Size of nearbyUnits: " + nearbyUnits.size() + " | current unit type: " + currentUnit.unitType());
                    for (long x = 0; x < nearbyUnits.size(); x++) {
                        Unit nearbyUnit = nearbyUnits.get(x);

                        if (currentUnit.unitType() == UnitType.Worker && gameController.canBuild(currentUnit.id(), nearbyUnit.id())) {

                            gameController.build(currentUnit.id(), nearbyUnit.id());
                            //System.out.println("Building a Factory!");
                            continue;

                        } else if (nearbyUnit.team() != myTeam && gameController.isAttackReady(currentUnit.id()) && gameController.canAttack(currentUnit.id(), nearbyUnit.id())) {

                            gameController.attack(currentUnit.id(), nearbyUnit.id());
                            //System.out.println("Attacked a unit");
                            continue;

                        }
                    }

                }



                // harvesting code here
                //System.out.println("In harvesting code");
                for (Direction direction: Direction.values()) {
                    if (gameController.canHarvest(currentUnit.id(), direction)) {
                        gameController.harvest(currentUnit.id(), direction);
                        //System.out.println("Harvested some karbonite!");
                    }
                }



                // there were no units around
                Direction randDir = randomEnum(Direction.class);

                if (gameController.karbonite() > bc.bcUnitTypeBlueprintCost(UnitType.Factory)) {

                    if (factoryMap.size() < 20) {

                        for (Direction direction : Direction.values()) {

                            if (gameController.canBlueprint(currentUnit.id(), UnitType.Factory, direction)) {
                                if (currentUnit.location().mapLocation().getX() != gameController.startingMap(Planet.Earth).getWidth()
                                        && currentUnit.location().mapLocation().getY() != gameController.startingMap(Planet.Earth).getHeight()) {
                                    gameController.blueprint(currentUnit.id(), UnitType.Factory, direction);
                                    //System.out.println("Blueprinted a factory!!!");
                                    continue;
                                }
                            }
                        }
                    }

                } else if (currentUnit.unitType() == UnitType.Worker && gameController.isMoveReady(currentUnit.id())) {

                    MapLocation factoryToBuildMapLoc = null;
                    for (Unit factory : factoryMap.values()) {

                        if (builtFactoryMap.containsKey(factory.id()))
                            continue;

                        if (factoryToBuildMapLoc == null) {
                            factoryToBuildMapLoc = factory.location().mapLocation();
                        } else {
                            long distanceToChosenLoc = currentUnit.location().mapLocation().distanceSquaredTo(factoryToBuildMapLoc);
                            long distanceLoc = currentUnit.location().mapLocation().distanceSquaredTo(factory.location().mapLocation());

                            if (distanceToChosenLoc > distanceLoc) {
                                factoryToBuildMapLoc = factory.location().mapLocation();
                            }

                        }
                    }

                    if (factoryToBuildMapLoc == null) {
                        tryToMoveInAnyDirection(currentUnit);
                    } else {
                        Direction direction = currentUnit.location().mapLocation().directionTo(factoryToBuildMapLoc);
                        if (gameController.canMove(currentUnit.id(), direction)) {
                            gameController.moveRobot(currentUnit.id(), direction);
                        }
                    }

                } else if (currentUnit.unitType() == UnitType.Knight && gameController.isMoveReady(currentUnit.id()) && currentUnit.location().isOnMap()) {

                    VecUnit vecAllUnits = gameController.units();
                    MapLocation closestEnemyLoc = null;

                    for (int x = 0; x < vecAllUnits.size(); x++) {
                        Unit unit = vecAllUnits.get(x);

                        if (unit.team() == myTeam || !unit.location().isOnMap()) {
                            continue;
                        }

                        if (closestEnemyLoc == null) {
                            closestEnemyLoc = unit.location().mapLocation();
                        } else {
                            long closestEnemyDist = currentUnit.location().mapLocation().distanceSquaredTo(closestEnemyLoc);
                            long enemyDist = currentUnit.location().mapLocation().distanceSquaredTo(unit.location().mapLocation());

                            if (closestEnemyDist > enemyDist) {

                                closestEnemyLoc = unit.location().mapLocation();

                            }
                        }
                    }

                    if (closestEnemyLoc != null) {
                        Direction direction = currentUnit.location().mapLocation().directionTo(closestEnemyLoc);

                        if (gameController.canMove(currentUnit.id(), direction)) {
                            gameController.moveRobot(currentUnit.id(), direction);
                        } else {
                            tryToMoveInAnyDirection(currentUnit);
                        }

                    } else {

                        tryToMoveInAnyDirection(currentUnit);

                    }

                    //System.out.println("Moved the unit");

                } else if (gameController.isMoveReady(currentUnit.id())) {
                    tryToMoveInAnyDirection(currentUnit);
                }
            }


            int count = 0;
            for (Unit unit : knightMap.values()) {
                if (unit.health() == 0) {
                    count++;
                }
            }
            System.out.println("The death counter is " + count);

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

    private static void updateMaps (Unit unit) {
        if (unit.unitType() == UnitType.Factory && !factoryMap.containsKey(unit.id())) {
            factoryMap.put(unit.id(), unit);
        } else if (unit.unitType() == UnitType.Worker && !workerMap.containsKey(unit.id())) {
            workerMap.put(unit.id(), unit);
        }  else if (unit.unitType() == UnitType.Knight && !knightMap.containsKey(unit.id())) {
            knightMap.put(unit.id(), unit);
        }
    }

    private static void tryToMoveInAnyDirection (Unit unit) {
        for (Direction direction : Direction.values()) {
            if (gameController.canMove(unit.id(), direction)) {
                gameController.moveRobot(unit.id(), direction);
                //System.out.println("Moved the unit");
                break;
            }
        }
    }
}
