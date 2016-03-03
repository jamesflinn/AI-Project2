package edu.cwru.sepia.agent.minimax;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionType;
import edu.cwru.sepia.action.DirectedAction;
import edu.cwru.sepia.action.TargetedAction;
import edu.cwru.sepia.environment.model.state.*;
import edu.cwru.sepia.util.Direction;
import edu.cwru.sepia.util.Pair;

import java.util.*;

/**
 * This class stores all of the information the agent
 * needs to know about the state of the game. For example this
 * might include things like footmen HP and positions.
 * <p>
 * Add any information or methods you would like to this class,
 * but do not delete or change the signatures of the provided methods.
 */
public class GameState {

    private boolean maxNode;
    private int xExtent;
    private int yExtent;

    private List<SimpleUnit> footmen;
    private List<SimpleUnit> archers;

    private List<ResourceLocation> resources;

    /**
     * You will implement this constructor. It will
     * extract all of the needed state information from the built in
     * SEPIA state view.
     * <p>
     * You may find the following state methods useful:
     * <p>
     * state.getXExtent() and state.getYExtent(): get the map dimensions
     * state.getAllResourceIDs(): returns all of the obstacles in the map
     * state.getResourceNode(Integer resourceID): Return a ResourceView for the given ID
     * <p>
     * For a given ResourceView you can query the position using
     * resource.getXPosition() and resource.getYPosition()
     * <p>
     * For a given unit you will need to find the attack damage, range and max HP
     * unitView.getTemplateView().getRange(): This gives you the attack range
     * unitView.getTemplateView().getBasicAttack(): The amount of damage this unit deals
     * unitView.getTemplateView().getBaseHealth(): The maximum amount of health of this unit
     *
     * @param state Current state of the episode
     */
    public GameState(State.StateView state) {

        this.maxNode = true;

        this.xExtent = state.getXExtent();
        this.yExtent = state.getYExtent();

        List<SimpleUnit> footmen = new ArrayList<>();
        for (Unit.UnitView unit : state.getUnits(0)) {
            int id = unit.getID();
            int x = unit.getXPosition();
            int y = unit.getYPosition();
            int baseHealth = unit.getTemplateView().getBaseHealth();
            int currentHealth = unit.getHP();
            int basicAttack = unit.getTemplateView().getBasicAttack();
            int range = unit.getTemplateView().getRange();
            footmen.add(new SimpleUnit(id, x, y, baseHealth, currentHealth, basicAttack, range));
        }
        this.footmen = footmen;

        List<SimpleUnit> archers = new ArrayList<>();
        for (Unit.UnitView unit : state.getUnits(1)) {
            int id = unit.getID();
            int x = unit.getXPosition();
            int y = unit.getYPosition();
            int baseHealth = unit.getTemplateView().getBaseHealth();
            int currentHealth = unit.getHP();
            int basicAttack = unit.getTemplateView().getBasicAttack();
            int range = unit.getTemplateView().getRange();
            archers.add(new SimpleUnit(id, x, y, baseHealth, currentHealth, basicAttack, range));
        }
        this.archers = archers;

        List<ResourceLocation> resources = new ArrayList<>();
        for (ResourceNode.ResourceView view : state.getAllResourceNodes()) {
            resources.add(new ResourceLocation(view.getXPosition(), view.getYPosition()));
        }
        this.resources = resources;
    }

    public GameState(GameState oldGameState, List<SimpleUnit> footmen, List<SimpleUnit> archers) {
        this.xExtent = oldGameState.getXExtent();
        this.yExtent = oldGameState.getYExtent();
        this.resources = oldGameState.getResources();
        this.maxNode = !oldGameState.maxNode;

        this.footmen = footmen;
        this.archers = archers;
    }

