package edu.cwru.sepia.agent.minimax;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.DirectedAction;
import edu.cwru.sepia.action.ActionType;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.util.Pair;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

public class MinimaxAlphaBeta extends Agent {

    private final int numPlys;

    public MinimaxAlphaBeta(int playernum, String[] args) {
        super(playernum);

        if (args.length < 1) {
            System.err.println("You must specify the number of plys");
            System.exit(1);
        }

        numPlys = Integer.parseInt(args[0]);
    }

    @Override
    public Map<Integer, Action> initialStep(State.StateView newstate, History.HistoryView statehistory) {
        return middleStep(newstate, statehistory);
    }

    @Override
    public Map<Integer, Action> middleStep(State.StateView newstate, History.HistoryView statehistory) {

        GameStateChild bestChild = alphaBetaSearch(new GameStateChild(newstate),
                numPlys,
                Double.NEGATIVE_INFINITY,
                Double.POSITIVE_INFINITY);

        return bestChild.action;
    }

    @Override
    public void terminalStep(State.StateView newstate, History.HistoryView statehistory) {

    }

    @Override
    public void savePlayerData(OutputStream os) {

    }

    @Override
    public void loadPlayerData(InputStream is) {

    }

    /**
     * You will implement this.
     * <p>
     * This is the main entry point to the alpha beta search. Refer to the slides, assignment description
     * and book for more information.
     * <p>
     * Try to keep the logic in this function as abstract as possible (i.e. move as much SEPIA specific
     * code into other functions and methods)
     *
     * @param node  The action and state to search from
     * @param depth The remaining number of plys under this node
     * @param alpha The current best value for the maximizing node from this node to the root
     * @param beta  The current best value for the minimizing node from this node to the root
     * @return The best child of this node with updated values
     */
    public GameStateChild alphaBetaSearch(GameStateChild node, int depth, double alpha, double beta) {
        double value;
        GameStateChild bestChild = node;
        value = -70000;

        for (GameStateChild child : orderChildrenWithHeuristics(node.state.getChildren())) {
            double childValue = alphaBetaValue(child, depth - 1, alpha, beta);
            if (childValue == value) {
                bestChild = Math.random() * 100 > 50 ? child : bestChild;
            }
            bestChild = childValue > value ? child : bestChild;
            value = childValue > value ? childValue : value;
        }

        System.out.printf("utility: %f\n", bestChild.state.getUtility());
        System.out.printf("%s\n\n", bestChild.action.toString());
        return bestChild;
    }

    /**
     * Returns the value of a node by applying the alphabeta search algorithm
     * @param node  The action and state to search from
     * @param depth The remaining number of plys under this node
     * @param alpha The current best value for the maximizing node from this node to the root
     * @param beta  The current best value for the minimizing node from this node to the root
     * @return The value of this node
     */
    public double alphaBetaValue(GameStateChild node, int depth, double alpha, double beta) {
        if (depth == 0 || isLeafNode(node)) {
            return node.state.getUtility();
        }

        if (isMaxNode(node)) {
            double value = Double.NEGATIVE_INFINITY;

            for (GameStateChild child : orderChildrenWithHeuristics(node.state.getChildren())) {
                value = Math.max(value, alphaBetaValue(child, depth - 1, alpha, beta));
                alpha = Math.max(alpha, value);

                if (beta <= alpha) {
                    break;
                }
            }
            return value;

        } else {
            double value = Double.POSITIVE_INFINITY;

            for (GameStateChild child : orderChildrenWithHeuristics(node.state.getChildren())) {
                value = Math.min(value, alphaBetaValue(child, depth - 1, alpha, beta));
                beta = Math.min(beta, value);

                if (beta <= alpha) {
                    break;
                } 
            }
            return value;
        }
    }

    /**
     * Orders a list of children by a heuristic
     *
     * The heuristic takes into account distance and unit health. In order to minimize the time spent calculating
     * The distance is the taxicab distance between each footman and each archer and not the actual path. Unit health
     * is important to value attacking states.
     *
     * @param children The list of children of the current state
     * @return The list of children sorted by your heuristic.
     */
    public List<GameStateChild> orderChildrenWithHeuristics(List<GameStateChild> children) {
        // multiplier that is positive for max nodes, negative for min nodes
        // used for when footmen/archers want opposite things (e.g., archers want more distance)
        int multiplier = children.get(0).state.getMaxNode() ? 1 : -1;

        ArrayList<Pair<Integer, GameStateChild>> heuristicValues = new ArrayList<>();
        for (GameStateChild child : children) {
            int value = 0;

            // Heuristics based upon units
            for (GameState.SimpleUnit footman : child.state.getFootmen()) {
                value += footman.getCurrentHealth();
                // give each state a value based upon distance the footman are from the archers
                for (GameState.SimpleUnit archer : child.state.getArchers()) {
                    value -= (taxicab(footman, archer) * multiplier);
                    value -= archer.getCurrentHealth();
                }
            }

            heuristicValues.add(new Pair<>(value, child));
        }

        Comparator<Pair<Integer, GameStateChild>> c = new Comparator<Pair<Integer, GameStateChild>>() {
            @Override
            public int compare(Pair<Integer, GameStateChild> o1, Pair<Integer, GameStateChild> o2) {
                return o2.a - o1.a;
            }
        };

        Collections.sort(heuristicValues, c);
        ArrayList<GameStateChild> orderedChildren = new ArrayList<>();
        for (Pair<Integer, GameStateChild> heuristic : heuristicValues) {
            orderedChildren.add(heuristic.b);
        }

        return orderedChildren;
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
     * Returns if the given node has a children or not.
     * @param node The node being tested.
     * @return true if node is a leaf node.
     */
    private boolean isLeafNode(GameStateChild node) {
        return node.state.getChildren() == null;
    }

    /**
     * Returns if the given node is a max node.
     * @param node The node being tested.
     * @return true if the node is a max node.
     */
    private boolean isMaxNode(GameStateChild node) {
        return node.state.getMaxNode();
    }
}
