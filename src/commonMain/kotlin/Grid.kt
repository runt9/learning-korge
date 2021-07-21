import com.soywiz.kds.Array2
import com.soywiz.kds.intArrayListOf
import com.soywiz.korge.view.View
import com.soywiz.korma.algo.AStar
import com.soywiz.korma.geom.IPoint
import com.soywiz.korma.geom.Point
import com.soywiz.korma.geom.distanceTo
import com.soywiz.korma.geom.toPoints
import com.soywiz.korma.math.betweenInclusive
import com.soywiz.korma.math.roundDecimalPlaces
import kotlin.math.abs

object Grid {
    private val grid = Array2.withGen(gridWidth, gridHeight) { x, y -> GridPoint(x.toDouble(), y.toDouble()) }

    private fun getFromGrid(x: Int, y: Int): GridPoint {
        return grid[x, y]
    }

    private fun getFromGrid(gridPoint: GridPoint): GridPoint {
        return getFromGrid(gridPoint.x.toInt(), gridPoint.y.toInt())
    }

    fun addRandomlyToGrid(minX: Int, maxX: Int): GridPoint {
        val randomOpenSpot = grid.filter {
            it.x.betweenInclusive(minX.toDouble(), maxX.toDouble()) && !it.isBlocked
        }.random()

        return addToGrid(randomOpenSpot.x.toInt(), randomOpenSpot.y.toInt())
    }

    fun addToGrid(x: Int, y: Int): GridPoint {
        val point = getFromGrid(x, y)
        point.isBlocked = true
        return point
    }

    fun View.gridPos(): GridPoint {
        return pos.toGridPoint()
    }

    private fun Point.toGridPoint(): GridPoint {
        return GridPoint.fromWorldPoint(this)
    }

    fun GameUnit.prepareToMove(toPoint: GridPoint): Boolean {
        val current = gridPos
        if (getFromGrid(toPoint).isBlocked) {
            return false
        }

        getFromGrid(current).isBlocked = false
        getFromGrid(toPoint).isBlocked = true
        return true
    }

    fun GridPoint.getNextStepTowards(other: GridPoint, path: MutableList<GridPoint>): GridPoint? {
        return getNextPossibleStepsToward(other, path).minByOrNull { it.distanceTo(other) }
    }

    fun GridPoint.getNextPossibleStepsToward(other: GridPoint, path: List<GridPoint>): List<GridPoint> {
        return grid.filter { !path.contains(it) && it != this && (it == other || !it.isBlocked) && abs(it.x - x) <= 1 && abs(it.y - y) <= 1 }
    }

    fun GameUnit.getPathTo(other: GameUnit): MutableList<GridPoint> {
        val pointToTarget = if (other.isAttacking || other.movingToGridPos == null) other.gridPos else other.movingToGridPos!!
        // In path, if next 2 nodes end up on a different adjacent, drop the first node
        val path = gridPos.getPathTo(pointToTarget)
        if (path.size > 1) {
            if (path[1].isAdjacentTo(gridPos)) {
                path.removeFirst()
            }
        }

        return path
    }

    fun GridPoint.getPathTo(other: GridPoint): MutableList<GridPoint> {
        return AStar(gridWidth, gridHeight) { pathX, pathY ->
            !(pathX == x.toInt() && pathY == y.toInt()) && grid[pathX, pathY].isBlocked
        }.find(x.toInt(), y.toInt(), other.x.toInt(), other.y.toInt(), findClosest = true, diagonals = false)
            .toPoints()
            .drop(1)
            .map { GridPoint(it.x.toDouble(), it.y.toDouble()) }
            .toMutableList()
    }

    fun List<GridPoint>.totalDistance(): Double {
        var distance = 0.0
        forEachIndexed { index, gridPoint ->
            if (index != size - 1) {
                distance += gridPoint.distanceTo(elementAt(index + 1))
            }
        }
        return distance
    }

    fun List<GridPoint>.isNowBlocked() = any { grid[it.x.toInt(), it.y.toInt()].isBlocked }

    fun GameUnit.gridDistance(other: GameUnit) = gridPos.distanceTo(other.gridPos)

    fun GameUnit.isAdjacentTo(others: Collection<GameUnit>): Boolean {
        return others.any { it.isAdjacentTo(this) }
    }