    /**
     * You will implement this function.
     * <p>
     * You should use weighted linear combination of features.
     * The features may be primitives from the state (such as hp of a unit)
     * or they may be higher level summaries of information from the state such
     * as distance to a specific location. Come up with whatever features you think
     * are useful and weight them appropriately.
     * <p>
     * It is recommended that you start simple until you have your algorithm working. Then watch
     * your agent play and try to add features that correct mistakes it makes. However, remember that
     * your features should be as fast as possible to compute. If the features are slow then you will be
     * able to do less plys in a turn.
     * <p>
     *  TODO Add a good comment about what is in your utility and why you chose those features.
     *
     * @return The weighted linear combination of the features
     */
    public double getUtility() {

        // TODO make this faster
        // highly unoptimized right now
        int utility = 0;

        int archerFeature   = 0;
        int footmanFeature  = 0;
        int distanceFeature = 0;
        int minDistFeature  = 70000;
        int archDistFeature = 0;
        int wallDistFeature = 0;
        int columnFeature   = 0;
        int rowFeature      = 0;
        int obstacleFeature = 0;

        // archers are bad
        for (SimpleUnit archer : archers) {
            archerFeature += archer.getCurrentHealth();
        }

        // footmen are good
        for (SimpleUnit footman : footmen) {
            footmanFeature += footman.getCurrentHealth();
        }

        // distance to archers is bad
        for (SimpleUnit footman : footmen) {
            for (SimpleUnit archer : archers) {
                distanceFeature += taxicab(footman.getLocation(), archer.getLocation());
            }
        }

        // minium distance to one archer
        for (SimpleUnit archer : archers) {
            int dist = 0;
            // minimum distance to one archer
            for (SimpleUnit footman : footmen) {
                dist += taxicab(footman.getLocation(), archer.getLocation());
            }
            minDistFeature = dist < minDistFeature ? dist : minDistFeature;
        }

        // archer distance from each other
        for (SimpleUnit archer : archers) {
            for (SimpleUnit archer2 : archers) {
                archDistFeature += taxicab(archer.getLocation(), archer2.getLocation());
            }
        }

        // archer distance to walls
        for (SimpleUnit archer : archers) {
            wallDistFeature += distanceToWalls(archer);
        }

        // don't want footmen on same row / column
        for (SimpleUnit footman : footmen) {
            for (SimpleUnit footman2 : footmen) {
                if (!footman.equals(footman2)) {
                    columnFeature -= footman.y == footman2.y ? 1 : 0;
                    rowFeature    -= footman.x == footman2.x ? 1 : 0;
                }
            }
        }

//        // don't want resources on same row / column
//        for (SimpleUnit footman : footmen) {
//            for (ResourceLocation resource : resources) {
//                obstacleFeature += resource.x == footman.getX() || resource.y == footman.getY() ? 5 : 0;
//            }
//        }

//        // if we are next to a resource then very bad
//        for (SimpleUnit footman : footmen) {
//            for (ResourceLocation resource : resources) {
//                if (taxicab(resource.getLocation(), footman.getLocation()) == 1) {
//                    obstacleFeature += 10000;
//                }
//            }
//        }

//        // if we are close to obstacles then bad?
//        for (SimpleUnit footman : footmen) {
//            int closestObstacle = 0;
//            for (ResourceLocation resource : resources) {
//                obstacleFeature += taxicab(footman.getLocation(), resource.getLocation());
//            }
//        }

//        // closest obstacle is bad
//        for (SimpleUnit footman : footmen) {
//            int closestObstacle = 0;
//            int newDist = 0;
//            for (ResourceLocation resource : resources) {
//                newDist = taxicab(footman.getLocation(), resource.getLocation());
//                closestObstacle = newDist > closestObstacle ? closestObstacle : newDist;
//            }
//
//            obstacleFeature += closestObstacle;
//        }

        //add utilities

        utility -= archerFeature;
        utility += footmanFeature;
        utility -= distanceFeature;
        utility -= minDistFeature;
        utility -= archDistFeature;
        utility -= wallDistFeature;
        utility += rowFeature;
        utility += columnFeature;
        utility -= obstacleFeature;

        return utility;
    }

    /**
     * You will implement this function.
     * <p>
     * This will return a list of GameStateChild objects. You will generate all of the possible
     * actions in a step and then determine the resulting game state from that action. These are your GameStateChildren.
     * <p>
     * You may find it useful to iterate over all the different directions in SEPIA.
     * <p>
     * for(Direction direction : Directions.values())
     * <p>
     * To get the resulting position from a move in that direction you can do the following
     * x += direction.xComponent()
     * y += direction.yComponent()
     *
     * @return All possible actions and their associated resulting game state
     */
    public List<GameStateChild> getChildren() {
        List<GameStateChild> children;

        if (maxNode) {
            // footmen
            children = getGameStateChildren(footmen, archers);
        } else {
            // archers
            children = getGameStateChildren(archers, footmen);
        }

        return children;
    }

