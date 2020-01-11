package md.netinfo.labs.petdetectivesolver.imageprocessing

import md.netinfo.labs.petdetectivesolver.model.PDSPoint

// asclearuc:TODO: convert X, Y coordinates to Point
data class FoundObjectInfo(val name: String,
                           val maxVal: Double, val ratio: Double, val scale: Double,
                           var start: PDSPoint, var end: PDSPoint,
                           var isDown: Boolean = false,
                           var isUp: Boolean = false,
                           var isLeft: Boolean = false,
                           var isRight: Boolean = false)
{
    var center =PDSPoint((start.x + end.x) / 2, (start.y + end.y) / 2)
    val shortName = name.subSequence(0, Math.min(name.length, 4))

    /** auxillary constructor, used in tests */
    constructor(name:String, isDown: Boolean, isUp: Boolean, isLeft: Boolean, isRight: Boolean) :
            this(name, 0.0, 0.0, 0.0, PDSPoint(0.0, 0.0), PDSPoint(0.0, 0.0),
                isDown, isUp, isLeft, isRight)

    companion object {
        fun getInfo(o: FoundObjectInfo): String {
            return getInfo(o.name, o)
        }
        fun getInfo(key: String, o: FoundObjectInfo): String {
            val sb = StringBuilder()
            sb.append(String.format("%-3s: ", "${key.substring(0, Math.min(key.length, 3))}"))
                .append("${if (o.isUp) "^" else "-"}${if (o.isDown) "v" else "-"}")
                .append("${if (o.isLeft) "<" else "-"}${if (o.isRight) ">" else "-"}")
            return sb.toString()
        }
    }
}
