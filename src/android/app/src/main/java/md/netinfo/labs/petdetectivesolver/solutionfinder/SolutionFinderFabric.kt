package md.netinfo.labs.petdetectivesolver.solutionfinder

import md.netinfo.labs.petdetectivesolver.gameboard.SimpleGameBoardData

object SolutionFinderFabric {
    enum class Type {
        BFS,
        Dijkstra
    }

    fun getSolutionFinder(type: Type, gameboard: SimpleGameBoardData): GameboardSolutionFinder? {
        return when(type) {
            Type.BFS        -> BFSAlgorithm(gameboard)
            Type.Dijkstra   -> DijkstraAlgorithm(gameboard)
            else            -> null
        }
    }
}