    public List<GameStateChild> getGameStateChildren(List<SimpleUnit> units, List<SimpleUnit> enemyUnits) {
        List<GameStateChild> children = new ArrayList<>();
        List<List<Action>> unitActionsList = new ArrayList<>();
        // Calculate all possible actions
        for (SimpleUnit unit : units) {
            unitActionsList.add(findAllActionsForUnit(unit, enemyUnits));
        }

        // Cartesian product of all unit's actions
        List<List<Action>> actionsList = cartesianProduct(unitActionsList);

        // Get new state from actions
        for (List<Action> actions : actionsList) {
            List<SimpleUnit> newEnemyUnits;
            if (enemyUnits.equals(footmen)) {
                newEnemyUnits = new ArrayList<>(footmen);
            } else {
                newEnemyUnits = new ArrayList<>(archers);
            }

            List<SimpleUnit> newUnitList = new ArrayList<>();
            for (Action action : actions) {
                SimpleUnit unitFromAction = findUnitById(action.getUnitId());
                Pair<SimpleUnit, SimpleUnit> unitPair = createNewUnitFromAction(unitFromAction, action);
                newUnitList.add(unitPair.a);
                newEnemyUnits = createNewEnemyList(unitPair.b, newEnemyUnits);
            }

            GameState newState;
            if (enemyUnits.equals(footmen)) {
                newState = new GameState(this, newEnemyUnits, newUnitList);
            } else {
                newState = new GameState(this, newUnitList, newEnemyUnits);
            }

            Map<Integer, Action> actionMap = new HashMap<>();
            for (Action action : actions) {
                actionMap.put(action.getUnitId(), action);
            }

            children.add(new GameStateChild(actionMap, newState));
        }


        return children;
    }

    /**
     * Finds all possible actions for a given unit
     * @param unit The unit whose actions are being found
     * @param enemyUnits The enemy units
     * @return A list of all possible actions
     */
    private List<Action> findAllActionsForUnit(SimpleUnit unit, List<SimpleUnit> enemyUnits) {
        List<Action> allPossibleActions = new ArrayList<>();

        // Find all move actions
        for (Direction direction : Direction.values()) {
            // Only move up, down, left, or right
            if (direction.equals(Direction.NORTHEAST) ||
                    direction.equals(Direction.NORTHWEST) ||
                    direction.equals(Direction.SOUTHEAST) ||
                    direction.equals(Direction.SOUTHWEST)) {
                continue;
            }

            int newX = unit.getX() + direction.xComponent();
            int newY = unit.getY() + direction.yComponent();

            if (!(newX >= xExtent ||
                    newY >= yExtent ||
                    newX < 0 ||
                    newY < 0 ||
                    resources.contains(new ResourceLocation(newX, newY)) ||
                    unitIsLocated(new Pair<>(newX, newY)))) {
                allPossibleActions.add(Action.createPrimitiveMove(unit.getId(), direction));
            }
        }

        // Find all attack actions
        List<Pair<Integer, Integer>> possibleTargets = findAllPossibleTargets(unit.getLocation(), enemyUnits);

        for (Pair<Integer, Integer> possibleTarget : possibleTargets) {
            int targetId = possibleTarget.a;
            int distance = possibleTarget.b;
            if (unit.getRange() >= distance) {
                allPossibleActions.add(Action.createPrimitiveAttack(unit.getId(), targetId));
            }
        }

        return allPossibleActions;
    }

    /**
     * Finds the cartesian product of each units's actions
     * @param actionsList A list of each unit's actions
     * @return The cartesian product of actions
     */
    private List<List<Action>> cartesianProduct(List<List<Action>> actionsList) {
        List<List<Action>> combinations = new ArrayList<>();
        for (List<Action> actions : actionsList) {
            List<List<Action>> extraColumnCombinations = new ArrayList<>();
            for (Action action : actions) {
                if (combinations.isEmpty()) {
                    extraColumnCombinations.add(Arrays.asList(action));
                } else {
                    for (List<Action> productList : combinations) {
                        List<Action> newProductList = new ArrayList<>(productList);
                        newProductList.add(action);
                        extraColumnCombinations.add(newProductList);
                    }
                }
            }
            combinations = extraColumnCombinations;
        }
        return combinations;
    }

