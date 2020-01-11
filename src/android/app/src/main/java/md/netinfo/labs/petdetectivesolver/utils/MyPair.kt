package md.netinfo.labs.petdetectivesolver.utils

class MyPair<A:Comparable<A>, B:Comparable<B>> (var first: A, var second: B): Comparable<MyPair<A, B>> {
    override fun compareTo(other: MyPair<A, B>): Int {
        if(first != other.first) return first.compareTo(other.first)
        return second.compareTo(other.second)
    }
}
