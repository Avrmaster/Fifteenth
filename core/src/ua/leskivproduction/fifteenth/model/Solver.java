package ua.leskivproduction.fifteenth.model;

import ua.leskivproduction.fifteenth.utils.MinQueue;

import java.util.Collections;
import java.util.LinkedList;

public class Solver {
    private final Board[] solution;
    private final boolean solvable;

    private final static int DEPTH_CHECK = 10;

    // знайти рішення для дошки initial
    public Solver(Board initial) {
//        solvable = initial.hamming()%2 == 0;
        solvable = true;
        if (solvable) {
            LinkedList<Board> solutionList = new LinkedList<>();
            MinQueue<Board> solutionQueue = new MinQueue<>();

            Board curBoard = initial;
            System.out.println("Goal: "+curBoard.isGoal());

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
//                System.out.println(curBoard);
            }

            while (curBoard != null && !curBoard.equals(initial)) {
                solutionList.add(curBoard);
                curBoard = curBoard.previous;
            }
            Collections.reverse(solutionList);

            solution = solutionList.toArray(new Board[0]);
        } else {
            solution = null;
        }
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
