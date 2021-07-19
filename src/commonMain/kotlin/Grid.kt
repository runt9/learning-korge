
import com.soywiz.kds.Array2
import com.soywiz.korma.geom.IPoint
import com.soywiz.korma.geom.Point
import com.soywiz.korma.math.betweenInclusive
import com.soywiz.korma.math.roundDecimalPlaces

object Grid {
    private val grid = Array2.withGen(gridWidth, gridHeight) { x, y -> GridPoint(x.toDouble(), y.toDouble()) }

    private fun getFromGrid(x: Int, y: Int): GridPoint {
        return grid[x, y]
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
