package md.netinfo.labs.petdetectivesolver.solutionfinder

import android.util.Log

import md.netinfo.labs.petdetectivesolver.gameboard.SimpleGameBoardData
import md.netinfo.labs.petdetectivesolver.resources.SharedGameObjects
import md.netinfo.labs.petdetectivesolver.utils.MyPair
import md.netinfo.labs.petdetectivesolver.utils.OrderedSet

open class DijkstraAlgorithm(gameboard: SimpleGameBoardData) : GameboardSolutionFinder(gameboard) {
    @JvmField
    protected var numberOfPets = 0
    @JvmField
    protected var numberOfRows = 0
    @JvmField
    protected var numberOfCols = 0
    @JvmField
    protected var carPosC: Int = -1
    @JvmField
    protected var carPosR: Int = -1
    // we must move the k-th animal from "from[k]" --> "to[k]"
    @JvmField
    protected var from: Array<Pair<Int, Int>> = Array(MAX_NUMBER_OF_PETS) { Pair(0, 0) }
    @JvmField
    protected var to: Array<Pair<Int, Int>> = Array(MAX_NUMBER_OF_PETS) { Pair(0, 0) }
    // distances between nodes
    @JvmField
    protected var d: Array<Array<Int>> =
            Array(MAX_NUMBER_OF_NODES) { Array(MAX_NUMBER_OF_NODES) { HUGE_DISTANCE } }

    @JvmField
    protected var movesCount = 0
    @JvmField
    protected var solution: MutableList<Solution.CarMove>? = null

    init {
        initInternals()
        parseGameboard()
    }

    /**
     * Function that solves puzzle
     * @return Number of steps in solution, if found. -1 otherwise
     */
    override fun solve(): Int {
        Log.d(TAG, "About to solve gameboard")
        // reset solution
        solution = null

        // Maps from vertex to its distance (from start vertex)
        var dist: MutableMap<ShortState, Int> = HashMap()
        // Stores all vertices in ascending order of distance
        var all: OrderedSet<MyPair<Int, State>> = OrderedSet(ArrayList())

        // number of moves
        movesCount = -1

        // Dijkstra
        all.insert(MyPair(0, State(0, carPosC, carPosR, 4)))

        var step = 0
        while (!all.isEmpty()) {
            // debug info
            step += 1
            if (step % 1000 == 1)
                Log.d(TAG, "Step $step: there are ${all.items.size} possibilities to consider")

            var curPair = all.min()!!
            var cur: State = curPair.second
            var l: Int = curPair.first

            all.items.removeAt(0)

            // final vertex, done
            if (cur.mask == POWER_OF_3[numberOfPets] - 1) {
                movesCount = l

                // construct path
                constructSolutionPath(cur)

                break
            }

            if (dist.containsKey(cur.getShortState()) && l != dist[cur.getShortState()])
                continue

            // consider each animal
            for (i in 0 until numberOfPets) {
                // t = 0 if i-th animal is not yet picked up
                // t = 1 if i-th animal is already picked up but not yet returned home
                // t = 2 if i-th animal is already returned home
                val t: Int = getDigit(cur.mask, i)
                var next: State? = null
                var lNext: Int = 0
                if (t == 0 && cur.cap > 0) {
                    // If we still have empty seat, try pick up this animal
                    next = State(setBit(cur.mask, i, 1), from[i].first, from[i].second, cur.cap - 1, cur)
                    lNext = l + d[getPosition(cur.c, cur.r)][getPosition(from[i].first, from[i].second)]
                } else if (t == 1) {
                    // Animal is already picked up, attempt to return him to home
                    next = State(setBit(cur.mask, i, 2), to[i].first, to[i].second, cur.cap + 1, cur)
                    lNext = l + d[getPosition(cur.c, cur.r)][getPosition(to[i].first, to[i].second)]
                }
                // add/replace if distance is lower
                if (next!= null) {
                    var nextShortState: ShortState =  next.getShortState()
                    val existingDistance = dist.getOrDefault(nextShortState, HUGE_DISTANCE)
                    if (dist.getOrDefault(nextShortState, HUGE_DISTANCE) > lNext) {
                        dist[nextShortState] = lNext
                        all.insert(MyPair(lNext, next))
                    }
                }
            }
        }

        Log.d(TAG, "Found solution: $movesCount moves")
        return movesCount
    }

    /**
     * Returns number of steps in solution (if exists or was found), -1 otherwise.
     * @return Number of steps in solution
     */
    override fun getSolutionMovesCount(): Int {
        return if (solution == null) -1 else movesCount
    }

    /**
     * Returns list of steps that solve the puzzle
     * @return List of step if solution exists, null otherwise
     */
    override fun getSolution(): Solution? {
        return Solution(if (solution == null) -1 else movesCount, solution)
    }

    protected fun initInternals() {
        for (i in 0 until MAX_NUMBER_OF_NODES)
            d[i][i] = 0
    }

