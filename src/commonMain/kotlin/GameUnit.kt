import Grid.getPathTo
import Grid.gridAdjacentTo
import Grid.gridPos
import Grid.prepareToMove
import Grid.totalDistance
import com.soywiz.klock.milliseconds
import com.soywiz.klock.seconds
import com.soywiz.klock.timesPerSecond
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.View
import com.soywiz.korge.view.addFixedUpdater
import com.soywiz.korge.view.addTo
import com.soywiz.korge.view.addUpdater
import com.soywiz.korge.view.center
import com.soywiz.korge.view.image
import com.soywiz.korge.view.name
import com.soywiz.korge.view.rotation
import com.soywiz.korge.view.tween.moveTo
import com.soywiz.korge.view.tween.rotateTo
import com.soywiz.korge.view.xy
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korio.async.launchAsap
import com.soywiz.korio.async.launchImmediately
import com.soywiz.korio.lang.Cancellable
import com.soywiz.korio.lang.cancel
import com.soywiz.korma.geom.Angle
import com.soywiz.korma.geom.cosine
import com.soywiz.korma.geom.degrees
import com.soywiz.korma.geom.sine
import com.soywiz.korma.interpolation.Easing
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

class GameUnit(
    private val ctx: CoroutineContext,
    parent: Container,
    name: String,
    initialRotation: Int,
    initialX: Int,
    initialY: Int,
    private val enemyTeam: Set<GameUnit>,
    bitmap: Bitmap
) {
    //    private val image: View = parent.image(bitmap.sliceWithSize(100, 100, 50, 50))
    val image: View = parent.image(bitmap)
        .center()
        .name(name)
        .xy(initialX, initialY)
        .rotation(initialRotation.degrees)
        .addTo(parent)
    private var target: GameUnit? = null
    private var attackJob: Cancellable? = null
    private var moveCommand: Job? = null
    private var isAttacking = false
    var gridPos = image.gridPos()

    init {
        image.scaledWidth = (cellSize - 10).toDouble()
        image.scaledHeight = (cellSize - 10).toDouble()

        image.addUpdater {
            acquireTarget()

            if (target == null || (moveCommand != null && !moveCommand!!.isCompleted)) {
                return@addUpdater
            }

            if (gridAdjacentTo(target!!)) {
                isAttacking = true
                if (attackJob == null) {
                    launchImmediately(ctx) {
                        image.rotateTo(Angle.between(gridPos, target!!.gridPos), time = 150.milliseconds, easing = Easing.SMOOTH)
                        attackJob = addFixedUpdater(1.timesPerSecond) {
//                            println("[$name]: attack")
                        }
                    }
                }
            } else {
                isAttacking = false
                attackJob?.cancel()
                val path = getPathTo(target!!)
                if (path.isEmpty()) {
                    return@addUpdater
                }
                val nextNode = path[0]
//                val nextNode = gridPos.getNextStepTowards(target!!.gridPos)!!
                if (prepareToMove(nextNode)) {
                    moveCommand = launchAsap(ctx) {
                        image.rotateTo(Angle.between(gridPos, nextNode), time = 150.milliseconds, easing = Easing.SMOOTH)
                        gridPos = nextNode
                        image.moveTo(nextNode.worldX, nextNode.worldY, time = 1.seconds, easing = Easing.SMOOTH)
                    }
                }
            }
        }
    }

    private fun acquireTarget() {
        target = enemyTeam.minByOrNull { this.getPathTo(it).totalDistance() }
    }
}

fun View.advance(amount: Double) = this.apply {
    x += (rotation).cosine * amount
    y += (rotation).sine * amount
}