    /**
     * Creates new units from a given action.
     * @param unit The unit performing the action
     * @param action The action being performed
     * @return A pair of units, the first being the unit after performing the action,
     *         and the second being the unit that was targeted, null if no unit was targeted
     */
    public Pair<SimpleUnit, SimpleUnit> createNewUnitFromAction(SimpleUnit unit, Action action) {
        if (action instanceof DirectedAction) {
            DirectedAction directedAction = (DirectedAction) action;
            int newX = unit.getX() + directedAction.getDirection().xComponent();
            int newY = unit.getY() + directedAction.getDirection().yComponent();

            SimpleUnit newUnit = new SimpleUnit(unit.getId(),
                    newX,
                    newY,
                    unit.getBaseHealth(),
                    unit.getCurrentHealth(),
                    unit.getBasicAttack(),
                    unit.getRange());

            return new Pair<>(newUnit, null);

        } else if (action instanceof TargetedAction) {
            TargetedAction targetedAction = (TargetedAction) action;

            // Find enemy unit
            SimpleUnit targetedUnit = null;
            for (SimpleUnit footman : this.footmen) {
                if (footman.getId() == targetedAction.getTargetId()) {
                    targetedUnit = footman;
                }
            }

            for (SimpleUnit archer : this.archers) {
                if (archer.getId() == targetedAction.getTargetId()) {
                    targetedUnit = archer;
                }
            }

            SimpleUnit newTargetedUnit = new SimpleUnit(targetedUnit.getId(),
                    targetedUnit.getX(),
                    targetedUnit.getY(),
                    targetedUnit.getBaseHealth(),
                    targetedUnit.getCurrentHealth() - unit.getBasicAttack(),
                    targetedUnit.getBasicAttack(),
                    targetedUnit.getRange());

            return new Pair<>(unit, newTargetedUnit);
        }

        // TODO: Throw exception?
        return null;
    }

    /**
     * Creates a new enemy list, updating the old one.
     * @param unit The unit, may be null
     * @param enemyList The list of enemy units that are to be updated
     * @return A updated enemy list
     */
    private List<SimpleUnit> createNewEnemyList(SimpleUnit unit, List<SimpleUnit> enemyList) {
        List<SimpleUnit> newEnemyList = new ArrayList<>(enemyList);

        if (unit != null) {
            int index = findIndexOfUnit(unit, enemyList);
            newEnemyList.remove(index);
            newEnemyList.add(unit);
        }
        return newEnemyList;
    }

    /**
     * Finds all possible targets for a given unit based on their location. Range is not factored.
     * @param location The location of the attacking unit
     * @param enemies The list of enemies who could possibly be attacked
     * @return List of tuples (targetID, range)
     */
    private List<Pair<Integer, Integer>> findAllPossibleTargets(Pair<Integer, Integer> location, List<SimpleUnit> enemies) {
        List<Pair<Integer, Integer>> possibleTargets = new ArrayList<>();
        for (SimpleUnit enemy : enemies) {
            if (Objects.equals(enemy.getLocation().a, location.a)) {
                // x coordinates are equal
                int distance = Math.abs(location.b - enemy.getLocation().b);
                possibleTargets.add(new Pair<>(enemy.getId(), distance));
            } else if (Objects.equals(enemy.getLocation().b, location.b)) {
                // y coordinates are equal
                int distance = Math.abs(location.a - enemy.getLocation().a);
                possibleTargets.add(new Pair<>(enemy.getId(), distance));
            }
        }
        return possibleTargets;
    }

