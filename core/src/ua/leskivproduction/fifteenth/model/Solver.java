package ua.leskivproduction.fifteenth.model;

import ua.leskivproduction.fifteenth.utils.MinQueue;

import java.util.Collections;
import java.util.LinkedList;

public class Solver {
    private final static int DEPTH_CHECK = 15;
    private final static float SOLVE_ANIMATION_TIME = 15;

    private Board[] solution;
    private boolean solvable;
    private boolean solving = true;

    private float solvingAnimationTime;
    private int animationStep;

    private float solveAnimInterval;
    private long solutionFoundingTime;

    /**
     * @return time in ms
     */
    public float getSolutionFoundingTime() {
        return (float)solutionFoundingTime/1000;
    }

    public boolean isSolving() {
        return solving;
    }

    /**
     * @return true if animation has finished
     */
    public boolean performAnimationSteps(Board board, float deltaTime) {
        if (!solving && solvable && animationStep < solution.length) {
            solvingAnimationTime += deltaTime;
            int performedByNow = Math.min(solution.length, (int)(solvingAnimationTime/solveAnimInterval));
            for (; animationStep < performedByNow; animationStep++) {
                board.moveTo(solution[animationStep]);
            }
            return animationFinished();
        }
        return true;
    }

    public boolean animationFinished() {
        if (solution == null)
            return true;
        return animationStep >= solution.length;
    }

    public int getAnimationStep() {
        return animationStep;
    }

    // знайти рішення для дошки initial
    public Solver(Board initial) {
        new Thread(() -> {
            solutionFoundingTime = System.currentTimeMillis();

            solvable = initial.solvable();

            if (solvable) {
                LinkedList<Board> solutionList = new LinkedList<>();
                MinQueue<Board> solutionQueue = new MinQueue<>();

                Board curBoard = initial;

                while (!curBoard.isGoal()) {
                    for (Board b : curBoard.neighbors()) {
                        if (b != null) {
                            Board depthBoard = b.previous.previous;

                            int checked = 1;
                            boolean toContinue = false;
                            while (checked++ < DEPTH_CHECK && depthBoard != null) {
                                if (depthBoard.equals(curBoard)) {
                                    toContinue = true;
                                    break;
                                }
                                depthBoard = depthBoard.previous;
                            }

                            if (toContinue)
                                continue;

                            solutionQueue.add(b);
                        }
                    }

                    Board toMoveTo;
                    while (true) {
                        toMoveTo = solutionQueue.removeMin();
                        if (curBoard.equals(toMoveTo))
                            continue;

                        break;
                    }

                    curBoard = toMoveTo;
                    System.out.println(curBoard.manhattan() + " = " + solutionQueue.size());
                }

                while (curBoard != null && !curBoard.equals(initial)) {
                    solutionList.add(curBoard);
                    curBoard = curBoard.previous;
                }
                Collections.reverse(solutionList);

                solution = solutionList.toArray(new Board[0]);

                solveAnimInterval = SOLVE_ANIMATION_TIME / solution.length;
                solveAnimInterval = Math.max(0.1f, Math.min(0.3f, solveAnimInterval));

            } else {
                solution = null;
            }

            solutionFoundingTime = System.currentTimeMillis() - solutionFoundingTime;
            solving = false;
        }).start();
    }

    // чи має початкова дошка розв’язок
    public boolean isSolvable() {
        return solvable;
    }

    // мінімальна кількість кроків для вирішення дошки, -1 якщо немає рішення
    public int moves() {
        if (solvable)
            return solution.length;
        return -1;
    }

    // послідовність дошок в найкоротшому рішенні; null якщо немає рішення
    public Board[] solution() {
        return solution;
    }

}
