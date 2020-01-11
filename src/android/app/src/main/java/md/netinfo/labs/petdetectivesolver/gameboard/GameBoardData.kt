package md.netinfo.labs.petdetectivesolver.gameboard

import md.netinfo.labs.petdetectivesolver.imageprocessing.FoundObjectInfo

class GameBoardData(val numberOfCols: Int, val numberOfRows: Int) {
    var gameboard =
        Array(numberOfCols) { _ -> Array(numberOfRows) { _ -> (null as FoundObjectInfo?) } }
}