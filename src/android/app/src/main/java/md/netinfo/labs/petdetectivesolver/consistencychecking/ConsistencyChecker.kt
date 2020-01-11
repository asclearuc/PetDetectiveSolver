package md.netinfo.labs.petdetectivesolver.consistencychecking

import android.content.Context
import android.util.Log

import md.netinfo.labs.petdetectivesolver.R
import md.netinfo.labs.petdetectivesolver.exceptions.WrongNumberOfPetsException
import md.netinfo.labs.petdetectivesolver.exceptions.WrongPetException
import md.netinfo.labs.petdetectivesolver.exceptions.CarNotFoundException
import md.netinfo.labs.petdetectivesolver.exceptions.IncorrectGameboardException
import md.netinfo.labs.petdetectivesolver.gameboard.GameBoard
import md.netinfo.labs.petdetectivesolver.imageprocessing.FoundObjectInfo
import md.netinfo.labs.petdetectivesolver.resources.GameObjects
import md.netinfo.labs.petdetectivesolver.resources.SharedGameObjects

/**
 * Class to detect if recognized object can form a valit Pet Detective challenge.
 * There are different constraints - like intersections, total number of pets,
 * order of pets and so on ...
 *
 * @param context Context of the application, needed to access resources
 * @param foundPets Map of found pets/pet houses/car
 * @param foundDots List of found dots
 */
class ConsistencyChecker(val context: Context) {
    /**
     * Main function.
     * Starts checking according to different constraints
     * @exception
     * @return True if all checks passed
     */
    public fun check(foundPets: Map<String, FoundObjectInfo>, foundDots: List<FoundObjectInfo>): Boolean {
        var result = false

        do {
            if (!checkNumbersOfDetectedObjects(foundPets, foundDots))
                break
            if (!checkOrderOfDetectedObjects(foundPets))
                break

            result = true
        } while (false)

        return result
    }

    /**
     * Check number of detected objects according to the constraints
     *  - total number of pets
     *  - sum of pets and dots
     * @return Returns true if detected object satisfy constraint
     */
    private fun checkNumbersOfDetectedObjects(foundPets: Map<String, FoundObjectInfo>, foundDots: List<FoundObjectInfo>): Boolean {
        // between 5 && 23 (including car)
        if (foundPets.size < NUMBER_OF_PETS_MIN || foundPets.size > NUMBER_OF_PETS_MAX)
            throw WrongNumberOfPetsException(
                context.getString(
                    R.string.check_wrong_number_of_pets,
                    foundPets.size,
                    NUMBER_OF_PETS_MIN,
                    NUMBER_OF_PETS_MAX
                )
            )

        val totalObjects = foundPets.size + foundDots.size
        if(!LIST_OF_SIZES.contains(totalObjects))
            throw WrongNumberOfPetsException(
                context.getString(
                    R.string.check_wrong_field_size,
                    foundPets.size,
                    foundDots.size
                )
            )

        return true
    }

    /**
     * Check if there theoretically could be a challenge with detected number of pets and dots
     *
     * @exception WrongPetException If such configuration could not exist
     * @return True if the configuration can satisfy a challenge in the game
     */
    private fun checkOrderOfDetectedObjects(foundPets: Map<String, FoundObjectInfo>): Boolean {
        val numberOfPets: Int = (foundPets.size - 1)/2
        Log.d(TAG, "Found $numberOfPets pets")

        var i: Int = 0
        while (i < numberOfPets) {
            if(!foundPets.containsKey("${SharedGameObjects.prefixPet}${GameObjects.petNames[i]}")
                || !foundPets.containsKey("${SharedGameObjects.prefixHouse}${GameObjects.petNames[i]}")) {
                throw WrongPetException(context.getString(
                        R.string.check_mandatory_pet_not_found,
                        i - 1,
                        GameObjects.petNames[i]
                    ))
            }
            i += 1
        }

        val isCar = foundPets.keys.map{it[0]}.contains(SharedGameObjects.prefixCar[0])
        if (!isCar) {
            throw CarNotFoundException(context.getString(
                    R.string.check_car_not_found,
                    i - 1
                ))
        }

        return true
    }