    /**
     * Parse gameboard to internal structures
     */
    protected fun parseGameboard() {
        Log.d(TAG, "About to parse gameboard")
        numberOfCols = gameboard.gameboard.size
        numberOfRows = gameboard.gameboard[0].size

        // pet type - used below
        var petList = ArrayList<String>()

        for (r in 0 until numberOfRows) {
            for(c in 0 until  numberOfCols) {

                val o = gameboard.gameboard[c][r]!!
                when (o.name[0]) {
                    // car
                    SharedGameObjects.prefixCar[0] -> {
                        carPosC = c
                        carPosR = r
                    }
                    // dot
                    SharedGameObjects.prefixDot[0] -> {
                    }
                    // pet
                    else -> {
                        // get pet type (house/pet) and real pet name (Siamese, Husky...)
                        var petType = o.name[0].toInt()
                        var petName: String = o.name.substring(1, o.name.length - 1)

                        // check if such pet is already in the list, if not - add new
                        var petId = petList.indexOf(petName)
                        if (petId == -1) {
                            petId = petList.size
                            petList.add(petName)
                        }
                        // is pet house
                        if (petType == SharedGameObjects.prefixHouse[0].toInt()) {
                            to[petId] = Pair(c, r)
                        } else {
                            from[petId] = Pair(c, r)
                        }
                    }
                }

                addEgde(c, r, c, r + 1, o.isDown)
                addEgde(c, r, c, r - 1, o.isUp)
                addEgde(c, r, c - 1, r, o.isLeft)
                addEgde(c, r, c + 1, r, o.isRight)
            }
        }

        numberOfPets = petList.size

        updateDistances()
        Log.d(TAG, "Parsed gameboard")
    }

    /**
     * Update distances from (r1, c2) to (r2, c2) is such edge exists
     * @param c1 Column for first position
     * @param r1 Row for first position
     * @param c2 Column for second position
     * @param r2 Row for second position
     * @param exist True if there is an edge that interconnects first and second positions
     */
    protected fun addEgde(c1: Int, r1: Int, c2: Int, r2: Int, exist: Boolean) {
        if (exist) {
            val pos1 = getPosition(c1, r1)
            val pos2 = getPosition(c2, r2)
            d[pos1][pos2] = 1
            d[pos2][pos1] = 1
        }
    }

    protected fun getPosition(c: Int, r: Int): Int = gameboard.get1DPosition(c, r)

    /**
     * Update other distances according to Floyd algorithm
     */
    private fun updateDistances() {
        val numberOfNodes = numberOfCols * numberOfRows
        for (k in 0 until numberOfNodes) {
            for (i in 0 until numberOfNodes) {
                for (j in 0 until numberOfNodes) {
                    d[i][j] = minOf(d[i][j], d[i][k] + d[k][j])
                }
            }
        }
    }

    /** Get the u-th digit in base-3 of the number mask */
    protected fun getDigit(mask: Int, u: Int): Int {
        return (mask % POWER_OF_3[u + 1]) / POWER_OF_3[u]
    }

    /** Set the u-th digit in base-3 of the number mask to new value "val" */
    protected fun setBit(mask: Int, u: Int, value: Int): Int {
        return mask + POWER_OF_3[u] * (value - getDigit(mask, u))
    }

    /**
     * Construct path from the current state.
     * @param cur State of the path
     */
    private fun constructSolutionPath(cur: State) {
        val gb = gameboard.gameboard
        solution = ArrayList()
        var currentElement: State? = cur
        // add all element to list
        while (currentElement != null) {
            solution!!.add(
                Solution.CarMove(
                    gameboard.get1DPosition(currentElement.c, currentElement.r),
                    if (currentElement.prevMove == null) 'i'
                    else if (gb[currentElement.c][currentElement.r]!!.name[0] == SharedGameObjects.prefixPet[0]) 't' else 'p'
                )
            )
            currentElement = currentElement.prevMove
        }
        solution!!.reverse()
        for(i in 1 until solution!!.size) {
            solution!![i].numberOfMoves = solution!![i-1].numberOfMoves + d[solution!![i-1].position][solution!![i].position]
        }
    }

    open inner class ShortState(
        var mask: Int,
        var c: Int,
        var r: Int,
            var cap: Int
    ) : Comparable<ShortState> {
        /** Required by OrderSet for correct comparison */
        override fun compareTo(other: ShortState): Int {
            if (mask != other.mask) return mask.compareTo(other.mask)
            if (c != other.c) return c.compareTo(other.c)
            if (r != other.r) return r.compareTo(other.r)
            return cap.compareTo(other.cap)
        }

        /** Required by HashMap for correct comparison */
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as ShortState
            return (mask == other.mask && c == other.c && r == other.r && cap == other.cap)
        }

        /** Required by HashMap for correct comparison */
        override fun hashCode(): Int {
            return (((mask.hashCode() * 31 + c.hashCode()) * 31 + r.hashCode()) * 31 + cap.hashCode())
        }
    }

    inner class State(
            mask: Int,
            c: Int,
            r: Int,
            cap: Int,
            val prevMove: State? = null
    ) : ShortState(mask, c, r, cap) {

        /** Required by HashMap for correct comparison */
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as State
            return (super.equals(other as ShortState) && prevMove == other.prevMove)
        }

        /** Required by HashMap for correct comparison */
        override fun hashCode(): Int {
            return super.hashCode() * 31 + prevMove.hashCode()
        }

        fun getShortState(): ShortState {
            return ShortState(mask, c, r, cap)
        }
    }

    companion object {
        /** TAG for logging */
        private val TAG = "PDS::DijkstraAlgorithm"
        /** Max number of pets */
        private val MAX_NUMBER_OF_PETS = 12
        /** Max number of nodes */
        private val MAX_NUMBER_OF_NODES = (MAX_NUMBER_OF_PETS + 1) * (MAX_NUMBER_OF_PETS + 1)
        /** Array of 3 */
        private val POWER_OF_3 = listOf<Int>(
            1,
            3,
            9,
            27,
            81,
            243,
            729,
            2187,
            6561,
            19683,
            59049,
            177147,
            531441,
            1594323,
            4782969,
            14348907,
            43046721,
            129140163,
            387420489,
            1162261467
        )
        /** Default big value for distance */
        private val HUGE_DISTANCE = 1000111000

    }

}
