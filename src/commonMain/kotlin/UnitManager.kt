import Grid.isAdjacentTo
import Grid.isWithinRange
import kotlin.coroutines.CoroutineContext

object UnitManager {
    val leftTeam = mutableSetOf<GameUnit>()
    val rightTeam = mutableSetOf<GameUnit>()
    val all
        get() = leftTeam + rightTeam

    suspend fun nextTurn(coroutineContext: CoroutineContext) {
        leftTeam.sortedBy { it.gridPos.x }.reversed().forEach { gu ->
            // If in attack range, start attacking
            if (gu.isAdjacentTo(rightTeam)) {
                // TODO: Start attacking
                return@forEach
            }

            // Check aggro range
            if (gu.isWithinRange(rightTeam, gu.aggroRangeFlat)) {

            }

            // Get next movement square (forward or towards target)
            // Initiate movement
        }
    }
}
