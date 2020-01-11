package md.netinfo.labs.petdetectivesolver.gameboard

import android.util.Log

import md.netinfo.labs.petdetectivesolver.imageprocessing.FoundObjectInfo
import md.netinfo.labs.petdetectivesolver.resources.SharedGameObjects

class GameBoard(val numberOfCols: Int, val numberOfRows: Int) {
    var gbd = GameBoardData(numberOfCols, numberOfRows)

    /**
     * Function to check validity of gameboard.
     * Returns false if any of the gameboard elements is uninitialized, true otherwise
     *
     * @return Gameboard validity
     */
    fun checkValidity(): Boolean {
        return !gbd.gameboard.flatten().any{it==null}
    }

    /**
     * Function to check gameboard. Fix is needed for car - current algorithm sometimes doesn't detect
     * correctly road detection under car.
     */
    fun fixGameboard() {
        var found = false

        var x = 0
        while (!found && x < gbd.gameboard.size) {
            var y = 0
            while (!found && y < gbd.gameboard[x].size) {
                val keyName = gbd.gameboard[x][y]!!.name
                if (keyName[0] == SharedGameObjects.prefixCar[0]) {
                    // get environment values
                    val isUp:       Boolean = when (y != 0)                         { false   -> { false }; else    -> { gbd.gameboard[x][y-1]!!.isDown  } }
                    val isDown:     Boolean = when (y != gbd.gameboard[x].size-1)   { false   -> { false }; else    -> { gbd.gameboard[x][y+1]!!.isUp    } }
                    val isLeft:     Boolean = when (x != 0)                         { false   -> { false }; else    -> { gbd.gameboard[x-1][y]!!.isRight } }
                    val isRight:    Boolean = when (x != gbd.gameboard.size-1)      { false   -> { false }; else    -> { gbd.gameboard[x+1][y]!!.isLeft  } }
                    // check against the car
                    var car: FoundObjectInfo  = gbd.gameboard[x][y]!!
                    if(isUp != car.isUp || isDown!=car.isDown || isLeft!=car.isLeft || isRight!=car.isRight) {
                        Log.i(TAG, "Fixing car@($x, $y): was: (u=${car.isUp}/d=${car.isDown}/l=${car.isLeft}/r=${car.isRight}); " +
                                    "new: (u=${isUp}/d=${isDown}/l=${isLeft}/r=${isRight})")
                        car.isUp = isUp
                        car.isDown = isDown
                        car.isLeft = isLeft
                        car.isRight = isRight
                    }
                    found = true
                }
                y += 1
            }
            x += 1
        }
    }

    /**
     * Function to return array formatted as string.
     * It is used to print contents of the gameboard
     * TODO: May be there is a standard way to do this
     * @return Gameboard as a string
     */
    override fun toString(): String {
        var sb: StringBuilder = StringBuilder()

        var x = 0
        sb.append("[")
        while (x < gbd.gameboard.size) {
            sb.append("numberOfCols=$x: [")
            var y = 0
            while (y < gbd.gameboard[x].size) {
                sb.append("numberOfRow=$y: ")
                sb.append(gbd.gameboard[x][y])
                y += 1
                if (y<gbd.gameboard[x].size)
                    sb.append(", ")
            }
            x += 1
            sb.append("]")
            if (x<gbd.gameboard.size)
                sb.append(", ")
        }

        sb.append("]")
        return sb.toString()
    }

    /**
     * Convert one-dimensional (linear) position to two dimensional one (column, row)
     * @param pos One-dimensional position
     * @return Pair of column and row
     */
    fun get2DPosition(pos: Int): Pair<Int, Int>  {
        val c = pos/numberOfRows
        val r = pos - c * numberOfRows
        return Pair(c, r)
    }

    fun getObjectByPosition(pos: Int) : FoundObjectInfo? {
        val pos2D = get2DPosition(pos)
        return gbd.gameboard[pos2D.first][pos2D.second]
    }

    fun getSimpleGameBoardData(): SimpleGameBoardData {
        var idx = 0
        var result = SimpleGameBoardData(numberOfCols, numberOfRows)
        for(c in 0 until numberOfCols) {
            for(r in 0 until numberOfRows) {
                result.gameboard[c][r] = SimpleGameBoardData.create(idx, gbd.gameboard[c][r])
                idx += 1
            }
        }
        return result
    }

    companion object {
        /** TAG for logging */
        private val TAG = "PDS::GameBoard"
    }
}