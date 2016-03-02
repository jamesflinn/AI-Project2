package edu.cwru.sepia.agent.minimax;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.DirectedAction;
import edu.cwru.sepia.action.ActionType;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.State;

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
        if (depth == 0 || isLeafNode(node)) {
            return node;
        }

        if (isMaxNode(node)) {
            double value = Double.NEGATIVE_INFINITY;

            for (GameStateChild child : orderChildrenWithHeuristics(node.state.getChildren())) {
                value = Math.max(value, alphaBetaSearch(child, depth - 1, alpha, beta).state.getUtility());
                alpha = Math.max(alpha, value);

                if (beta <= alpha) {
                    break;
                }
            }
        } else {
            double value = Double.POSITIVE_INFINITY;

            for (GameStateChild child : orderChildrenWithHeuristics(node.state.getChildren())) {
                value = Math.min(value, alphaBetaSearch(child, depth - 1, alpha, beta).state.getUtility());
                beta = Math.min(beta, value);

                if (beta <= alpha) {
                    break;
                }
            }
        }
        return node;
    }

    /**
     * You will implement this.
     * <p>
     * Given a list of children you will order them according to heuristics you make up.
     * See the assignment description for suggestions on heuristics to use when sorting.
     * <p>
     * Use this function inside of your alphaBetaSearch method.
     * <p>
     * Include a good comment about what your heuristics are and why you chose them.
     *
     * @param children
     * @return The list of children sorted by your heuristic.
     */
    public List<GameStateChild> orderChildrenWithHeuristics(List<GameStateChild> children) {

        Map<Integer, GameStateChild> heuristicValues = new HashMap<>();
        for (GameStateChild child : children) {
            int value = 0;
            for (Map.Entry<Integer, Action> action : child.action.entrySet()) {

                // if this is an attack then best
                if (action.getValue().getType() == ActionType.PRIMITIVEATTACK) {
                    value += 1000;
                }

                // give each state a value based upon distance the footman are from the archers
                for (GameState.SimpleUnit footman : child.state.getFootmen()) {
                    for (GameState.SimpleUnit archer : child.state.getArchers()) {
                        value -= chebyshev(footman, archer);
                    }

                    // if we are moving into a resource then very bad
                    for (GameState.ResourceLocation resource : child.state.getResources()) {
                        if (resource.getLocation().equals(footman.getLocation())) {
                            value -= 10000;
                        }
                    }
                }

            }

            heuristicValues.put(value, child);
        }

        // This is a little roundabout way to order the children
        // sort the keys in descending order based upon the value assigned
        // then put each GameStateChild in the new list of gamestatechildren in order then return

        ArrayList<Integer> keys = new ArrayList<>(heuristicValues.keySet());
        Collections.sort(keys);
        Collections.reverse(keys);

        ArrayList<GameStateChild> orderedChildren = new ArrayList<>();

        for (Integer heuristic : keys) {
            orderedChildren.add(heuristicValues.get(heuristic));
        }

        return orderedChildren;
    }

    /**
     * Computes the chebyshev distance between two units
     *
     * @param first  The first unit
     * @param second The second unit
     * @return       The chebyshev distance between the two units.
     */
    private int chebyshev(GameState.SimpleUnit first, GameState.SimpleUnit second) {

        int deltaX = Math.abs(first.getX() - second.getX());
        int deltaY = Math.abs(first.getX() - second.getY());

        return deltaX < deltaY ? deltaY : deltaX;

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
        return true;
    }
}
