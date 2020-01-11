package md.netinfo.labs.petdetectivesolver.solutionfinder

class Solution(var numberOfSteps: Int, var solutionPath: List<CarMove>?) {
    /**
     * Class that specifies car moves
     * @param   position Position where car moves
     * @param   action  Action what car does on this step
     */
    data class CarMove(val position: Int, val action: Char, var numberOfMoves: Int = 0)
}