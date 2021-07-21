
import Grid.diagonalTo
import com.soywiz.klock.milliseconds
import com.soywiz.klock.timesPerSecond
import com.soywiz.korge.particle.ParticleEmitter
import com.soywiz.korge.particle.ParticleEmitterView
import com.soywiz.korge.particle.particleEmitter
import com.soywiz.korge.ui.UIProgressBar
import com.soywiz.korge.ui.buttonBackColor
import com.soywiz.korge.ui.uiProgressBar
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.View
import com.soywiz.korge.view.addFixedUpdater
import com.soywiz.korge.view.center
import com.soywiz.korge.view.image
import com.soywiz.korge.view.rotation
import com.soywiz.korge.view.tween.moveBy
import com.soywiz.korge.view.tween.moveTo
import com.soywiz.korge.view.xy
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.color.RGBA
import com.soywiz.korio.async.launchImmediately
import com.soywiz.korio.lang.Cancellable
import com.soywiz.korio.lang.cancel
import com.soywiz.korma.geom.cosine
import com.soywiz.korma.geom.degrees
import com.soywiz.korma.geom.sine
import kotlin.coroutines.CoroutineContext
import kotlin.random.Random

class GameUnit(
    private val ctx: CoroutineContext,
    name: String,
    initialRotation: Int,
    initialX: Int,
    initialY: Int,
    val enemyTeam: Set<GameUnit>,
    bitmap: Bitmap,
    val hitParticles: ParticleEmitter
) : Container() {
    val body: View

    var target: GameUnit? = null
    private var attackJob: Cancellable? = null
    var isAttacking = false
    var aggroRangeFlat = 2
    var gridPos: GridPoint
    var previousGridPos: GridPoint? = null
    var movingToGridPos: GridPoint? = null
    var hp = Random.nextInt(100, 200)
    var damage = Random.nextInt(20, 40)
    var currentHitParticles: ParticleEmitterView? = null
    var healthBar: UIProgressBar

    init {
        this.name = name
        xy(initialX, initialY)
        gridPos = GridPoint.fromWorldPoint(pos)

        body = image(bitmap).center().rotation(initialRotation.degrees)

        healthBar = uiProgressBar(cellSize.toDouble() * 0.75, 2.0, current = hp.toDouble(), maximum = hp.toDouble()) {
            // TODO: Not hard-coded
            xy(-15.0, -20.0)
            buttonBackColor = RGBA(0, 0, 0, 100)
//            buttonNormal = Colors.GREEN
        }
    }

    fun startAttacking() {
        if (attackJob != null) {
            return
        }

        if (diagonalTo(target!!)) {
            launchImmediately(ctx) {
                moveBy(body.rotation.cosine * 3, body.rotation.sine * 3, time = 150.milliseconds)
            }
        }

        previousGridPos = null

        attackJob = addFixedUpdater(0.5.timesPerSecond) {
            launchImmediately(ctx) {
                val currentX = pos.x
                val currentY = pos.y

                moveBy(body.rotation.cosine * 3, body.rotation.sine * 3, time = 25.milliseconds)
                moveTo(currentX, currentY, time = 150.milliseconds)

                target?.let { t ->
                    t.takeDamage(damage)

                    if (t.hp <= 0) {
                        UnitManager.unitKilled(t)
                        target = null
                        cancelAttacking()
                    }
                } ?: cancelAttacking()
            }
        }
    }

    suspend fun takeDamage(damage: Int) {
        hp -= damage
        healthBar.current = hp.toDouble()
        currentHitParticles?.removeFromParent()
        currentHitParticles = particleEmitter(hitParticles, time = 50.milliseconds)
    }

    fun cancelAttacking() {
        attackJob?.cancel()
        attackJob = null
    }
}