    /**
     * Checks if a unit is located at the specified location
     * @param location the location being checked
     * @return true if there is a unit located at location
     */
    private boolean unitIsLocated(Pair<Integer, Integer> location) {
        for (SimpleUnit footman : footmen) {
            if (footman.getLocation().equals(location)) {
                return true;
            }
        }

        for (SimpleUnit archer : archers) {
            if (archer.getLocation().equals(location)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Finds a unit from the footmen and archers list and returns it.
     * @param id the id of the unit
     * @return the SimpleUnit
     */
    private SimpleUnit findUnitById(int id) {
        for (SimpleUnit unit : footmen) {
            if (unit.getId() == id) {
                return unit;
            }
        }

        for (SimpleUnit unit : archers) {
            if (unit.getId() == id) {
                return unit;
            }
        }

        return null;
    }

    /**
     * Finds the index of the unit by id.
     * @param unit the unit being searched for
     * @param unitList the list the unit is contained in
     * @return the index of the unit in the unitList
     */
    private int findIndexOfUnit(SimpleUnit unit, List<SimpleUnit> unitList) {
        int index = 0;
        for (SimpleUnit newUnit : unitList) {
            if (unit.getId() == newUnit.getId()) {
                return index;
            }
            index += 1;
        }
        return -1;
    }

    public boolean getMaxNode() {
        return this.maxNode;
    }

    public int getXExtent() {
        return xExtent;
    }

    public int getYExtent() {
        return yExtent;
    }

    public List<SimpleUnit> getFootmen() {
        return footmen;
    }

    public List<SimpleUnit> getArchers() {
        return archers;
    }

    public List<ResourceLocation> getResources() {
        return resources;
    }

    public String toString() {

        StringBuilder builder = new StringBuilder();

        builder.append("FOOTMEN:\n");
        for (SimpleUnit footman : footmen) {
            builder.append(footman.toString());
        }

        builder.append("\nARCHERS:\n");
        for (SimpleUnit archer : archers) {
            builder.append(archer.toString());
        }

        return builder.toString();
    }

    /**
     * Computes the taxicab distance between two units
     *
     * @param first  The first unit
     * @param second The second unit
     * @return       The taxicab distance between the two units.
     */
    private int taxicab(Pair<Integer, Integer> first, Pair<Integer, Integer> second) {

        int deltaX = Math.abs(first.a - second.a);
        int deltaY = Math.abs(first.b - second.b);

        return deltaX + deltaY;

    }

    /**
     * This calculates the distance the the two closest walls for a unit
     * @param unit the unit we want the distance from
     * @return     the distance to the two closest walls summed
     */
    private int distanceToWalls(SimpleUnit unit) {

        int distToYWall = Math.min(unit.x, xExtent - unit.x);
        int distToXWall = Math.min(unit.y, yExtent - unit.y);

        return distToXWall + distToYWall;

    }

    /**
     * Represents a unit, but only has the fields necessary for the Minimax algorithm.
     */
    public class SimpleUnit {
        private int id;
        private int x;
        private int y;

        private int baseHealth;
        private int currentHealth;

        private int basicAttack;
        private int range;

        public SimpleUnit(int id, int x, int y, int baseHealth, int currentHealth, int basicAttack, int range) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.baseHealth = baseHealth;
            this.currentHealth = currentHealth;
            this.basicAttack = basicAttack;
            this.range = range;
        }

        /**
         * Returns true if this unit is located at (x, y)
         * @param x the x coordinate
         * @param y the y coordinate
         * @return true if this is located at (x, y)
         */
        public boolean isLocated(int x, int y) {
            return this.x == x && this.y == y;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            SimpleUnit that = (SimpleUnit) o;

            if (id != that.getId()) return false;
            if (x != that.getX()) return false;
            if (y != that.getY()) return false;
            if (baseHealth != that.getBaseHealth()) return false;
            if (currentHealth != that.getCurrentHealth()) return false;
            if (basicAttack != that.getBasicAttack()) return false;
            return range == that.getRange();
        }

        public int getId() {
            return id;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public Pair<Integer, Integer> getLocation() {
            return new Pair<Integer, Integer>(x, y);
        }

        public int getBaseHealth() {
            return baseHealth;
        }

        public int getCurrentHealth() {
            return currentHealth;
        }

        public int getBasicAttack() {
            return basicAttack;
        }

        public int getRange() {
            return range;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("location: (").append(x).append(", ").append(y).append(")\n");
            builder.append("health: ").append(currentHealth).append(" / ").append(baseHealth).append("\n");

            return builder.toString();
        }
    }

    /**
     * Simple class to hold the locations of resources.
     */
    public class ResourceLocation {
        public int x;
        public int y;

        public ResourceLocation(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public Pair<Integer, Integer> getLocation() {
            return new Pair<Integer, Integer>(x, y);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ResourceLocation that = (ResourceLocation) o;

            if (x != that.x) return false;
            return y == that.y;
        }
    }
}
