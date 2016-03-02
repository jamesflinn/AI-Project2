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

    private int turnNumber;
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

        this.turnNumber = state.getTurnNumber();

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
        this.turnNumber = oldGameState.getTurnNumber() + 1;

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

        int utility = 0;
        // archers are bad
        for (SimpleUnit archer : archers) {
            utility += -5 * archer.getCurrentHealth();
        }

        // footmen are good
        for (SimpleUnit footman : footmen) {
            utility += 5 * footman.getCurrentHealth();
            for (SimpleUnit archer : archers) {
                utility -= taxicab(footman, archer);
            }
        }

        // distance to archers is bad
        // Heuristics based upon units
        for (SimpleUnit footman : footmen) {

            // give each state a value based upon distance the footman are from the archers
            for (GameState.SimpleUnit archer : archers) {
                utility -= taxicab(footman, archer);
            }

            // if we are moving into a resource then very bad
            for (GameState.ResourceLocation resource : resources) {
                if (resource.getLocation().equals(footman.getLocation())) {
                    utility -= 10000;
                }
            }
        }

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

        if (turnNumber % 2 == 0) {
            // footmen
            children = getGameStateChildren(footmen, archers);
        } else {
            // archers
            children = getGameStateChildren(archers, footmen);
        }

        return children;
    }

    public List<GameStateChild> getGameStateChildren(List<SimpleUnit> units, List<SimpleUnit> enemyUnits) {
        // TODO: This only works when there are 2 units for a player, must adjust for when there is only one
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
                    resources.contains(new ResourceLocation(newX, newY)))) {
                boolean noEnemy = true;
                for (SimpleUnit enemyUnit : enemyUnits) {
                    if (enemyUnit.isLocated(newX, newY)) {
                        // TODO: This only allows footmen to attack archers. Must include range, also movement must change
                        noEnemy = false;
                        allPossibleActions.add(Action.createPrimitiveAttack(unit.getId(), enemyUnit.getId()));
                    }
                }
                if (noEnemy) {
                    allPossibleActions.add(Action.createPrimitiveMove(unit.getId(), direction));
                }
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

    public int getTurnNumber() {
        return this.turnNumber;
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
    private int taxicab(GameState.SimpleUnit first, GameState.SimpleUnit second) {

        int deltaX = Math.abs(first.getX() - second.getX());
        int deltaY = Math.abs(first.getY() - second.getY());

        return deltaX + deltaY;

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

        @Override
        public String toString() {
            return "SimpleUnit{" +
                    "y=" + y +
                    ", id=" + id +
                    ", x=" + x +
                    '}';
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
