
import com.soywiz.klock.milliseconds
import com.soywiz.klock.timesPerSecond
import com.soywiz.korge.view.Circle
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
import com.soywiz.korge.view.hitShape
import com.soywiz.korge.view.image
import com.soywiz.korge.view.tween.rotateBy
import com.soywiz.korge.view.tween.rotateTo
import com.soywiz.korge.view.xy
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korim.bitmap.context2d
import com.soywiz.korim.color.Colors
import com.soywiz.korio.async.launchImmediately
import com.soywiz.korio.lang.Cancellable
import com.soywiz.korio.lang.cancel
import com.soywiz.korma.algo.AStar
import com.soywiz.korma.annotations.KormaExperimental
import com.soywiz.korma.geom.Angle
import com.soywiz.korma.geom.PointInt
import com.soywiz.korma.geom.cosine
import com.soywiz.korma.geom.degrees
import com.soywiz.korma.geom.sine
import com.soywiz.korma.geom.toPoints
import com.soywiz.korma.geom.vector.LineCap
import com.soywiz.korma.geom.vector.circle
import com.soywiz.korma.geom.vector.lineTo
import com.soywiz.korma.interpolation.Easing
import kotlin.coroutines.CoroutineContext

@KormaExperimental
class GameUnit(
    private val ctx: CoroutineContext,
    name: String,
    initialRotation: Int,
    initialX: Int,
    initialY: Int,
    private val enemyTeam: Set<GameUnit>,
    bitmap: Bitmap
) : Container() {
    val body: View

    private var target: GameUnit? = null
    private var attackJob: Cancellable? = null
    var isAttacking = false
    private val attackRange = circle(cellSize.toDouble() / 2 + 5, fill = Colors.RED).anchor(0.25, 0.25).alpha(0.0)
    private val aggroRange = circle(cellSize.toDouble()).anchor(0.25, 0.25).alpha(0.0)
    private val viewLine: View
    private var pathToTarget = mutableListOf<PointInt>()
    private var currentPathGraphic: Image? = null

    init {
        this.name = name
        xy(initialX, initialY)
        rotation = initialRotation.degrees

        viewLine = image(NativeImage(cellSize, 2).context2d {
            lineWidth = 0.05
            lineCap = LineCap.ROUND
            stroke(Colors.WHITE) {
                moveTo(0.0, 0.5)
                lineTo(cellSize, 0)
            }
            rotation = initialRotation.degrees
            hitShape {
                moveTo(0.0, 0.5)
                lineTo(cellSize, 0)
            }
        }).alpha(1)

        body = image(bitmap).center()
        hitShape {
            circle(0, 0, cellSize / 2 - 5)
        }

        addUpdater {
            val scale = it / 16.milliseconds

            if (target != null) {
                if (attackRange.collidesWithShape(target!!)) {
                    isAttacking = true
                    if (attackJob == null) {
                        launchImmediately(ctx) {
                            rotateTo(Angle.between(pos, target!!.pos), time = 150.milliseconds, easing = Easing.SMOOTH)
                            attackJob = addFixedUpdater(1.timesPerSecond) {
//                            println("[$name]: attack")
                            }
                        }
                    }
                } else if (attackRange.collidesWithShape(enemyTeam.filter { it != target }.map { it })) {
                    target = enemyTeam.filter { it != target }.find { attackRange.collidesWithShape(it) }
//                } else if (pathToTarget.isNotEmpty()) {
//
                } else {
                    if (viewingAtCollision(false)) {
//                        val timeResult = measureTime { findClosestTargetByPath() }
//                        println("[$name]: Found path in $timeResult")
                        launchImmediately(ctx) {
                            rotateBy(15.degrees, 50.milliseconds)
                        }
                    } else {
                        if (canSafelyRotateTowardsTarget()) {
                            launchImmediately(ctx) {
                                rotateTo(Angle.between(pos, target!!.pos), time = 150.milliseconds, easing = Easing.SMOOTH)
                            }
                        }

                        advance(0.5 * scale * speed)
                    }
                }
            } else {
                when {
                    attackRange.collidesWithShape(enemyTeam.filter { it != target }.map { it }) -> {
                        target = enemyTeam.filter { it != target }.find { attackRange.collidesWithShape(it) }
                    }
                    aggroRange.collidesWithShape(enemyTeam.filter { it != target }) -> {
                        target = enemyTeam.filter { it != target }.find { aggroRange.collidesWithShape(it) }
                        aggroRange.radius = cellSize.toDouble()
                    }
                    else -> {
                        aggroRange.radius += scale * 0.5 * speed
                    }
                }

                isAttacking = false
                attackJob?.cancel()

                if (viewingAtCollision(true)) {
                    launchImmediately(ctx) {
                        rotateBy(15.degrees, 50.milliseconds, easing = Easing.SMOOTH)
                    }
                    return@addUpdater
                }

                advance(0.5 * scale * speed)
            }
        }
    }

    private fun canSafelyRotateTowardsTarget(): Boolean {
        val tmpLine = image(NativeImage(cellSize, 2).context2d {
            lineWidth = 0.05
            lineCap = LineCap.ROUND
            stroke(Colors.WHITE) {
                moveTo(0.0, 0.5)
                lineTo(cellSize, 0)
            }
            this.rotate(Angle.between(pos, target!!.pos))
            hitShape {
                moveTo(0.0, 0.5)
                lineTo(cellSize, 0)
            }
        }).alpha(0.5)

        val canRotate = target?.let { t -> tmpLine.collidesWithShape(t) } == false
        tmpLine.removeFromParent()
        return canRotate
    }

    fun pathTo(other: GameUnit): MutableList<PointInt> {
        val projectionCircle = Circle((cellSize / 2 - 5).toDouble())
        val unitCollisions = UnitRegistry.all.filter { gu -> gu != this@GameUnit }

        val newPath = AStar(virtualWidth / cellSize, virtualHeight / cellSize) { pathX, pathY ->
            projectionCircle.xy(pathX * cellSize, pathY * cellSize).collidesWithShape(unitCollisions)
        }.find(x.toInt() / cellSize, y.toInt() / cellSize, other.x.toInt() / cellSize, other.y.toInt() / cellSize)

        return newPath.toPoints().toMutableList()
    }

    fun advance(amount: Double) {
        x += (rotation).cosine * amount
        y += (rotation).sine * amount
    }

    fun viewingAtCollision(attackingCheck: Boolean) =
        UnitRegistry.all.filter { gu -> gu.enemyTeam == enemyTeam && gu != this@GameUnit && (!attackingCheck || gu.isAttacking) }
            .any { gu -> viewLine.collidesWithShape(gu) }

    fun findClosestTargetByPath() {
        var shortestPath = mutableListOf<PointInt>()
        var shortestDistance = Double.MAX_VALUE

        enemyTeam.forEach {
            val path = pathTo(it)
            val pathDistance = path.totalDistance()
            if (pathDistance < shortestDistance) {
                shortestPath = path
                shortestDistance = pathDistance
            }
        }

        shortestPath.removeFirstOrNull()
        pathToTarget = shortestPath
        currentPathGraphic?.removeFromParent()
        currentPathGraphic = parent?.image(NativeImage(640, 360).context2d {
            lineWidth = 0.05
            lineCap = LineCap.ROUND
            stroke(Colors.WHITE) {
                moveTo(x, y)
                shortestPath.forEach {
                    lineTo(it.x * cellSize, it.y * cellSize)
                }
            }
        })

    }

    fun List<PointInt>.totalDistance(): Double {
        var distance = 0.0
        forEachIndexed { index, point ->
            if (index != size - 1) {
                distance += point.p.distanceTo(elementAt(index + 1).p)
            }
        }
        return distance
    }

}
