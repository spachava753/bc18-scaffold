import bc.*;

import java.security.SecureRandom;
import java.util.LinkedList;
import java.util.Queue;

public class Player {
    private static GameController gameController = null;
    private static Team myTeam = null;
    private static Team enemyTeam = null;
    private static SecureRandom random = new SecureRandom();
    private static Queue<UnitType> buildQueue = new LinkedList<>();
    private static Unit rocket = null;

    public static void main (String[] args) {
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

        // request some extra workers
        /*
        for (int i = 0; i < 20; i++) {
            buildQueue.add(UnitType.Worker);
        }
        */
        // request some extra units
        for (int i = 0; i < 20; i++) {
            buildQueue.add(UnitType.Worker);
        }

        for (int i = 0; i < 20; i++) {
            buildQueue.add(UnitType.Knight);
        }

        for (int i = 0; i < 20; i++) {
            buildQueue.add(UnitType.Worker);
        }

        for (int i = 0; i < 50; i++) {
            buildQueue.add(UnitType.Knight);
        }

        while (true) {
            try {
                //System.out.print("round: " + gameController.round());
                //System.out.println(", time left: " + gameController.getTimeLeftMs());

                //System.out.println("We have " + gameController.karbonite() + " karbonite");

                //System.out.println("Build queue size is: " + buildQueue.size());

                VecUnit vecUnits = gameController.myUnits();
                //System.out.println("We have a total of " + vecUnits.size() + " units");

                ////System.out.println("In factory code");
                for (long i = 0; i < vecUnits.size(); i++) {

                    Unit currentUnit = vecUnits.get(i);
                    //System.out.println("Current unit is a: " + currentUnit.unitType());

                    if (currentUnit.unitType() == UnitType.Rocket && rocket == null) {
                        rocket = currentUnit;
                    }

                    // first factory logic
                    if (currentUnit.unitType() == UnitType.Factory) {

                        // get all the unit ids in the garrison
                        VecUnitID vecUnitID = currentUnit.structureGarrison();
                        if (vecUnitID.size() > 0) {

                            Direction direction = randomEnum(Direction.class);
                            ////System.out.print("The random direction was " + direction);

                            if (gameController.canUnload(currentUnit.id(), direction)) {

                                gameController.unload(currentUnit.id(), direction);
                                ////System.out.println("Unloaded a knight!");
                                continue;

                            }

                        } else if (buildQueue.size() > 0 && gameController.canProduceRobot(currentUnit.id(), buildQueue.peek())) {

                            gameController.produceRobot(currentUnit.id(), buildQueue.poll());
                            ////System.out.println("Produced a Knight!");

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

                    // replicating code here
                    /*
                    //System.out.println("In replicating code");
                    for (Direction direction: Direction.values()) {
                        if (gameController.canReplicate(currentUnit.id(), direction)) {
                            gameController.replicate(currentUnit.id(), direction);
                            //System.out.println("Replicated a worker!");
                            break;
                        }
                    }
                    */

                    // building and attacking code here
                    ////System.out.println("In building and attacking code");
                    Location location = currentUnit.location();
                    if (location.isOnMap()) {

                        VecUnit nearbyUnits = gameController.senseNearbyUnits(location.mapLocation(), 2);
                        ////System.out.println("Size of nearbyUnits: " + nearbyUnits.size() + " | current unit type: " + currentUnit.unitType());
                        for (long x = 0; x < nearbyUnits.size(); x++) {
                            Unit nearbyUnit = nearbyUnits.get(x);

                            if (currentUnit.unitType() == UnitType.Worker && gameController.canBuild(currentUnit.id(), nearbyUnit.id())) {

                                gameController.build(currentUnit.id(), nearbyUnit.id());
                                ////System.out.println("Building a Factory!");
                                continue;

                            } else if (nearbyUnit.team() != myTeam && gameController.isAttackReady(currentUnit.id()) && gameController.canAttack(currentUnit.id(), nearbyUnit.id())) {

                                gameController.attack(currentUnit.id(), nearbyUnit.id());
                                ////System.out.println("Attacked a unit");
                                continue;

                            }
                        }

                    }

                    // there were no units around
                    // blueprinting and moving code
                    ////System.out.println("In blueprinting and moving code");
                    Direction randDir = randomEnum(Direction.class);

                    if (gameController.karbonite() > bc.bcUnitTypeBlueprintCost(UnitType.Factory) && gameController.canBlueprint(currentUnit.id(), UnitType.Factory, randDir)) {

                        gameController.blueprint(currentUnit.id(), UnitType.Factory, randDir);
                        ////System.out.println("Blueprinted a factory!!!");

                    } else if (gameController.isMoveReady(currentUnit.id()) && gameController.canMove(currentUnit.id(), randDir)) {

                        gameController.moveRobot(currentUnit.id(), randDir);
                       //// System.out.println("Moved the unit");

                    }

                    // rocket code here
                    /*
                    //System.out.println("Rocket code here");
                    if (rocket == null) {
                        if (currentUnit.unitType() == UnitType.Worker) {
                            for (Direction direction : Direction.values()) {
                                if (gameController.canBlueprint(currentUnit.id(), UnitType.Rocket, direction)) {
                                    gameController.blueprint(currentUnit.id(), UnitType.Rocket, direction);
                                    //System.out.println("Blueprinted a rocket");
                                }
                            }
                        }
                    } else {
                        GameMap gameMap = new GameMap();
                        int y = (int) (Math.random() * gameMap.getMars_map().getHeight());
                        int x = (int) (Math.random() * gameMap.getMars_map().getWidth());

                        //System.out.println("The mars cooridnates are " + x + " and " + y);
                        Location marsRocketLandingLoc = new Location();
                        marsRocketLandingLoc.mapLocation().setPlanet(Planet.Mars);
                        marsRocketLandingLoc.mapLocation().setX(x);
                        marsRocketLandingLoc.mapLocation().setX(y);

                        if (gameMap.getMars_map().isPassableTerrainAt(marsRocketLandingLoc.mapLocation()) == 1) {
                            if (gameController.canLaunchRocket(rocket.id(), marsRocketLandingLoc.mapLocation())) {
                                gameController.launchRocket(rocket.id(), marsRocketLandingLoc.mapLocation());
                                //System.out.println("Launched rocket");
                            }
                        }
                    }
                    */
                }


            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                ////System.out.println("Build queue size is: " + buildQueue.size());

                //System.out.println("---------------Finished Round-----------------");
                gameController.nextTurn();
            }
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
