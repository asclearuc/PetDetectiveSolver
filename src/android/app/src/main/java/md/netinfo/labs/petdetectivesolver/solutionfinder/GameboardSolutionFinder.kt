package md.netinfo.labs.petdetectivesolver.solutionfinder

import md.netinfo.labs.petdetectivesolver.gameboard.GameBoard
import md.netinfo.labs.petdetectivesolver.gameboard.SimpleGameBoardData
import md.netinfo.labs.petdetectivesolver.imageprocessing.FoundObjectInfo

abstract class GameboardSolutionFinder(val gameboard: SimpleGameBoardData) : SolutionFinder() {
    /**
     * Class that specifies car moves
     * @param   position Position where car moves
     * @param   action  Action what car does on this step
     * @param   objectInfo Found object information
     */
    data class ExtendedCarMove(val carMove: Solution.CarMove, val objectInfo: FoundObjectInfo?)

    fun getExtendedSolutionPath(gameboard: GameBoard): DetailedSolution? {
        val solution = getSolution() ?: return null
        return Companion.getExtendedSolutionPath(solution!!, gameboard)
    }

    companion object {
        fun getExtendedSolutionPath(solution: Solution, gameboard: GameBoard): DetailedSolution? {
            return solution.solutionPath!!.toMutableList().map {
                ExtendedCarMove(
                    Solution.CarMove(
                        it.position,
                        it.action,
                        it.numberOfMoves
                    ),
                    gameboard.getObjectByPosition(it.position)
                )
            }
        }
    }
}