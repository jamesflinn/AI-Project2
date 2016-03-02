package edu.cwru.sepia.agent.minimax;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.DirectedAction;
import edu.cwru.sepia.action.ActionType;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.State;
import javafx.util.Pair;

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
     * @param children The list of children of the current state
     * @return The list of children sorted by your heuristic.
     */
    public List<GameStateChild> orderChildrenWithHeuristics(List<GameStateChild> children) {

        System.out.printf("number of states: %d\n", children.size());

        ArrayList<Pair<Integer, GameStateChild>> heuristicValues = new ArrayList();
        for (GameStateChild child : children) {
            int value = 0;

            // Heuristics based upon actions
            for (Map.Entry<Integer, Action> action : child.action.entrySet()) {

                // if this is an attack then best
                if (action.getValue().getType() == ActionType.PRIMITIVEATTACK) {
                    value += 1000;
                }

            }

            // Heuristics based upon units
            for (GameState.SimpleUnit footman : child.state.getFootmen()) {

                // give each state a value based upon distance the footman are from the archers
                for (GameState.SimpleUnit archer : child.state.getArchers()) {
                    value -= taxicab(footman, archer);
                }

                // if we are moving into a resource then very bad
                for (GameState.ResourceLocation resource : child.state.getResources()) {
                    if (resource.getLocation().equals(footman.getLocation())) {
                        value -= 10000;
                    }
                }
            }


            heuristicValues.add(new Pair<Integer, GameStateChild>(value, child));
            System.out.printf("putting value: %d\n", heuristicValues.size());
        }


        Comparator<Pair<Integer, GameStateChild>> c = new Comparator<Pair<Integer, GameStateChild>>() {
            @Override
            public int compare(Pair<Integer, GameStateChild> o1, Pair<Integer, GameStateChild> o2) {

                return o1.getKey() - o2.getKey();
            }
        };

        Collections.sort(heuristicValues, c);
        ArrayList<GameStateChild> orderedChildren = new ArrayList<>();
        for (Pair<Integer, GameStateChild> heuristic : heuristicValues) {
            orderedChildren.add(heuristic.getValue());
        }

        System.out.printf("number ordered = %d\n", orderedChildren.size());

        for (int i = 0; i < orderedChildren.size(); i++) {
            System.out.println(orderedChildren.get(i).state.toString());
            System.out.printf("value for state: %d = %d%n", i, heuristicValues.get(i).getKey());
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
        return true;
    }
}
