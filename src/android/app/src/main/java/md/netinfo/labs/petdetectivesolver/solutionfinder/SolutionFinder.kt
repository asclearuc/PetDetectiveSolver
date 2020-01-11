package md.netinfo.labs.petdetectivesolver.solutionfinder

abstract class SolutionFinder {

    /**
     * Function that solves puzzle
     * @return Number of steps in solution, if found. -1 otherwise
     */
    abstract fun solve(): Int

    /**
     * Returns number of steps in solution (if exists or was found), -1 otherwise.
     * @return Number of steps in solution
     */
    abstract fun getSolutionMovesCount(): Int
    /**
     * Returns list of steps that solve the puzzle
     * @return List of step if solution exists, null otherwise
     */
    abstract fun getSolution(): Solution?
}
