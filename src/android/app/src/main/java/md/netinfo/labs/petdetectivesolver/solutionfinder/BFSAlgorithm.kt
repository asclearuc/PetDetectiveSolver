package md.netinfo.labs.petdetectivesolver.solutionfinder

import android.util.Log

import md.netinfo.labs.petdetectivesolver.BuildConfig
import md.netinfo.labs.petdetectivesolver.gameboard.SimpleGameBoardData
import md.netinfo.labs.petdetectivesolver.resources.SharedGameObjects

class BFSAlgorithm(gameboard: SimpleGameBoardData): GameboardSolutionFinder(gameboard) {
    private var numberOfRows = 0
    private var numberOfCols = 0
    //private var d: Array<Array<Array<Byte>>> = Array(N) { Array(ARRAY_DIM) { Array(ARRAY_DIM) { ARRAY_FILL } } }
    private var d: Array<Byte>? = null

    private var parsedGameboard: Array<Array<ParsedGameboardItem>>? = null

    private var carPos: Pair<Int, Int>? = null

    init {
        Log.d(TAG, "Allocating d")
        d = Array(N*ARRAY_DIM*ARRAY_DIM) { ARRAY_FILL }
        Log.d(TAG, "Allocated d")
        parseGameboard()
    }

    override fun solve(): Int {
        return -1
    }
    /**
     * Returns number of steps in solution (if exists or was found), -1 otherwise.
     * @return Number of steps in solution
     */
    override fun getSolutionMovesCount(): Int {
        return -1
    }
    /**
     * Returns list of steps that solve the puzzle
     * @return List of step if solution exists, null otherwise
     */
    override fun getSolution(): Solution? {
        return null
    }


    private fun parseGameboard() {
        numberOfCols = gameboard.gameboard.size
        numberOfRows = gameboard.gameboard[0].size

        parsedGameboard = Array(numberOfCols+2){Array(numberOfRows+2) {ParsedGameboardItem()} }

        // pet type - used below
        var petType: Int
        var petList = ArrayList<String>()

        var c = 0
        while(c<numberOfCols) {
            var r = 0
            while (r<numberOfRows) {
                val r1 = r + 1
                val c1 = c + 1

                val o = gameboard.gameboard[c][r]!!
                var petId = 0
                when (o.name[0]) {
                    // car
                    SharedGameObjects.prefixCar[0] -> {
                        carPos = Pair(r1, c1)
                        petType = 99
                    }
                    // dot
                    SharedGameObjects.prefixDot[0] -> {
                        petType = 0
                    }
                    // pet
                    else                       -> {
                        // get pet type (house/pet) and real pet name (Siamese, Husky...)
                        petType = o.name[0].toInt()
                        var petName: String = o.name.substring(1, o.name.length-1)

                        // check if such pet is already in the list, if not - add new
                        petId = petList.indexOf(petName)
                        if (petId==-1) {
                            petId = petList.size
                            petList.add(petName)
                        }
                        // change index to negative if it is a house
                        if (petType == SharedGameObjects.prefixHouse[0].toInt()) {
                            petId = -petId
                        }
                    }
                }

                // update parsed gameboard based on the information
                with(parsedGameboard!![r1][c1]) {
                    this.petId      = petId
                    this.upRef      = getPosition(r1-1, c1, o.isUp)
                    this.downRef    = getPosition(r1+1, c1, o.isDown)
                    this.leftRef    = getPosition(r1,   c1-1, o.isLeft)
                    this.rightRef   = getPosition(r1,   c1+1, o.isRight)
                }

                r+=1
            }
            c+=1
        }

        if(BuildConfig.DEBUG) {
            var sb = StringBuilder()
            for(c in parsedGameboard!!) {
                if(!sb.isEmpty())
                    sb.append(",")
                sb.append("[")
                var sb1 = StringBuilder()
                for (e in c) {
                    if(!sb1.isEmpty())
                        sb1.append(",")
                    sb1.append("(${e.petId}/${e.upRef}/${e.downRef}/${e.leftRef}/${e.rightRef})")
                }
                sb.append("${sb1.toString()}]")
            }
            Log.d(TAG, "Array: ${sb.toString()}")
        }
    }

    private fun getPosition(c: Int, r: Int, isNeeded: Boolean):Int {
        return when (isNeeded) {
            true        -> getPosition(c, r)
            else        -> -1
        }
    }
    private fun getPosition(c: Int, r: Int): Int = (numberOfCols * r + c)

    data class ParsedGameboardItem(var petId:Int=0, var upRef:Int = -1, var downRef: Int = -1, var leftRef: Int=-1, var rightRef: Int=-1)

    companion object {
        /** TAG for logging */
        private val TAG = "PDS::BFSSolutionFinder"
        /** Maximum number of moves */
        private val N = 45
        /** Array dimensions */
        private val ARRAY_DIM = 2048
        /** Array fill value */
        private val ARRAY_FILL: Byte = 0x3C
    }
}
