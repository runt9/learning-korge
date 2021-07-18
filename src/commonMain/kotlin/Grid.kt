import com.soywiz.kds.Array2
import com.soywiz.korge.view.View
import com.soywiz.korma.geom.IPoint
import com.soywiz.korma.geom.Point
import com.soywiz.korma.geom.distanceTo
import com.soywiz.korma.geom.ds.get
import com.soywiz.korma.math.betweenInclusive
import com.soywiz.korma.math.roundDecimalPlaces
import kotlin.math.abs

object Grid {
    private val grid = Array2.withGen(gridWidth, gridHeight) { x, y -> GridPoint(x.toDouble(), y.toDouble()) }

    private fun getFromGrid(x: Int, y: Int): GridPoint {
        return grid[x, y]
    }

    private fun getFromGrid(gridPoint: GridPoint): GridPoint {
        return grid[gridPoint]
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

    fun GameUnit.getPathTo(other: GameUnit): List<GridPoint> {
        return gridPos.getPathTo(other.gridPos)
    }

    fun GridPoint.getPathTo(other: GridPoint): List<GridPoint> {
        var shortestPath = listOf<GridPoint>()
        var shortestDistance = Double.MAX_VALUE

        getNextPossibleStepsToward(other, emptyList()).forEach { nextPossibleStep ->
            if (nextPossibleStep == other) {
                return@getPathTo emptyList()
            }
            val path = mutableListOf<GridPoint>()
            var nextPoint: GridPoint? = nextPossibleStep
            while (nextPoint != null && !path.contains(nextPoint) && path.totalDistance() < shortestDistance) {
                path += nextPoint
                nextPoint = nextPoint.getNextStepTowards(other, path)

                if (nextPoint == other) {
                    path.add(nextPoint)
                    break
                }
            }

            if (!path.contains(other)) {
                return@forEach
            }

            val pathTotal = path.totalDistance()
            if (pathTotal < shortestDistance) {
//                path.removeAll { it == other }
                shortestPath = path
                shortestDistance = pathTotal
            }
        }

        return shortestPath
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

    fun GameUnit.gridDistance(other: GameUnit) = gridPos.distanceTo(other.gridPos)

    fun GameUnit.gridAdjacentTo(other: GameUnit): Boolean {
        val selfPos = gridPos
        val otherPos = other.gridPos
        return abs(selfPos.x - otherPos.x) <= 1 && abs(selfPos.y - otherPos.y) <= 1
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
