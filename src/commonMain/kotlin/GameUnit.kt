
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
import com.soywiz.korge.view.addUpdater
import com.soywiz.korge.view.alpha
import com.soywiz.korge.view.anchor
import com.soywiz.korge.view.center
import com.soywiz.korge.view.circle
import com.soywiz.korge.view.collidesWithShape
import com.soywiz.korge.view.image
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
import kotlin.coroutines.CoroutineContext
import kotlin.random.Random

class GameUnit(
    private val ctx: CoroutineContext,
    name: String,
    initialRotation: Int,
    initialX: Int,
    initialY: Int,
    private val enemyTeam: Set<GameUnit>,
    bitmap: Bitmap
) : Container() {
    val body: View = image(bitmap).center()

    private var target: GameUnit? = null
    private var attackJob: Cancellable? = null
    var isAttacking = false
    private var currentPath: MutableList<GridPoint> = mutableListOf()
    private var currentPathGraphic: Image? = null
    private val attackRange = circle(cellSize.toDouble() / 2 + 5).anchor(0.25, 0.25).alpha(0.1)

    //    var gridPos = gridPos()
    val gridPos
        get() = gridPos()
    var movingToGridPos: GridPoint? = null

    init {
        this.name = name
        xy(initialX, initialY)
//        gridPos = gridPos()
        rotation = initialRotation.degrees
        body.speed = Random.nextDouble(0.75, 1.25)

        addUpdater {
            val scale = it / 16.milliseconds

            if (attackRange.collidesWithShape(enemyTeam.filter { it != target }.map { it.body })) {
                target = enemyTeam.filter { it != target }.find { attackRange.collidesWithShape(it.body) }
            }

            if (target == null) {
                refreshTargetAndPath()
                return@addUpdater
            }

            if (currentPath.isNowBlocked()) {
                refreshTargetAndPath()
            }

            if (attackRange.collidesWithShape(target!!.body)) {
                finishPreviousMovement()
                isAttacking = true
                currentPathGraphic?.removeFromParent()
                if (attackJob == null) {
                    launchImmediately(ctx) {
                        rotateTo(Angle.between(pos, target!!.pos), time = 150.milliseconds, easing = Easing.SMOOTH)
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
                    advance(0.5 * scale * speed)
                }
            }
        }
    }

    private fun atDestination(): Boolean {
        return movingToGridPos?.let { pos.distanceTo(it.worldX, it.worldY) <= 10 } ?: false
    }

    private fun acquireTarget() {
        target = enemyTeam.find { gridAdjacentTo(it) } ?: enemyTeam.minByOrNull { getPathTo(it).totalDistance() }
    }

    private fun refreshTargetAndPath() {
        val previousTarget = target
        acquireTarget()
        target?.let {
            if (previousTarget == it && !currentPath.isNowBlocked() && it.isAttacking || it.movingToGridPos?.gridAdjacentTo(gridPos) == true) {
                return@let
            }

            currentPath = getPathTo(it).toMutableList()
            currentPathGraphic?.removeFromParent()
            currentPathGraphic = parent?.image(NativeImage(640, 360).context2d {
                lineWidth = 0.05
                lineCap = LineCap.ROUND
                stroke(Colors.WHITE) {
                    moveTo(x, y)
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
            println("[${name} | ($gridPos)]: Moving to [$nextNode]")
            Grid.blockPos(nextNode)
            Grid.unblockPos(gridPos)
            launch(ctx) {
                rotateTo(Angle.between(gridPos, nextNode), time = 50.milliseconds, easing = Easing.SMOOTH)
            }
        }
    }

    private fun finishPreviousMovement() {
//        gridPos = Grid.findClosestToWorldPos(pos)
        Grid.blockPos(gridPos)
    }

    fun advance(amount: Double) {
        x += (rotation).cosine * amount
        y += (rotation).sine * amount
    }
}
