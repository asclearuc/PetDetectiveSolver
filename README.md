# PetDetectiveSolver
Solver for Lumosity's Pet Detective challenge

## Why this program
The program is an answer to several of my challenges:
* Sometimes, I got stuck solving Pet Detective puzzles. Some of the solutions are not obvious and hard to find - but you always want to solve the puzzle.
* Computer vision is interesting for me, so OpenCV was used ;) For PoC version (that screenshot could be parsed) - Python was used
* I play Lumosity on my phone, so this should be Android program, and Kotlin is #1 language for Android.
* Server-side solution (not supplied at this moment) is written using Java Spring

## Implementation
Pet Detective Solver uses OpenCV (v4.1.0 - supplied with sources), to the supplied image (the image is checked against all pets and their houses).

Next stage - finding the car on the image (the car could be pointing in one of four directions).

Roads on image (and type of intersections) are detected using "dumb" comparision (comparing background colors).

## Tested
The program was tested on Google Nexus 5 and OnePlus 3.

## Known problems
There are different issues in the current version of the program, which could impair with the process of finding solution:
 * Sometimes not all "empty" intersections are found. OpenCV is used to find them - maybe some parameters for OpenCV should be adjusted?
 * Sometimes road/road intersections are not correctly recognized. Maybe the algorithm should be changed? 
 * The algorithm used to find the path (Dijkstra) is slow. Either it should be optimized, or something else should be used. Especially it could be seen on levels 12 and above. For example, one of challenges on Level 15 took over 6 minutes to be solved on OnePlus 3.
## Contributing
This project welcomes contributions and suggestions. The app could (and should) be much better.

