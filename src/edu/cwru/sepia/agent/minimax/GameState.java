package edu.cwru.sepia.agent.minimax;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionType;
import edu.cwru.sepia.action.DirectedAction;
import edu.cwru.sepia.action.TargetedAction;
import edu.cwru.sepia.environment.model.state.*;
import edu.cwru.sepia.util.Direction;

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

    private int playersTurn;
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
        this.xExtent = state.getXExtent();
        this.yExtent = state.getYExtent();

        List<SimpleUnit> footmen = new ArrayList<>();
        for (Unit.UnitView unit : state.getUnits(0)) {
            int x = unit.getXPosition();
            int y = unit.getYPosition();
            int baseHealth = unit.getTemplateView().getBaseHealth();
            int currentHealth = unit.getHP();
            int basicAttack = unit.getTemplateView().getBasicAttack();
            int range = unit.getTemplateView().getRange();
        }
        this.footmen = footmen;

        List<SimpleUnit> archers = new ArrayList<>();
        for (Unit.UnitView unit : state.getUnits(1)) {
            int x = unit.getXPosition();
            int y = unit.getYPosition();
            int baseHealth = unit.getTemplateView().getBaseHealth();
            int currentHealth = unit.getHP();
            int basicAttack = unit.getTemplateView().getBasicAttack();
            int range = unit.getTemplateView().getRange();
            archers.add(new SimpleUnit(x, y, baseHealth, currentHealth, basicAttack, range));
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
        this.playersTurn = oldGameState.getPlayersTurn() == 0 ? 1 : 0;

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
     * Add a good comment about what is in your utility and why you chose those features.
     *
     * @return The weighted linear combination of the features
     */
    public double getUtility() {
        return 0.0;
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
        List<GameStateChild> children = new ArrayList<>();
        if (playersTurn == 0) {
            for (SimpleUnit unit : footmen) {
                children.addAll(findAllPositionsForUnit(unit));
            }
        } else {
            for (SimpleUnit unit : archers) {
                children.addAll(findAllPositionsForUnit(unit));
            }
        }
        return children;
    }

    public List<GameStateChild> findAllPositionsForUnit(SimpleUnit unit) {
        List<GameStateChild> allPossiblePositions = new ArrayList<>();

        for (Direction direction : Direction.values()) {
            int newX = unit.getX() + direction.xComponent();
            int newY = unit.getY() + direction.yComponent();

            SimpleUnit newUnit = new SimpleUnit(newX,
                    newY,
                    unit.getBaseHealth(),
                    unit.getCurrentHealth(),
                    unit.getBasicAttack(),
                    unit.getRange());

            if (!(newX >= xExtent ||
                    newY >= yExtent ||
                    newX < 0 ||
                    newY < 0 ||
                    resources.contains(new ResourceLocation(newX, newY)))) {
                List<SimpleUnit> newUnitList;
                if (footmen.contains(unit)) {
                    newUnitList = new ArrayList<>(footmen);
                } else {
                    newUnitList = new ArrayList<>(archers);
                }
                newUnitList.remove(unit);
                newUnitList.add(newUnit);
            }
        }

        return allPossiblePositions;
    }

    public int getPlayersTurn() {
        return playersTurn;
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

    /**
     * Represents a unit, but only has the fields necessary for the Minimax algorithm.
     */
    private class SimpleUnit {
        private int x;
        private int y;

        private int baseHealth;
        private int currentHealth;

        private int basicAttack;
        private int range;

        public SimpleUnit(int x, int y, int baseHealth, int currentHealth, int basicAttack, int range) {
            this.x = x;
            this.y = y;
            this.baseHealth = baseHealth;
            this.currentHealth = currentHealth;
            this.basicAttack = basicAttack;
            this.range = range;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            SimpleUnit that = (SimpleUnit) o;

            if (x != that.x) return false;
            if (y != that.y) return false;
            if (baseHealth != that.baseHealth) return false;
            if (currentHealth != that.currentHealth) return false;
            if (basicAttack != that.basicAttack) return false;
            return range == that.range;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
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
    }

    /**
     * Simple class to hold the locations of resources.
     */
    private class ResourceLocation {
        public int x;
        public int y;

        public ResourceLocation(int x, int y) {
            this.x = x;
            this.y = y;
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
