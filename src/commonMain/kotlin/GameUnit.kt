
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.Image
import com.soywiz.korge.view.View
import com.soywiz.korge.view.alpha
import com.soywiz.korge.view.anchor
import com.soywiz.korge.view.center
import com.soywiz.korge.view.circle
import com.soywiz.korge.view.hitShape
import com.soywiz.korge.view.image
import com.soywiz.korge.view.xy
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korim.bitmap.context2d
import com.soywiz.korim.color.Colors
import com.soywiz.korio.lang.Cancellable
import com.soywiz.korma.geom.PointInt
import com.soywiz.korma.geom.cosine
import com.soywiz.korma.geom.degrees
import com.soywiz.korma.geom.sine
import com.soywiz.korma.geom.vector.LineCap
import com.soywiz.korma.geom.vector.circle
import com.soywiz.korma.geom.vector.lineTo
import kotlin.coroutines.CoroutineContext

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
    var aggroRangeFlat = 2
    private val viewLine: View
    private var pathToTarget = mutableListOf<PointInt>()
    var gridPointPath = mutableListOf<GridPoint>()
    private var currentPathGraphic: Image? = null
    var gridPos: GridPoint
    var movingToGridPos: GridPoint? = null
    val attackRangeAmt = cellSize / 2 + 5


    init {
        this.name = name
        xy(initialX, initialY)
        gridPos = GridPoint.fromWorldPoint(pos)
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

    }


    fun advance(amount: Double) {
        x += (rotation).cosine * amount
        y += (rotation).sine * amount
    }

}
