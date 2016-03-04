package edu.cwru.sepia.agent.minimax;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.DirectedAction;
import edu.cwru.sepia.action.TargetedAction;
import edu.cwru.sepia.environment.model.state.*;
import edu.cwru.sepia.util.Direction;
import edu.cwru.sepia.util.Pair;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class stores all of the information the agent
 * needs to know about the state of the game. For example this
 * might include things like footmen HP and positions.
 * <p>
 * Add any information or methods you would like to this class,
 * but do not delete or change the signatures of the provided methods.
 */
public class GameState {

    /**
     * Used for caching distances so that A* does not need to be recalculated every turn.
     * Key is the location, and value is (distance, time_remaining).
     */
    private static Map<Pair<Integer, Integer>, Pair<Integer, Integer>> aStarCache = new ConcurrentHashMap<>();

    private boolean maxNode;
    private int xExtent;
    private int yExtent;

    private List<SimpleUnit> footmen;
    private List<SimpleUnit> archers;

    private List<MapLocation> resources;

    // PUBLIC FUNCTIONS

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

        // TODO: Get rid of repeated code
        List<SimpleUnit> footmen = new ArrayList<>();
        for (Unit.UnitView unit : state.getUnits(0)) {
            int id = unit.getID();
            int x = unit.getXPosition();
            int y = unit.getYPosition();
            int baseHealth = unit.getTemplateView().getBaseHealth();
            int currentHealth = unit.getHP();
            int basicAttack = unit.getTemplateView().getBasicAttack();
            int range = unit.getTemplateView().getRange();
            Queue<Pair<Integer, Integer>> previousLocations= new LinkedList<>();
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
            Queue<Pair<Integer, Integer>> previousLocations= new LinkedList<>();
            archers.add(new SimpleUnit(id, x, y, baseHealth, currentHealth, basicAttack, range));
        }
        this.archers = archers;

        List<MapLocation> resources = new ArrayList<>();
        for (ResourceNode.ResourceView view : state.getAllResourceNodes()) {
            resources.add(new MapLocation(view.getXPosition(), view.getYPosition()));
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

        updateCache();
    }

