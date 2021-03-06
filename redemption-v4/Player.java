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
    private static Map<Integer, Unit> rangerMap = new HashMap<>();

    private static MapLocation[] enemyMapLocation = null;

    private static Unit rocket = null;
    private static List<MapLocation> marsMapLocations = new LinkedList<>();

    public static void main (String[] args) {
        factoryMap = Collections.synchronizedMap(factoryMap);
        builtFactoryMap = Collections.synchronizedMap(builtFactoryMap);
        workerMap = Collections.synchronizedMap(workerMap);
        knightMap = Collections.synchronizedMap(knightMap);

        //System.out.print("GameController connected");
        gameController = new GameController();

        // mars landing spots
        PlanetMap marsMap = gameController.startingMap(Planet.Mars);
        for (int x = 0; x < marsMap.getWidth(); x++) {
            for (int y = 0; y < marsMap.getHeight(); y++) {
                MapLocation mapLocation = new MapLocation(Planet.Mars, x, y);
                if (marsMap.isPassableTerrainAt(mapLocation) == 1) {
                    marsMapLocations.add(mapLocation);
                }
            }
        }

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
            System.out.print("round: " + gameController.round());
            System.out.println(" time left: " + gameController.getTimeLeftMs());

            if (knightMap.size() >= 40 && rangerMap.size() >= 25) {
                VecUnit vecUnit = gameController.startingMap(Planet.Earth).getInitial_units();
                //enemyMapLocation = new MapLocation[(int) (vecUnit.size()/2)];

                int counter = 0;
                for (long q = 0; q < vecUnit.size(); q++) {

                    if (vecUnit.get(q).team() != myTeam) {
                        enemyMapLocation[counter] = vecUnit.get(q).location().mapLocation();
                        counter++;
                    }
                }
            }

            VecUnit vecUnits = gameController.myUnits();

            // update the unit list
            if (gameController.getTimeLeftMs() > 700) {
                updateMaps(vecUnits);
            }

            for (long i = 0; i < vecUnits.size(); i++) {
                Unit currentUnit = vecUnits.get(i);

                if (currentUnit.location().isInSpace() || currentUnit.location().isInGarrison()) {
                    continue;
                }

                if (currentUnit.unitType() == UnitType.Rocket)
                    rocket = currentUnit;

                if (currentUnit.unitType() == UnitType.Factory) {
                    runFactory (currentUnit);
                    continue;
                }

                if (currentUnit.unitType() == UnitType.Worker) {
                    if (currentUnit.location().isOnPlanet(Planet.Earth)) {

                        // replicating code
                        if (unitCountOnPlanet(workerMap, Planet.Earth) < 20) {
                            if (currentUnit.unitType() == UnitType.Worker) {
                                for (Direction direction : Direction.values()) {
                                    if (replicate(currentUnit, direction))
                                        break;
                                }
                            }
                        }

                        runWorker(currentUnit);
                    } else {
                        runMarsWorker(currentUnit);
                    }

                    continue;
                }

                if (currentUnit.unitType() == UnitType.Knight || currentUnit.unitType() == UnitType.Ranger) {
                    runCombatUnit(currentUnit);
                    continue;
                }

                // rocketCode
                if (currentUnit.unitType() == UnitType.Rocket) {
                    runRocket(currentUnit);
                }

            }

            //System.out.println("---------------Finished Round-----------------");
            gameController.nextTurn();
        }
    }

    private static void runMarsWorker(Unit currentUnit) {
        System.out.println("Is a Mars worker");
        harvest(currentUnit);
        tryToMoveInAnyDirection(currentUnit);

        if (unitCountOnPlanet(workerMap, Planet.Mars) < 20) {
            if (currentUnit.unitType() == UnitType.Worker) {
                for (Direction direction : Direction.values()) {
                    if (replicate(currentUnit, direction)){

                        System.out.println("Replicated on Mars");
                        break;
                    }
                }
            }
        }

        if (gameController.karbonite() > bc.bcUnitTypeBlueprintCost(UnitType.Factory)) {

            if (unitCountOnPlanet(factoryMap, Planet.Mars) <= 4) {

                for (Direction direction : Direction.values()) {

                    if (gameController.canBlueprint(currentUnit.id(), UnitType.Factory, direction)) {
                        if (currentUnit.location().mapLocation().getX() != gameController.startingMap(Planet.Earth).getWidth()
                                && currentUnit.location().mapLocation().getY() != gameController.startingMap(Planet.Earth).getHeight()) {
                            gameController.blueprint(currentUnit.id(), UnitType.Factory, direction);
                            //System.out.println("Blueprinted a factory!!!");
                        }
                    }
                }
            }

        }

        Location location = currentUnit.location();
        if (location.isOnMap()) {

            VecUnit nearbyUnits = gameController.senseNearbyUnits(location.mapLocation(), (long) Math.sqrt(currentUnit.visionRange()));
            //System.out.println("Size of nearbyUnits: " + nearbyUnits.size() + " | current unit type: " + currentUnit.unitType());
            for (long x = 0; x < nearbyUnits.size(); x++) {
                Unit nearbyUnit = nearbyUnits.get(x);

                if (currentUnit.unitType() == UnitType.Worker && gameController.canBuild(currentUnit.id(), nearbyUnit.id())) {

                    gameController.build(currentUnit.id(), nearbyUnit.id());
                    //System.out.println("Building a Factory!");
                    continue;

                } else if (nearbyUnit.team() != myTeam) {

                    Direction runAwayDir = nearbyUnit.location().mapLocation().directionTo(currentUnit.location().mapLocation());

                    move(currentUnit.id(), runAwayDir);

                    //System.out.println("Attacked a unit");
                    continue;

                } else {
                    //tryToMoveInAnyDirection(currentUnit);
                }
            }

        }

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

        if (factoryToBuildMapLoc != null) {
            Direction direction = currentUnit.location().mapLocation().directionTo(factoryToBuildMapLoc);
            move(currentUnit.id(), direction);
        }

        // if we can still move, that means we haven't moved at all
        if (gameController.isMoveReady(currentUnit.id())) {
            MapLocation targetLocation = null;
            VecMapLocation vecMapLocation = gameController.allLocationsWithin(currentUnit.location().mapLocation(), (long) Math.sqrt(currentUnit.visionRange()) );
            for (long a = 0; a < vecMapLocation.size(); a++) {
                MapLocation mapLoc = vecMapLocation.get(a);

                if (gameController.karboniteAt(mapLoc) > 0 && !gameController.hasUnitAtLocation(mapLoc)) {
                    if (targetLocation == null) {

                        targetLocation = mapLoc;

                    } else if (gameController.karboniteAt(targetLocation) < gameController.karboniteAt(mapLoc)) {
                        targetLocation = mapLoc;
                    }
                }

            }

            if (targetLocation != null)
                move(currentUnit.id(), currentUnit.location().mapLocation().directionTo(targetLocation));
            else
                tryToMoveInAnyDirection(currentUnit);
        }
    }

    private static void runRocket(Unit rocket) {
        if (rocket.structureGarrison().size() == 8 && rocket.rocketIsUsed() == 0 && rocket.location().isOnMap()) {

            if (gameController.canLaunchRocket(rocket.id(), marsMapLocations.get(0))) {
                gameController.launchRocket(rocket.id(), marsMapLocations.get(0));
                marsMapLocations.remove(0);
            }

        } else if (rocket.structureGarrison().size() <= 8 && rocket.structureGarrison().size() > 0
                && rocket.rocketIsUsed() == 1 && rocket.location().isOnPlanet(Planet.Mars)) {

            for (int i = 0; i < rocket.structureGarrison().size(); i++) {

                for (Direction direction: Direction.values()) {

                    if (gameController.canUnload(rocket.id(), direction)) {
                        gameController.unload(rocket.id(), direction);
                        break;

                    }
                }
            }
        } else if (rocket.rocketIsUsed() == 1 && rocket.location().isOnPlanet(Planet.Mars) && rocket.structureGarrison().size() > 0) {
            // nothing for the rocket to do
            gameController.disintegrateUnit(rocket.id());
            rocket = null;
            return;
        }

        // load units
        if (rocket.structureGarrison().size() < 8 && rocket.location().isOnPlanet(Planet.Earth)) {

            VecUnit vecUnit = gameController.senseNearbyUnitsByTeam(rocket.location().mapLocation(), rocket.visionRange(), myTeam);
            for (int i = 0; i < vecUnit.size(); i++) {
                Unit nearbyUnit = vecUnit.get(i);

                if (nearbyUnit.unitType() == UnitType.Worker) {
                    if (gameController.canLoad(rocket.id(), nearbyUnit.id())) {
                        gameController.load(rocket.id(), nearbyUnit.id());
                    }
                }
            }

        }
    }

    private static void runFactory (Unit factory) {
        // first factory logic
        if (factory.unitType() == UnitType.Factory) {

            // get all the unit ids in the garrison
            VecUnitID vecUnitID = factory.structureGarrison();
            if (vecUnitID.size() > 0) {

                if (!builtFactoryMap.containsKey(factory.id())) {
                    builtFactoryMap.put(factory.id(), factory);
                }

                Direction direction = randomEnum(Direction.class);
                ////System.out.print("The random direction was " + direction);

                if (gameController.canUnload(factory.id(), direction)) {

                    gameController.unload(factory.id(), direction);
                    //System.out.println("Unloaded a knight!");

                }

            } /*else if (knightMap.size() <= 50 && gameController.canProduceRobot(factory.id(), UnitType.Knight)) {

                gameController.produceRobot(factory.id(), UnitType.Knight);
                //System.out.println("Produced a Knight!");

                if (!builtFactoryMap.containsKey(factory.id())) {
                    builtFactoryMap.put(factory.id(), factory);
                }
            } else if (rangerMap.size() <= 30 && gameController.canProduceRobot(factory.id(), UnitType.Ranger)) {
                gameController.produceRobot(factory.id(), UnitType.Ranger);
                //System.out.println("Produced a Knight!");

                if (!builtFactoryMap.containsKey(factory.id())) {
                    builtFactoryMap.put(factory.id(), factory);
                }
            }*/
        }
    }

    private static void runCombatUnit(Unit currentUnit) {

        Location location = currentUnit.location();
        if (location.isOnMap()) {

            VecUnit nearbyUnits = gameController.senseNearbyUnits(location.mapLocation(), (long) Math.sqrt(currentUnit.attackRange()));
            //System.out.println("Size of nearbyUnits: " + nearbyUnits.size() + " | current unit type: " + currentUnit.unitType());
            /*
            for (long x = 0; x < nearbyUnits.size(); x++) {
                Unit nearbyUnit = nearbyUnits.get(x);

                if (nearbyUnit.team() != myTeam && gameController.isAttackReady(currentUnit.id()) && gameController.canAttack(currentUnit.id(), nearbyUnit.id())) {

                    gameController.attack(currentUnit.id(), nearbyUnit.id());
                    //System.out.println("Attacked a unit");
                    continue;

                }
            }
            */

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

            if (closestEnemyLoc != null && !currentUnit.location().mapLocation().isWithinRange(currentUnit.attackRange(), closestEnemyLoc)) {
                Direction direction = currentUnit.location().mapLocation().directionTo(closestEnemyLoc);

                move(currentUnit.id(), direction);

            } else if (closestEnemyLoc != null && currentUnit.location().mapLocation().isWithinRange((long) (currentUnit.attackRange()*0.2), closestEnemyLoc) && gameController.isMoveReady(currentUnit.id())) {

                Direction directionToRunAway = closestEnemyLoc.directionTo(currentUnit.location().mapLocation());

                move(currentUnit.id(), directionToRunAway);

            } else if (enemyMapLocation != null) {
                for (MapLocation mapLocation : enemyMapLocation) {
                    if (mapLocation != null) {
                        move(currentUnit.id(), currentUnit.location().mapLocation().directionTo(mapLocation));
                    }
                }

            } else {
                tryToMoveInAnyDirection(currentUnit);
            }

            //System.out.println("Moved the unit");
        }
    }

    private static void runWorker(Unit currentUnit) {

        harvest(currentUnit);

        if (rocket == null) {
            if (gameController.karbonite() > bc.bcUnitTypeBlueprintCost(UnitType.Rocket)) {
                for (Direction direction : Direction.values()) {

                    if (gameController.canBlueprint(currentUnit.id(), UnitType.Rocket, direction)) {
                        if (currentUnit.location().mapLocation().getX() != gameController.startingMap(Planet.Earth).getWidth()
                                && currentUnit.location().mapLocation().getY() != gameController.startingMap(Planet.Earth).getHeight()) {
                            gameController.blueprint(currentUnit.id(), UnitType.Rocket, direction);
                            System.out.println("Blueprinted a rocket!!!");
                        }
                    }
                }
            }
        } else if (currentUnit.location().isOnMap() && rocket != null){
            // try to board rocket
            move(currentUnit.id(), currentUnit.location().mapLocation().directionTo(rocket.location().mapLocation()));
        }

        if (gameController.karbonite() > bc.bcUnitTypeBlueprintCost(UnitType.Factory)) {

            if (unitCountOnPlanet(factoryMap, Planet.Earth) != 0) {

                for (Direction direction : Direction.values()) {

                    if (gameController.canBlueprint(currentUnit.id(), UnitType.Factory, direction)) {
                        if (currentUnit.location().mapLocation().getX() != gameController.startingMap(Planet.Earth).getWidth()
                                && currentUnit.location().mapLocation().getY() != gameController.startingMap(Planet.Earth).getHeight()) {
                            gameController.blueprint(currentUnit.id(), UnitType.Factory, direction);
                            //System.out.println("Blueprinted a factory!!!");
                        }
                    }
                }
            }

        }

        Location location = currentUnit.location();
        if (location.isOnMap()) {

            VecUnit nearbyUnits = gameController.senseNearbyUnits(location.mapLocation(), (long) Math.sqrt(currentUnit.visionRange()));
            //System.out.println("Size of nearbyUnits: " + nearbyUnits.size() + " | current unit type: " + currentUnit.unitType());
            for (long x = 0; x < nearbyUnits.size(); x++) {
                Unit nearbyUnit = nearbyUnits.get(x);

                if (currentUnit.unitType() == UnitType.Worker && gameController.canBuild(currentUnit.id(), nearbyUnit.id())) {

                    gameController.build(currentUnit.id(), nearbyUnit.id());
                    //System.out.println("Building a Factory!");
                    continue;

                } else if (nearbyUnit.team() != myTeam) {

                    Direction runAwayDir = nearbyUnit.location().mapLocation().directionTo(currentUnit.location().mapLocation());

                    move(currentUnit.id(), runAwayDir);

                    //System.out.println("Attacked a unit");
                    continue;

                } else {
                    //tryToMoveInAnyDirection(currentUnit);
                }
            }

        }

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

        if (factoryToBuildMapLoc != null) {
            Direction direction = currentUnit.location().mapLocation().directionTo(factoryToBuildMapLoc);
            move(currentUnit.id(), direction);
        }

        // if we can still move, that means we haven't moved at all
        if (gameController.isMoveReady(currentUnit.id())) {
            MapLocation targetLocation = null;
            VecMapLocation vecMapLocation = gameController.allLocationsWithin(currentUnit.location().mapLocation(), (long) Math.sqrt(currentUnit.visionRange()) );
            for (long a = 0; a < vecMapLocation.size(); a++) {
                MapLocation mapLoc = vecMapLocation.get(a);

                if (gameController.karboniteAt(mapLoc) > 0 && !gameController.hasUnitAtLocation(mapLoc)) {
                    if (targetLocation == null) {

                        targetLocation = mapLoc;

                    } else if (gameController.karboniteAt(targetLocation) < gameController.karboniteAt(mapLoc)) {
                        targetLocation = mapLoc;
                    }
                }

            }

            if (targetLocation != null)
                move(currentUnit.id(), currentUnit.location().mapLocation().directionTo(targetLocation));
            else
                tryToMoveInAnyDirection(currentUnit);
        }
    }

    private static void harvest (Unit unit) {
        if (unit.unitType() != UnitType.Worker)
            return;

        // harvesting code here
        //System.out.println("In harvesting code");
        for (Direction direction: Direction.values()) {
            if (gameController.canHarvest(unit.id(), direction)) {
                gameController.harvest(unit.id(), direction);
                //System.out.println("Harvested some karbonite!");
            }
        }
    }

    public static <T extends Enum<?>> T randomEnum(Class<T> clazz){
        int x = random.nextInt(clazz.getEnumConstants().length);
        return clazz.getEnumConstants()[x];
    }

    private static void updateMaps (VecUnit units) {
        workerMap = new HashMap<>();
        factoryMap = new HashMap<>();
        knightMap = new HashMap<>();
        rangerMap = new HashMap<>();


        for (long i = 0; i < units.size(); i++) {
            Unit unit = units.get(i);

            if (unit.unitType() == UnitType.Factory && !factoryMap.containsKey(unit.id())) {
                factoryMap.put(unit.id(), unit);
            } else if (unit.unitType() == UnitType.Worker && !workerMap.containsKey(unit.id())) {
                workerMap.put(unit.id(), unit);
            } else if (unit.unitType() == UnitType.Knight && !knightMap.containsKey(unit.id())) {
                knightMap.put(unit.id(), unit);
            } else if (unit.unitType() == UnitType.Ranger && !rangerMap.containsKey(unit.id())) {
                rangerMap.put(unit.id(), unit);
            }
        }
    }

    private static void tryToMoveInAnyDirection (Unit unit) {
        for (Direction direction : Direction.values()) {
            if (move(unit.id(), direction)) {
                //System.out.println("Moved the unit");
                break;
            }
        }
    }

    private static boolean canMove(int id, Direction direction) {
        return gameController.isMoveReady(id) && gameController.canMove(id, direction) && gameController.unit(id).location().isOnMap();
    }

    private static boolean move(int id, Direction direction) {
        if (canMove(id, direction)) {
            gameController.moveRobot(id, direction);

            return true;
        }

        return false;
    }

    private static boolean replicate (Unit unit, Direction direction) {
        if (unit.unitType() == UnitType.Worker) {
            if (gameController.canReplicate(unit.id(), direction)){
                gameController.replicate(unit.id(), direction);

                return true;
            }

        }

        return false;
    }

    private static int unitCountOnPlanet(Map<Integer, Unit> map, Planet planet) {
        int counter = 0;
        for (Unit unit: map.values()) {
            if (unit.location().isOnPlanet(planet)) {
                counter++;
            }
        }

        return counter;
    }
}
