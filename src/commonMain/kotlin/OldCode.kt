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

//        var shortestPath = listOf<GridPoint>()
//        var shortestDistance = Double.MAX_VALUE
//
//        getNextPossibleStepsToward(other, emptyList()).forEach { nextPossibleStep ->
//            if (nextPossibleStep == other) {
//                return@getPathTo emptyList()
//            }
//            val path = mutableListOf<GridPoint>()
//            var nextPoint: GridPoint? = nextPossibleStep
//            while (nextPoint != null && !path.contains(nextPoint) && path.totalDistance() < shortestDistance) {
//                path += nextPoint
//                nextPoint = nextPoint.getNextStepTowards(other, path)
//
//                if (nextPoint == other) {
//                    path.add(nextPoint)
//                    break
//                }
//            }
//
//            if (!path.contains(other)) {
//                return@forEach
//            }
//
//            val pathTotal = path.totalDistance()
//            if (pathTotal < shortestDistance) {
////                path.removeAll { it == other }
//                shortestPath = path
//                shortestDistance = pathTotal
//            }
//        }
//
//        return shortestPath

//            if (target == null || (moveCommand != null && !moveCommand!!.isCompleted)) {
//                return@addUpdater
//            }
//
//            if (gridAdjacentTo(target!!)) {
//                isAttacking = true
//                if (attackJob == null) {
//                    launchImmediately(ctx) {
//                        image.rotateTo(Angle.between(gridPos, target!!.gridPos), time = 150.milliseconds, easing = Easing.SMOOTH)
//                        attackJob = addFixedUpdater(1.timesPerSecond) {
////                            println("[$name]: attack")
//                        }
//                    }
//                }
//            } else {
//                isAttacking = false
//                attackJob?.cancel()
//                val path = getPathTo(target!!)
//                if (path.isEmpty()) {
//                    return@addUpdater
//                }
//                val nextNode = path[0]
////                val nextNode = gridPos.getNextStepTowards(target!!.gridPos)!!
//                if (prepareToMove(nextNode)) {
//                    moveCommand = launchAsap(ctx) {
//                        image.rotateTo(Angle.between(gridPos, nextNode), time = 150.milliseconds, easing = Easing.SMOOTH)
//                        gridPos = nextNode
//                        image.moveTo(nextNode.worldX, nextNode.worldY, time = 1.seconds, easing = Easing.SMOOTH)
//                    }
//                }
//            }

//movingToGridPos?.let { nextNode ->
//    Grid.blockPos(nextNode)
//    launchAsap(ctx) {
//        image.rotateTo(Angle.between(gridPos, nextNode), time = 150.milliseconds, easing = Easing.SMOOTH)
//
//        println("[$name | ($gridPos)]: Moving to [$nextNode]")
//        image.tween(this::x[nextNode.worldX], this::y[nextNode.worldY], time = 1.seconds, easing = Easing.LINEAR) {
//            if (it != 1.0) {
//                return@tween
//            }
//
//            println("[$name | ($gridPos)]: COMPLETE: Moving to [$nextNode]")
//            Grid.unblockPos(gridPos)
//            gridPos = nextNode
//            if (currentPath.isNotEmpty()) {
//                movingToGridPos = currentPath.removeFirst()
//                println("[$name | ($gridPos)]: Next movement to: [$movingToGridPos]")
//            }
//        }
//    }
//}