    /**
     * Updates the cache. If the time runs out for any entry, removes that entry from the Map.
     */
    private void updateCache() {
        for (Map.Entry<Pair<Integer, Integer>, Pair<Integer, Integer>> entry : GameState.aStarCache.entrySet()) {
            if (entry.getValue().b - 1 == 0) {
                GameState.aStarCache.remove(entry.getKey());
            } else {
                GameState.aStarCache.put(entry.getKey(), new Pair<>(entry.getValue().a, entry.getValue().b - 1));
            }
        }
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
        int previousLocFeature = 0;

        // distance to archers is bad
        // calculate aStar to closest archer
        for (SimpleUnit footman : footmen) {

            footmanFeature += footman.getCurrentHealth();

            int closest = 70000;
            SimpleUnit closeArch = null;
            for (SimpleUnit archer: archers) {
                int newDist = taxicab(footman.getLocation(), archer.getLocation());
                closeArch = newDist < closest ? archer : closeArch;
                closest = newDist < closest ? newDist : closest;

                archerFeature += archer.getCurrentHealth();
            }

            if (GameState.aStarCache.containsKey(footman.getLocation())) {
                System.out.println("CACHING!");
                distanceFeature += GameState.aStarCache.get(footman.getLocation()).a;
            } else {
                int distance = aStarDistance(new MapLocation(footman), new MapLocation(closeArch));
                distanceFeature += distance;
                GameState.aStarCache.put(footman.getLocation(), new Pair<>(distance, 3));
            }
        }

        // archer distance from each other
        for (SimpleUnit archer : archers) {
            for (SimpleUnit archer2 : archers) {
                archDistFeature += taxicab(archer.getLocation(), archer2.getLocation());
            }
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

        //add utilities

        utility -= archerFeature;
        utility += footmanFeature;
        utility -= distanceFeature * 5;
        utility -= archDistFeature;
        utility -= wallDistFeature;
        utility += rowFeature;
        utility += columnFeature;

        //System.out.println(toString());

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

    // PRIVATE FUNCTIONS

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
                    resources.contains(new MapLocation(newX, newY)) ||
                    unitIsLocated(new Pair<>(newX, newY)))) {
                allPossibleActions.add(Action.createPrimitiveMove(unit.getId(), direction));
            }
        }

        // Find all attack actions
        List<Integer> possibleTargets = findAllPossibleTargets(unit.getLocation(), unit.getRange(), enemyUnits);

        for (int target : possibleTargets) {
            allPossibleActions.add(Action.createPrimitiveAttack(unit.getId(), target));
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
     * @param unit      The unit, may be null
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
     * Finds all possible targets for a given unit based on their location.
     * @param location The location of the attacking unit
     * @param range    The range of the attacking unit
     * @param enemies  The list of enemies who could possibly be attacked
     * @return List of tuples (targetID, range)
     */
    private List<Integer> findAllPossibleTargets(Pair<Integer, Integer> location, int range, List<SimpleUnit> enemies) {
        List<Integer> possibleTargets = new ArrayList<>();
        for (SimpleUnit enemy : enemies) {
            Pair<Integer, Integer> enemyLocation = enemy.getLocation();
            if (range >= Math.abs(location.a - enemyLocation.a) &&
                    range >= Math.abs(location.b - enemyLocation.b)) {
                possibleTargets.add(enemy.getId());
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

    /**
     * This calculates the distance of a path between two locations
     * @param start The beginning location
     * @param goal  The goal location
     * @return      The distance bewteen the two locations
     */
    private int aStarDistance(MapLocation start, MapLocation goal) {

        // search Lists
        Comparator comparator = pathComparator(start, goal);
        PriorityQueue<Stack<MapLocation>> openList =
                new PriorityQueue<>(11, comparator);

        Set<MapLocation> closedList = new HashSet<>();
        Stack<MapLocation> finalPath = new Stack<>();
        Stack<MapLocation> initialPath = new Stack<>();

        initialPath.add(start);
        openList.add(initialPath);

        // The search
        while (!openList.isEmpty()) {

            Stack<MapLocation> currentPath = openList.poll();
            MapLocation currentLoc = currentPath.peek();

            if (currentLoc.equals(goal)) {
                // found the goal
                currentPath.pop();
                while (!currentPath.isEmpty()) {
                    finalPath.push(currentPath.pop());
                }
                break;
            } else {
                // didn't find the goal
                MapLocation[] successors =
                        expandState(currentLoc);

                closedList.add(currentLoc);

                for (MapLocation successor : successors) {

                    // System.out.println(successor + " : " + enemyFootmanLoc);

                    if (successor != null && !closedList.contains(successor) &&
                            !resources.contains(successor) &&
                            !alreadyPath(openList, successor) &&
                            !resources.contains(successor)) {

                        Stack<MapLocation> newPath =
                                (Stack<MapLocation>) currentPath.clone();
                        newPath.push(successor);
                        openList.add(newPath);
                    }
                }

            }
        }

        if (finalPath.isEmpty()) {
            System.out.println("No Avaliable path");
            return 0;
        }

        // return the path
        finalPath.pop();
        return finalPath.size();
    }

    /**
     * returns all of the successor nodes of a MapLocation
     *
     * @param loc   The location to expand
     * @return      The successor locations
     */
    private MapLocation[] expandState(MapLocation loc) {

        MapLocation north       = new MapLocation(loc.x, loc.y - 1);
        MapLocation south       = new MapLocation(loc.x, loc.y + 1);
        MapLocation west        = new MapLocation(loc.x - 1, loc.y);
        MapLocation east        = new MapLocation(loc.x + 1, loc.y);
        MapLocation[] successors = {north, south, east, west};

        for (int i = 0; i < successors.length; i++) {
            if ( successors[i].x < 0 || successors[i].x > xExtent ||
                    successors[i].y < 0 || successors[i].y > yExtent ||
                    resources.contains(successors[i])) {

                successors[i] = null;
            }
        }

        return successors;
    }

    /**
     * Checks the open list to see if a path already has reached the location
     *
     * @param openList  the open list
     * @param location  the location
     * @return          Whether there is already a path
     */
    private boolean alreadyPath(PriorityQueue<Stack<MapLocation>> openList, MapLocation location) {
        for (Stack<MapLocation> path : openList) {
            if (path.peek().equals(location)) {
                return true;
            }
        }

        return false;
    }

    /**
     * returns a comparator for lists of locations.
     *
     * @return the comparator
     */
    private Comparator pathComparator(MapLocation start, MapLocation goal) {

        // class that compares two MapLocations by their heuristic
        class PathComparator implements Comparator<Stack<MapLocation>> {

            MapLocation start;
            MapLocation goal;

            public PathComparator(MapLocation start, MapLocation goal) {
                this.start = start;
                this.goal = goal;
            }

            public int compare(Stack<MapLocation> o1, Stack<MapLocation> o2) {

                int dist1 = o1.size();
                int dist2 = o2.size();
                int taxicab1 = taxicab(o1.peek().getLocation(), goal.getLocation());
                int taxicab2 = taxicab(o2.peek().getLocation(), goal.getLocation());

                if (taxicab1 + dist1 == taxicab2 + dist2) {
                    return 0;
                }

                return  dist1 + taxicab1 < dist2 + taxicab2 ? -1 : 1;

            }

            public boolean equals(Object obj) {
                return false;
            }
        }

        return new PathComparator(start, goal);
    }

    // GETTERS / SETTERS

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

    public List<MapLocation> getResources() {
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

    // CLASSES

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
            return new Pair<>(x, y);
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
            String builder = "";
            builder += "location: (" + x + ", " + y + ")\n";
            builder += "health: " + currentHealth + " / " + baseHealth + "\n";

            return builder;
        }
    }

    /**
     * Simple class to hold the locations of resources.
     */
    public class MapLocation {
        public int x;
        public int y;

        public MapLocation(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public MapLocation(SimpleUnit unit) {
            this.x = unit.x;
            this.y = unit.y;
        }

        public Pair<Integer, Integer> getLocation() {
            return new Pair<>(x, y);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MapLocation that = (MapLocation) o;

            if (x != that.x) return false;
            return y == that.y;
        }
    }
}