    fun GameUnit.isAdjacentTo(other: GameUnit): Boolean {
        val selfPos = gridPos
        val otherPos = other.gridPos
        return selfPos.isAdjacentTo(otherPos)
    }

    fun GridPoint.isAdjacentTo(other: GridPoint): Boolean {
        return isWithinRange(other, 1)
    }

    fun GridPoint.isAdjacentTo(other: Collection<GridPoint>): Boolean {
        return other.any { this.isWithinRange(it, 1) }
    }

    fun GameUnit.isWithinRange(others: Collection<GameUnit>, range: Int): Boolean {
        return others.any { it.isWithinRange(this, range) }
    }

    fun GameUnit.isWithinRange(other: GameUnit, range: Int): Boolean {
        val selfPos = gridPos
        val otherPos = other.gridPos
        return selfPos.isWithinRange(otherPos, range)
    }

    fun GridPoint.isWithinRange(other: GridPoint, range: Int): Boolean {
        return abs(x - other.x) <= range && abs(y - other.y) <= range
    }

    fun blockPos(pos: GridPoint) {
        getFromGrid(pos).isBlocked = true
    }

    fun unblockPos(pos: GridPoint) {
        getFromGrid(pos).isBlocked = false
    }

    fun GameUnit.diagonalTo(other: GameUnit) = abs(gridPos.x - other.gridPos.x) == 1.0 && abs(gridPos.y - other.gridPos.y) == 1.0

    fun nextForward(gu: GameUnit): GridPoint {
        val pos = gu.gridPos
        val rx = ((gridWidth - 1) / 2.0).compareTo(pos.x)
        val ry = findDirectionOfNextColumnOpen(gu)

        intArrayListOf(rx, 0, -rx).forEach { nextX ->
            intArrayListOf(0, ry, -ry).forEach { nextY ->
                grid.tryGet((pos.x + nextX).toInt(), (pos.y + nextY).toInt())?.let { testPoint ->
                    if (!testPoint.isBlocked && testPoint != pos && testPoint != gu.previousGridPos && !crossesExistingMovement(pos, testPoint)) {
                        return testPoint
                    }
                }
            }
        }

        return pos
    }

    private fun findDirectionOfNextColumnOpen(gu: GameUnit): Int {
        val rx = ((gridWidth - 1) / 2.0).compareTo(gu.gridPos.x)

        var nextClosestYPoint = grid.filter { it.x == gu.gridPos.x + rx && !it.isBlocked }.minByOrNull { abs(it.y - (gridHeight / 2)) }
        if (nextClosestYPoint == null) {
            nextClosestYPoint = grid.filter { it.x == gu.gridPos.x && !it.isBlocked }.minByOrNull { abs(it.y - (gridHeight / 2)) }
        }
        return nextClosestYPoint?.y?.compareTo(gu.gridPos.y) ?: 0
    }

    fun nextTowardsTarget(gu: GameUnit): GridPoint? {
        val pos = gu.gridPos
        val rx = ((gridWidth - 1) / 2.0).compareTo(pos.x)
//        val ry = ((gridHeight - 1) / 2.0).compareTo(pos.y)
        val ry = findDirectionOfNextColumnOpen(gu)
        val safeTarget = gu.target!!
        val targetPos = if (safeTarget.movingToGridPos != null) safeTarget.movingToGridPos!! else safeTarget.gridPos
        if (pos.isAdjacentTo(targetPos)) {
            return pos
        }

        val baseDistance = pos.manhattanDistance(targetPos)
        var shortestDistance = Double.MAX_VALUE
        var possibleFirstNode: GridPoint? = null
        val yList = if (ry == 0) intArrayListOf(0, 1, -1) else intArrayListOf(0, ry, -ry)
        intArrayListOf(rx, 0, -rx).forEach { nextX ->
            yList.forEach { nextY ->
                grid.tryGet((pos.x + nextX).toInt(), (pos.y + nextY).toInt())?.let { testPoint ->
                    if (testPoint.isBlocked || testPoint == gu.previousGridPos || testPoint == pos || crossesExistingMovement(pos, testPoint)) {
                        return@let
                    }

                    if (testPoint.isAdjacentTo(targetPos)) {
                        return testPoint
                    }

                    // TODO: Maybe not the right spot for this
                    // If the tile is adjacent to a non-target enemy, change target to that enemy and return the point
                    val possibleNewTargets =
                        gu.enemyTeam.filter { testPoint.isAdjacentTo(it.gridPos) || (it.movingToGridPos != null && testPoint.isAdjacentTo(it.movingToGridPos!!)) }
                    if (possibleNewTargets.isNotEmpty()) {
                        gu.target = possibleNewTargets.first()
                        return testPoint
                    }

                    val distance = testPoint.manhattanDistance(targetPos)
                    if (distance < baseDistance || (distance < shortestDistance && checkNextStep(testPoint, distance, listOf(pos), targetPos, gu.enemyTeam))) {
                        shortestDistance = distance
                        possibleFirstNode = testPoint

                    }

                    // TODO: Can likely optimize by stopping after hitting like 2 longer distances in a row or something
                }
            }
        }

        return possibleFirstNode
    }