    /**
     * Checks validity of the gameboard.
     * Gameboard is valid if
     * - there are no link outside of the gameboard
     * - two neighboring objects are correctly interconnected (e.g. either both point to each other, or both of them do not point to each other)
     * @param gameboard Gameboard to check
     * @exception IncorrectGameboardException In case if gameboard is not valid.
     */
    fun check(gameboard: GameBoard) {
        checkEdges(gameboard)
        checkInternals(gameboard)
    }

    /**
     * Checks if there is a link that points outside of the gameboard
     * @param gameboard Gameboard to check
     * @exception IncorrectGameboardException In case if gameboard is not valid.
     */
    fun checkEdges(gameboard: GameBoard) {
        val numberOfRows = gameboard.numberOfRows
        val numberOfCols = gameboard.numberOfCols
        val gbd = gameboard.gbd.gameboard

        // check out-of-board on first and last column
        for (r in 0 until numberOfRows) {
            if (gbd[0][r]!!.isLeft) {
                throw IncorrectGameboardException(
                    context.getString(
                        R.string.check_link_out_of_board,
                        0,
                        r,
                        gbd[0][r]!!.name,
                        context.getString(R.string.check_link_direction_left)
                    )
                )
            }
            if (gbd[numberOfCols-1][r]!!.isRight) {
                throw IncorrectGameboardException(
                    context.getString(
                        R.string.check_link_out_of_board,
                        0,
                        r,
                        gbd[numberOfCols-1][r]!!.name,
                        context.getString(R.string.check_link_direction_right)
                    )
                )
            }
        }

        // check out-of-board on first and last row
        for (c in 0 until numberOfCols) {
            if (gbd[c][0]!!.isUp) {
                throw IncorrectGameboardException(
                    context.getString(
                        R.string.check_link_out_of_board,
                        0,
                        c,
                        gbd[c][0]!!.name,
                        context.getString(R.string.check_link_direction_up)
                    )
                )
            }
            if (gbd[c][numberOfRows-1]!!.isDown) {
                throw IncorrectGameboardException(
                    context.getString(
                        R.string.check_link_out_of_board,
                        0,
                        c,
                        gbd[c][numberOfRows-1]!!.name,
                        context.getString(R.string.check_link_direction_down)
                    )
                )
            }
        }
    }

    /**
     * Checks if two neighboring objects are correctly interconnected
     * (e.g. either both point to each other, or both of them do not point to each other)
     * @param gameboard Gameboard to check
     * @exception IncorrectGameboardException In case if gameboard is not valid.
     */
    fun checkInternals(gameboard: GameBoard) {
        val numberOfRows = gameboard.numberOfRows
        val numberOfCols = gameboard.numberOfCols
        val gbd = gameboard.gbd.gameboard

        // check left-right
        for (c in 0 until numberOfCols - 1) {
            for (r in 0 until numberOfRows) {
                if (gbd[c][r]!!.isRight != gbd[c + 1][r]!!.isLeft) {
                    throw IncorrectGameboardException(
                        context.getString(
                            R.string.check_uncorrelated_links,
                            c, r, gbd[c][r]!!.name,
                            c + 1, r, gbd[c + 1][r]!!.name,
                            context.getString(R.string.check_link_direction_left),
                            context.getString(R.string.check_link_direction_right)
                        )
                    )
                }
            }
        }
        // check up-down
        for (c in 0 until numberOfCols) {
            for (r in 0 until numberOfRows - 1) {
                if (gbd[c][r]!!.isDown != gbd[c][r + 1]!!.isUp) {
                    throw IncorrectGameboardException(
                        context.getString(
                            R.string.check_uncorrelated_links,
                            c, r, gbd[c][r]!!.name,
                            c + 1, r, gbd[c][r + 1]!!.name,
                            context.getString(R.string.check_link_direction_up),
                            context.getString(R.string.check_link_direction_down)
                        )
                    )
                }
            }
        }
    }

    companion object {
        /** TAG for logging */
        private val TAG = "PDS::ConsistencyChecker"
        /** Min number of pets and pet houses (including car) */
        val NUMBER_OF_PETS_MIN = 5
        /** Max number of pets and pet houses (including car) */
        val NUMBER_OF_PETS_MAX = 23
        /** List of all possible sizes for the field */
        val LIST_OF_SIZES = listOf<Int>(9, 12, 15, 18, 20, 24)
    }

}