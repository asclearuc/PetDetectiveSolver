package md.netinfo.labs.petdetectivesolver.solutionfinder

import md.netinfo.labs.petdetectivesolver.gameboard.SimpleGameBoardData
import org.junit.Assert.*
import org.junit.Test

class DijkstraAlgorithmTest {
    class DijkstraAlgorithmWrapper(gb: SimpleGameBoardData) : DijkstraAlgorithm(gb)
    {
        fun getCharacteristics(): Triple<Int, Int, Int> = Triple(numberOfPets, numberOfCols, numberOfRows)
        fun getCarPosition(): Pair<Int, Int>            = Pair(carPosC, carPosR)
        fun getFromArray(): Array<Pair<Int, Int>>       = from
        fun getToArray(): Array<Pair<Int, Int>>         = to
        fun getDirections(): Array<Array<Int>>          = d
    }

    @Test
    fun test_Screenshot_20190203_161914() {
        val gb = getGameboard_Screenshot_20190203_161914()
        var dsfw = DijkstraAlgorithmWrapper(gb)

        val (numberOfPets, numberOfCols, numberOfRows) = dsfw.getCharacteristics()
        val (carPosCol, carPosRow)       = dsfw.getCarPosition()!!
        assertEquals(9, numberOfPets)
        assertEquals(4, numberOfCols)
        assertEquals(6, numberOfRows)
        assertEquals(2, carPosCol)
        assertEquals(2, carPosRow)

        val from    = dsfw.getFromArray()
        val to      = dsfw.getToArray()
        val d       = dsfw.getDirections()


        assertEquals(Pair(0, 0), to[0])          // husky
        assertEquals(Pair(2, 4), from[0])
        assertEquals(6, d[0][16])
        assertEquals(6, d[16][0])

        assertEquals(Pair(3, 4), to[3])          // tabby
        assertEquals(Pair(0, 3), from[3])
        assertEquals(6, d[3][22])
        assertEquals(6, d[22][3])

    }
    @Test
    fun test_Simple2x2_Correct() {
        val gb = getGameboard_Simple2x2_Correct()
        var dsfw = DijkstraAlgorithmWrapper(gb)

        val (numberOfPets, numberOfCols, numberOfRows) = dsfw.getCharacteristics()
        val (carPosCol, carPosRow)       = dsfw.getCarPosition()!!
        assertEquals(1, numberOfPets)
        assertEquals(2, numberOfCols)
        assertEquals(2, numberOfRows)
        assertEquals(0, carPosCol)
        assertEquals(1, carPosRow)

        val from    = dsfw.getFromArray()
        val to      = dsfw.getToArray()
        val d       = dsfw.getDirections()


        assertEquals(Pair(1, 1), to[0])          // husky
        assertEquals(Pair(0, 0), from[0])
        assertEquals(2, d[0][3])
        assertEquals(2, d[3][0])

        assertEquals(3, dsfw.solve())
        
        var solution = dsfw.getSolution()
        assertEquals(3, solution!!.numberOfSteps)
        assertEquals(Solution.CarMove(1, 'i'), solution!!.solutionPath!!.get(0))
        assertEquals(Solution.CarMove(0, 't', 1), solution!!.solutionPath!!.get(1))
        assertEquals(Solution.CarMove(3, 'p', 3), solution!!.solutionPath!!.get(2))
    }

    fun getGameboard_Screenshot_20190203_161914(): SimpleGameBoardData {
        // based on Screenshot_20190203-161914.jpg
        var gb = SimpleGameBoardData(4,6)
        // column 0
        gb.gameboard[0][0] = SimpleGameBoardData.SimpleObjectInfo(0, "hHusky", true, false, false, true)
        gb.gameboard[0][1] = SimpleGameBoardData.SimpleObjectInfo(1, "pCockatiel",  true, true,  false, true)
        gb.gameboard[0][2] = SimpleGameBoardData.SimpleObjectInfo(2, "pHedgehog",   true, true,  false, false)
        gb.gameboard[0][3] = SimpleGameBoardData.SimpleObjectInfo(3, "pTabby",      true, true,  false, false)
        gb.gameboard[0][4] = SimpleGameBoardData.SimpleObjectInfo(4, "hFerret",     true, true,  false, true)
        gb.gameboard[0][5] = SimpleGameBoardData.SimpleObjectInfo(5, "dot",         false,true,  false, true)
        // column 1
        gb.gameboard[1][0] = SimpleGameBoardData.SimpleObjectInfo(6, "hSiamese",    false,false, true,  true)
        gb.gameboard[1][1] = SimpleGameBoardData.SimpleObjectInfo(7,"hChameleon",  true, false, true,  true)
        gb.gameboard[1][2] = SimpleGameBoardData.SimpleObjectInfo(8,"dot",         false,true,  false, true)
        gb.gameboard[1][3] = SimpleGameBoardData.SimpleObjectInfo(9,"pSiamese",    true, false, false, true)
        gb.gameboard[1][4] = SimpleGameBoardData.SimpleObjectInfo(10,"dot",         true, true,  true,  false)
        gb.gameboard[1][5] = SimpleGameBoardData.SimpleObjectInfo(11,"pChameleon",  false,true,  true,  true)
        // column 2
        gb.gameboard[2][0] = SimpleGameBoardData.SimpleObjectInfo(12,"hCockatiel",  true, false, true,  true)
        gb.gameboard[2][1] = SimpleGameBoardData.SimpleObjectInfo(13,"hHedgehog",   false,true,  true,  true)
        gb.gameboard[2][2] = SimpleGameBoardData.SimpleObjectInfo(14, "cRight",      true, false, true,  true)
        gb.gameboard[2][3] = SimpleGameBoardData.SimpleObjectInfo(15,"pFerret",     true, true,  true,  false)
        gb.gameboard[2][4] = SimpleGameBoardData.SimpleObjectInfo(16,"pHusky",      true, true,  false, true)
        gb.gameboard[2][5] = SimpleGameBoardData.SimpleObjectInfo(17,"hTurtle",     false,true,  true,  true)
        // column 3
        gb.gameboard[3][0] = SimpleGameBoardData.SimpleObjectInfo(18,"hDachsund",   true, false, true,  false)
        gb.gameboard[3][1] = SimpleGameBoardData.SimpleObjectInfo(19,"dot",         true, true,  true,  false)
        gb.gameboard[3][2] = SimpleGameBoardData.SimpleObjectInfo(20,"pTurtle",     true, true,  true,  false)
        gb.gameboard[3][3] = SimpleGameBoardData.SimpleObjectInfo(21,"pDachsund",   true, true,  false, false)
        gb.gameboard[3][4] = SimpleGameBoardData.SimpleObjectInfo(22,"hTabby",      true, true,  true,  false)
        gb.gameboard[3][5] = SimpleGameBoardData.SimpleObjectInfo(23,"dot",         false,true,  true,  false)

        return gb
    }
    fun getGameboard_Simple2x2_Correct(): SimpleGameBoardData {
        var gb = SimpleGameBoardData(2,2)
        // column 0
        gb.gameboard[0][0] = SimpleGameBoardData.SimpleObjectInfo(0,"pHusky",      true, false, false, true)
        gb.gameboard[0][1] = SimpleGameBoardData.SimpleObjectInfo(1, "cRight",      false,true,  false, true)
        // column 1
        gb.gameboard[1][0] = SimpleGameBoardData.SimpleObjectInfo(2, "dot",         true, false, true,  false)
        gb.gameboard[1][1] = SimpleGameBoardData.SimpleObjectInfo(3,"hHusky",      false,true,  true,  false)

        return gb
    }
}
