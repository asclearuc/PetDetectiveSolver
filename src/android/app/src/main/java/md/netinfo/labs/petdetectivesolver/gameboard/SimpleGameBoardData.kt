package md.netinfo.labs.petdetectivesolver.gameboard

import md.netinfo.labs.petdetectivesolver.imageprocessing.FoundObjectInfo

class SimpleGameBoardData(val numberOfCols: Int, val numberOfRows: Int) {
    var gameboard =
        Array(numberOfCols) { _ -> Array(numberOfRows) { _ -> (null as SimpleObjectInfo?) } }

    data class SimpleObjectInfo(
        val id: Int,
        val name: String,
        var isDown: Boolean = false,
        var isUp: Boolean = false,
        var isLeft: Boolean = false,
        var isRight: Boolean = false
    )

    /**
     * Convert two-dimensional position (row, column) to one dimensional (linear) position
     * @param c Column of the position
     * @param r Row of the position
     * @return Converted position
     */
    fun get1DPosition(c: Int, r: Int): Int = (numberOfRows * c + r)

    companion object {
        fun create(id: Int, oi: FoundObjectInfo?): SimpleObjectInfo? {
            return when(oi) {
                null -> { null }
                else -> { SimpleObjectInfo(id, oi.name, oi.isDown, oi.isUp, oi.isLeft, oi.isRight)}
            }
        }
    }
}