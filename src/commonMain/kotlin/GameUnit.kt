import Grid.getPathTo
import Grid.gridAdjacentTo
import Grid.gridPos
import Grid.isNowBlocked
import Grid.totalDistance
import com.soywiz.klock.milliseconds
import com.soywiz.klock.timesPerSecond
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.Image
import com.soywiz.korge.view.View
import com.soywiz.korge.view.addFixedUpdater
import com.soywiz.korge.view.addTo
import com.soywiz.korge.view.addUpdater
import com.soywiz.korge.view.center
import com.soywiz.korge.view.image
import com.soywiz.korge.view.name
import com.soywiz.korge.view.rotation
import com.soywiz.korge.view.tween.rotateTo
import com.soywiz.korge.view.xy
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korim.bitmap.context2d
import com.soywiz.korim.color.Colors
import com.soywiz.korio.async.launch
import com.soywiz.korio.async.launchImmediately
import com.soywiz.korio.lang.Cancellable
import com.soywiz.korio.lang.cancel
import com.soywiz.korma.geom.Angle
import com.soywiz.korma.geom.cosine
import com.soywiz.korma.geom.degrees
import com.soywiz.korma.geom.sine
import com.soywiz.korma.geom.vector.LineCap
import com.soywiz.korma.interpolation.Easing
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

class GameUnit(
    private val ctx: CoroutineContext,
    private val parent: Container,
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
    var isAttacking = false
    private var currentPath: MutableList<GridPoint> = mutableListOf()
    private var currentPathGraphic: Image? = null
    var gridPos = image.gridPos()
    var movingToGridPos: GridPoint? = null

    init {
        image.scaledWidth = (cellSize - 10).toDouble()
        image.scaledHeight = (cellSize - 10).toDouble()

        image.addUpdater {
            val scale = it / 16.milliseconds

            if (target == null) {
                refreshTargetAndPath()
                return@addUpdater
            }

            if (gridAdjacentTo(target!!)) {
                isAttacking = true
                currentPathGraphic?.removeFromParent()
                if (attackJob == null) {
                    launchImmediately(ctx) {
                        image.rotateTo(Angle.between(image.pos, target!!.image.pos), time = 150.milliseconds, easing = Easing.SMOOTH)
                        attackJob = addFixedUpdater(1.timesPerSecond) {
//                            println("[$name]: attack")
                        }
                    }
                }
            } else {
                isAttacking = false
                attackJob?.cancel()

                if (atDestination()) {
                    finishPreviousMovement()
                }

                if (movingToGridPos == null || atDestination()) {
                    beginNextMovement()
                }

                movingToGridPos?.run {
                    advance(0.25 * scale)
                }
            }
        }
    }

    private fun atDestination(): Boolean {
        return movingToGridPos?.let { image.pos.distanceTo(it.worldX, it.worldY) <= 5 } ?: false
    }

    private fun acquireTarget() {
        target = enemyTeam.minByOrNull { this.getPathTo(it).totalDistance() }
    }

    private fun refreshTargetAndPath() {
        val previousTarget = target
        acquireTarget()
        target?.let {
            if (previousTarget == it && !currentPath.isNowBlocked() && it.isAttacking) {
                return@let
            }

            currentPath = getPathTo(it).toMutableList()
            currentPathGraphic?.removeFromParent()
            currentPathGraphic = parent.image(NativeImage(640, 360).context2d {
                lineWidth = 0.05
                lineCap = LineCap.ROUND
                stroke(Colors.WHITE) {
                    moveTo(image.x, image.y)
                    currentPath.forEach { gp ->
                        lineTo(gp.worldX, gp.worldY)
                    }
                }
            })
        }
    }

    private fun beginNextMovement() {
        refreshTargetAndPath()
        movingToGridPos = if (currentPath.isNotEmpty()) currentPath.removeFirst() else null

        movingToGridPos?.let { nextNode ->
            println("[${image.name} | ($gridPos)]: Moving to [$nextNode]")
            Grid.blockPos(nextNode)
            launch(ctx) {
                image.rotateTo(Angle.between(gridPos, nextNode), time = 50.milliseconds, easing = Easing.SMOOTH)
            }
        }
    }

    private fun finishPreviousMovement() {
        movingToGridPos?.let { nextNode ->
            println("[${image.name} | ($gridPos)]: COMPLETE: Moving to [$nextNode]")
            Grid.unblockPos(gridPos)
            gridPos = nextNode
        }
    }
}

fun View.advance(amount: Double) = this.apply {
    x += (rotation).cosine * amount
    y += (rotation).sine * amount
}
