package md.netinfo.labs.petdetectivesolver.model

data class PDSPoint(var x: Int, var y: Int) : Comparable<PDSPoint>
{
    constructor(x: Double, y: Double) : this(x.toInt(), y.toInt())

    override fun compareTo(other: PDSPoint): Int {
        if (x < other.x)                { return -1 }
        else if (x > other.x)           { return 1}
        else
            if (y < other.y)            { return -1 }
            else if (y > other.y)       { return 1 }
            else                        { return 0 }
    }
}