package md.netinfo.labs.petdetectivesolver.gameboard

import android.content.Context
import android.os.Build
import android.util.Log
import md.netinfo.labs.petdetectivesolver.BuildConfig
import md.netinfo.labs.petdetectivesolver.R
import md.netinfo.labs.petdetectivesolver.exceptions.*
import md.netinfo.labs.petdetectivesolver.imageprocessing.FoundObjectInfo

/**
 * Class to generate gameboard as 2D array of objects
 *
 * @param context Context of the application, needed to access resources
 * @param foundPets Map of found pets/pet houses/car
 * @param foundDots List of found dots
 */
class GameBoardGenerator (
    val context: Context,
    val foundPets: Map<String, FoundObjectInfo>,
    val foundDots: List<FoundObjectInfo>
) {

    public fun generateArray(): GameBoard {
        var listObjects = foundPets.values.toMutableList()
        listObjects.addAll(foundDots)
        var allObjects = listObjects.toTypedArray()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            throw PDSException(
                context.resources.getString(
                    R.string.android_nougat_needed
                )
            )
        }

        var sortedByX = allObjects.sortedArrayWith(Comparator{ o1, o2 -> o1.center.x.compareTo(o2.center.x) })
        var sortedByY = allObjects.sortedArrayWith(Comparator{ o1, o2 -> o1.center.y.compareTo(o2.center.y) })
        for(o in sortedByX) Log.d(TAG, "Sorted by X: ${o.shortName}@${o.center}")
        for(o in sortedByY) Log.d(TAG, "Sorted by Y: ${o.shortName}@${o.center}")

        val (sizeX, sizeY) = getGameBoardSize(foundPets.size, foundDots.size)
        // check that sizes in the same X/Y group do not differ too much
        // raises exception if not correlated
        checkGroupCorrelation(sortedByX.map { it.center.x }, sizeY, "by X")
        checkGroupCorrelation(sortedByY.map { it.center.y }, sizeX, "by Y")

        var gb = generateGameBoard(sortedByX.toList(), sizeX, sizeY)
        if(!gb.checkValidity()) {
            Log.e(TAG, "Gameboard contains empty elements, gameboard is: '${gb}'")
            throw GameBoardHasEmptyValuesException(context.getString(R.string.gameboard_has_empty_values))
        }

        gb.fixGameboard()

        return gb
    }

    /**
     * Function to return game board size depending on the passed number of pets and number of dots
     *
     * @param petSize Number of pets (including pet houses and car) on the board
     * @param dotSize Number of empty intersections on the board
     * @return Game board size (X x Y)
     */
    private fun getGameBoardSize(petSize: Int, dotSize: Int): Pair<Int, Int> {
        return when (petSize + dotSize) {
            9       -> Pair(3, 3)
            12      -> Pair(3, 4)
            15      -> Pair(3, 5)
            18      -> Pair(3, 6)
            20      -> Pair(4, 5)
            24      -> Pair(4, 6)
            else    -> throw WrongNumberOfPetsException(
                context.getString(R.string.check_wrong_field_size, petSize, dotSize)
            )
        }
    }

    /**
     * Check group correlation.
     * Group objects by groupSize, and check difference between max and min values.
     * The difference should be within threshold.
     *
     * @param objects List of objects to check for correlation
     * @param groupSize Size of the group, objects in list should be grouped by
     * @param where Auxiliary string, used in log writing (plus exception)
     * @return True if objects within the group are correlated
     */
    private fun checkGroupCorrelation(objects: List<Int>, groupSize: Int, where: String): Boolean {
        var idx = 0
        while(idx < objects.size) {
            val diff = objects[idx+groupSize-1] - objects[idx]
            if(diff > PROXIMITY_GROUP_MAX_VALUE) {
                Log.d(TAG, "small ${objects[idx]} big ${objects[idx+groupSize-1]}")
                throw GroupTooScatteredException(
                    context.getString(
                        R.string.group_values_too_scattered,
                        where,
                        diff,
                        PROXIMITY_GROUP_MAX_VALUE
                    )
                )
            }
            idx += groupSize
        }
        return true
    }

    /**
     * Generate gameboard of correspondent size.
     * @param objectsGroupedByX List of objects, grouped by X coordinate
     * @param sizeX Size of gameboard on X axis
     * @param sizeY Size of gameboard on Y axis
     * @return Gameboard with size of sizeX x sizeY
     */
    private fun generateGameBoard(objectsGroupedByX: List<FoundObjectInfo>, sizeX: Int, sizeY: Int): GameBoard {
        var gb = GameBoard(sizeX, sizeY)

        var x = 0
        while(x*sizeY < objectsGroupedByX.size) {
            var subListY = objectsGroupedByX.subList(x*sizeY, (x+1)*sizeY).sortedWith(Comparator{ o1, o2 -> o1.center.y.compareTo(o2.center.y) })
            var y = 0
            while(y<sizeY) {
                gb.gbd.gameboard[x][y] = subListY[y]
                y+=1
            }
            x += 1
        }

        if(BuildConfig.DEBUG) {
            Log.d(TAG, "Gameboard: ${gb}")
            for(x in 0 until sizeX) {
                for(y in 0 until sizeY) {
                    val o: FoundObjectInfo = gb.gbd.gameboard[x][y]!!
                    Log.d(TAG, "Pos[$x, $y]=${FoundObjectInfo.getInfo(o)}")
                }
            }
        }
        return gb
    }

    companion object {
        /** TAG for logging */
        private val TAG = "PDS::GameBoardGenerator"
        /** Max allowed difference in coordinates in the group. TODO: May be to move to the settings? But not sure if needed */
        private val PROXIMITY_GROUP_MAX_VALUE = 20
    }
}