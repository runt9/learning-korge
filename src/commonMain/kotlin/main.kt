import Grid.addRandomlyToGrid
import com.soywiz.korev.Key
import com.soywiz.korge.Korge
import com.soywiz.korge.input.keys
import com.soywiz.korge.view.addTo
import com.soywiz.korge.view.alpha
import com.soywiz.korge.view.graphics
import com.soywiz.korge.view.roundRect
import com.soywiz.korge.view.xy
import com.soywiz.korim.color.Colors
import com.soywiz.korim.format.readBitmap
import com.soywiz.korio.file.std.resourcesVfs

suspend fun main() = Korge(width = 1280, height = 720, bgcolor = Colors["#2b2b2b"], virtualWidth = virtualWidth, virtualHeight = virtualHeight) {
	graphics {
		(0..gridWidth).forEach { x ->
			(0..gridHeight).forEach { y ->
				roundRect((cellSize - 10).toDouble(), (cellSize - 10).toDouble(), 5.0, fill = Colors["#cccccc"]).xy(cellSize * x + 5, cellSize * y + 5)
					.alpha(0.25)
			}
		}
	}

	val redTeam = resourcesVfs["redArrow-tp.png"].readBitmap()
	val blueTeam = resourcesVfs["blueArrow-tp.png"].readBitmap()

	repeat(8) {
		val gridEntry = addRandomlyToGrid(0, 5)
		UnitRegistry.team1.add(
			GameUnit(coroutineContext, "left$it", 0, gridEntry.worldX.toInt(), gridEntry.worldY.toInt(), UnitRegistry.team2, redTeam).addTo(
				this
			)
		)
	}

	repeat(8) {
		val gridEntry = addRandomlyToGrid(gridWidth - 6, gridWidth - 1)
		UnitRegistry.team2.add(
			GameUnit(
				coroutineContext,
				"right$it",
				180,
				gridEntry.worldX.toInt(),
				gridEntry.worldY.toInt(),
				UnitRegistry.team1,
				blueTeam
			).addTo(this)
		)
	}

	var previousSpeed = speed
	keys {
		down(Key.SPACE) {
			if (speed == 0.0) {
				speed = previousSpeed
			} else {
				previousSpeed = speed
				speed = 0.0
			}
		}

		down(Key.DOWN) {
			speed /= 2
		}

		down(Key.UP) {
			speed *= 2
		}
	}
}
