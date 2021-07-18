//private fun refreshPath() {
//    val notMe = UnitRegistry.all.filter { u -> u != this@GameUnit && u != target }.map { u -> u.image }.toList()
//    val newPath = AStar(640, 360) { x, y ->
//        val xDiff = target!!.image.x - image.x
//        if (xDiff < 0) {
//            if (x < target!!.image.x) {
//                return@AStar false
//            }
//        } else {
//            if (x > target!!.image.x) {
//                return@AStar false
//            }
//        }
//
//        projectionRect.xy(x, y).collidesWith(notMe)
//    }
//        .find(image.x.toInt(), image.y.toInt(), target!!.image.x.toInt(), target!!.image.y.toInt())
//    val points = newPath.toPoints().toMutableList()
//    points.removeFirstOrNull()
//    currentPath = points
//    currentPathGraphic?.removeFromParent()
//    currentPathGraphic = parent.image(NativeImage(640, 360).context2d {
//        lineWidth = 0.05
//        lineCap = LineCap.ROUND
//        stroke(Colors.WHITE) {
//            moveTo(image.x, image.y)
//            points.forEach {
//                lineTo(it.x, it.y)
//            }
//        }
//    })
//}

//private suspend fun advanceTowardsTarget(scale: Double) {
//    val collisionAvoiding = UnitRegistry.all.filter { u -> u != this@GameUnit && u != target }.map { u -> u.image }.toList()
//    val rotationTowardsTarget = Angle.between(image.pos, target!!.image.pos)
//    var finalRotation = rotationTowardsTarget
//    projectionRect.rotation(image.rotation)
//
//    projectionRect.xy(image.x, image.y).rotation(finalRotation).advance(2 * scale)
////        if (projectionRect.collidesWith(collisionAvoiding)) {
////            projectionRect.xy(image.x, image.y).rotation(image.rotation).advance(3 * scale)
////        }
//
//    var degreesSpun = 0
//    while (projectionRect.collidesWith(collisionAvoiding)) {
//        finalRotation += 1.degrees
//
//        if (finalRotation > 360.degrees) {
//            finalRotation -= 360.degrees
//        }
//
//        degreesSpun += 1
//
//        if (degreesSpun >= 360) {
//            throw Exception("Failed to find safe rotation")
//        }
//        projectionRect.xy(image.x, image.y).rotation(finalRotation).advance(2 * scale)
//    }
//
//    image.rotateTo(finalRotation, time = 50.milliseconds)
//    image.advance(0.25 * scale)
//}