    private fun crossesExistingMovement(start: GridPoint, end: GridPoint) = UnitManager.all
        .filter { it.movingToGridPos != null && it.gridPos != start && it.gridPos != end }
        .any { gu ->
            val guPos = gu.gridPos
            val guMove = gu.movingToGridPos!!

            abs(guPos.x - guMove.x) == 1.0 &&
                    abs(guPos.y - guMove.y) == 1.0 &&
                    abs(start.x - end.x) == 1.0 &&
                    abs(start.y - end.y) == 1.0 &&
                    guPos.isAdjacentTo(start) &&
                    gu.gridPos.isAdjacentTo(end) &&
                    guMove.isAdjacentTo(start) &&
                    guMove.isAdjacentTo(end)
        }

    private fun checkNextStep(
        startingPoint: GridPoint,
        bestDistanceSoFar: Double,
        previousPoints: Collection<GridPoint>,
        targetPos: GridPoint,
        enemies: Collection<GameUnit>
    ): Boolean {
        val rx = ((gridWidth - 1) / 2.0).compareTo(startingPoint.x)
        val ry = ((gridWidth - 1) / 2.0).compareTo(startingPoint.y)

        intArrayListOf(rx, 0, -rx).forEach { nextX ->
            intArrayListOf(0, ry, -ry).forEach { nextY ->
                grid.tryGet((startingPoint.x + nextX).toInt(), (startingPoint.y + nextY).toInt())?.let { testPoint ->
                    if (testPoint.isBlocked || previousPoints.any { it == testPoint } || testPoint == startingPoint) {
                        return@let
                    }

                    if (testPoint.isAdjacentTo(targetPos)) {
                        return true
                    }

                    val possibleNewTargets =
                        enemies.filter { testPoint.isAdjacentTo(it.gridPos) || (it.movingToGridPos != null && testPoint.isAdjacentTo(it.movingToGridPos!!)) }
                    if (possibleNewTargets.isNotEmpty()) {
                        return true
                    }

                    val distance = testPoint.manhattanDistance(targetPos)
                    if (distance < bestDistanceSoFar && checkNextStep(testPoint, distance, previousPoints + testPoint, targetPos, enemies)) {
                        return true
                    }

                    // TODO: Can likely optimize by stopping after hitting like 2 longer distances in a row or something
                }
            }
        }

        return false
    }
}

class GridPoint(
    override val x: Double,
    override val y: Double,
    var isBlocked: Boolean = false,
    val worldX: Double = x * cellSize + cellSize / 2,
    val worldY: Double = y * cellSize + cellSize / 2
) : IPoint {
    companion object {
        fun fromWorldPoint(point: Point) =
            GridPoint(((point.x - cellSize / 2) / cellSize).roundDecimalPlaces(0), ((point.y - cellSize / 2) / cellSize).roundDecimalPlaces(0))
    }

    fun manhattanDistance(other: GridPoint) = abs(x - other.x) + abs(y - other.y)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as GridPoint

        if (x != other.x) return false
        if (y != other.y) return false

        return true
    }

    override fun hashCode(): Int {
        var result = x.hashCode()
        result = 31 * result + y.hashCode()
        return result
    }

    override fun toString(): String {
        return "GridPoint(x=$x, y=$y)"
    }
}
