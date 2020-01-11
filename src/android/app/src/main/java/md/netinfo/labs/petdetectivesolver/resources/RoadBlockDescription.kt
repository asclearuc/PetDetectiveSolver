package md.netinfo.labs.petdetectivesolver.resources

data class RoadBlockDescription(val name: String,
                                val isUp: Boolean, val isDown: Boolean,
                                val isRight: Boolean, val isLeft: Boolean)
{}
