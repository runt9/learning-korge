
import Grid.isAdjacentTo
import Grid.isWithinRange
import com.soywiz.klock.milliseconds
import com.soywiz.klock.seconds
import com.soywiz.korge.view.tween.hide
import com.soywiz.korge.view.tween.moveTo
import com.soywiz.korge.view.tween.rotateTo
import com.soywiz.korio.async.launchImmediately
import com.soywiz.korma.geom.Angle
import com.soywiz.korma.interpolation.Easing
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

object UnitManager {
    val leftTeam = mutableSetOf<GameUnit>()
    val rightTeam = mutableSetOf<GameUnit>()
    val all
        get() = leftTeam + rightTeam
    lateinit var onBattleComplete: (String) -> Unit

    suspend fun nextTurn(coroutineContext: CoroutineContext) {
        all.filter { it.movingToGridPos != null }.forEach { gu ->
            println("[${gu.name}]: Resetting for new turn")
            gu.previousGridPos = gu.gridPos
            gu.gridPos = gu.movingToGridPos!!
            gu.movingToGridPos = null
        }

        leftTeam.sortedBy { it.gridPos.x }.reversed().forEach { gu ->
            launchImmediately(coroutineContext) {
                handleNextTurnForUnit(gu)
            }
        }

        rightTeam.sortedBy { it.gridPos.x }.forEach { gu ->
            launchImmediately(coroutineContext) {
                handleNextTurnForUnit(gu)
            }
        }
    }

    suspend fun handleNextTurnForUnit(gu: GameUnit) {
        // If in attack range, start attacking
        if (gu.isAdjacentTo(gu.enemyTeam)) {
            if (gu.target == null || !gu.isAdjacentTo(gu.target!!)) {
                gu.target = gu.enemyTeam.find { gu.isAdjacentTo(it) }
            }

            println("[${gu.name}]: Attacking so not moving")
            gu.body.rotateTo(Angle.Companion.between(gu.gridPos, gu.target!!.gridPos), time = 150.milliseconds)
            gu.startAttacking()
            return
        }

        gu.cancelAttacking()

        // Check aggro range
        if (gu.target == null) {
            if (gu.isWithinRange(gu.enemyTeam, gu.aggroRangeFlat)) {
                gu.target = gu.enemyTeam.find { gu.isWithinRange(it, gu.aggroRangeFlat) }
                println("[${gu.name}]: Acquired target [${gu.target?.name}]")
            } else {
                gu.aggroRangeFlat++
                println("[${gu.name}]: Aggro range increased")
            }
        }

        // Get next movement square (forward or towards target)
        var nextPoint = if (gu.target != null) {
            Grid.nextTowardsTarget(gu)
        } else {
            Grid.nextForward(gu)
        }

        if (nextPoint == null) {
            println("[${gu.name}]: Path to target blocked, resetting")
            gu.target = null
            nextPoint = Grid.nextForward(gu)
        }

        // Initiate movement
        if (nextPoint != gu.gridPos) {
            println("[${gu.name}]: Moving from [${gu.gridPos}] to [${nextPoint}]")
            gu.movingToGridPos = nextPoint
            Grid.unblockPos(gu.gridPos)
            Grid.blockPos(nextPoint)
            val newAngle = Angle.Companion.between(gu.gridPos, nextPoint)
            val distance = gu.gridPos.manhattanDistance(nextPoint)

            launchImmediately(coroutineContext) {
                gu.body.rotateTo(newAngle, time = 150.milliseconds)
            }

            launchImmediately(coroutineContext) {
                gu.moveTo(
                    nextPoint.worldX,
                    nextPoint.worldY,
                    time = 1.seconds - (200.milliseconds / distance),
                    easing = Easing.LINEAR
                )
            }
        }
    }

    suspend fun unitKilled(t: GameUnit) {
        Grid.unblockPos(t.gridPos)
        leftTeam.remove(t)
        rightTeam.remove(t)
        all.filter { it.target == t }.forEach { it.target = null }
        t.hide()
        t.removeFromParent()

        if (leftTeam.isEmpty()) {
            onBattleComplete("Team2")
        } else if (rightTeam.isEmpty()) {
            onBattleComplete("Team1")
        }
    }

    fun onBattleComplete(callback: (String) -> Unit) {
        onBattleComplete = callback
    }
}
