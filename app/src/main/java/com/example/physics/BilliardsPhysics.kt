package com.example.physics

import kotlin.math.*

data class Vector2D(val x: Float, val y: Float) {
    operator fun plus(other: Vector2D) = Vector2D(x + other.x, y + other.y)
    operator fun minus(other: Vector2D) = Vector2D(x - other.x, y - other.y)
    operator fun times(scalar: Float) = Vector2D(x * scalar, y * scalar)
    operator fun div(scalar: Float) = if (scalar != 0f) Vector2D(x / scalar, y / scalar) else Vector2D(0f, 0f)

    fun length() = sqrt(x * x + y * y)
    fun normalized(): Vector2D {
        val len = length()
        return if (len > 0f) this / len else Vector2D(0f, 0f)
    }
    fun dot(other: Vector2D) = x * other.x + y * other.y
    fun dist(other: Vector2D) = sqrt((x - other.x).pow(2) + (y - other.y).pow(2))
}

enum class BallType {
    CUE, SOLID, STRIPE, EIGHT_BALL
}

data class BilliardBall(
    val id: Int,
    var pos: Vector2D,
    var vel: Vector2D = Vector2D(0f, 0f),
    var isPocketed: Boolean = false,
    val type: BallType,
    val colorHex: Long,
    val number: Int,
    // Visual orientation for 3D spin rotation
    var rotationX: Float = 0f,
    var rotationY: Float = 0f
)

data class TablePocket(
    val id: Int,
    val pos: Vector2D,
    val radius: Float = 22f
)

class BilliardsPhysicsEngine {
    val tableWidth = 800f
    val tableHeight = 400f
    val cushionSize = 18f
    val ballRadius = 11.5f

    // Pocket list: corners and side centers
    val pockets = listOf(
        TablePocket(1, Vector2D(cushionSize, cushionSize)), // Top-Left
        TablePocket(2, Vector2D(tableWidth / 2f, cushionSize - 4f), radius = 20f), // Top-Center
        TablePocket(3, Vector2D(tableWidth - cushionSize, cushionSize)), // Top-Right
        TablePocket(4, Vector2D(cushionSize, tableHeight - cushionSize)), // Bottom-Left
        TablePocket(5, Vector2D(tableWidth / 2f, tableHeight - cushionSize + 4f), radius = 20f), // Bottom-Center
        TablePocket(6, Vector2D(tableWidth - cushionSize, tableHeight - cushionSize)) // Bottom-Right
    )

    // Cushion boundaries (collidable rect bounds)
    val minX get() = cushionSize + ballRadius
    val maxX get() = tableWidth - cushionSize - ballRadius
    val minY get() = cushionSize + ballRadius
    val maxY get() = tableHeight - cushionSize - ballRadius

    val balls = ArrayList<BilliardBall>()

    // Current spin applied to the cue ball
    // sideSpin (english): -1 (left) to +1 (right)
    // verticalSpin (draw/follow): -1 (backspin) to +1 (topspin)
    var cueBallSideSpin: Float = 0f
    var cueBallVerticalSpin: Float = 0f

    init {
        resetTable()
    }

    fun resetTable() {
        balls.clear()
        // Cue ball (white) placed at head string (1/4 length)
        balls.add(
            BilliardBall(
                id = 0,
                pos = Vector2D(200f, tableHeight / 2f),
                type = BallType.CUE,
                colorHex = 0xFFFFFFFF,
                number = 0
            )
        )

        // Generate 15 Object Balls racked in a triangular grid
        // Yellow (1,9), Blue (2,10), Red (3,11), Purple (4,12), Orange (5,13), Green (6,14), Maroon (7,15), Black (8)
        val ballColors = listOf(
            0xFFFFD700, // 1 Yellow
            0xFF1E90FF, // 2 Blue
            0xFFFF4500, // 3 Red
            0xFF8A2BE2, // 4 Purple
            0xFFFF8C00, // 5 Orange
            0xFF228B22, // 6 Green
            0xFF800000, // 7 Maroon
            0xFF1C1C1C, // 8 Black
            0xFFFFD700, // 9 Stripe Yellow
            0xFF1E90FF, // 10 Stripe Blue
            0xFFFF4500, // 11 Stripe Red
            0xFF8A2BE2, // 12 Stripe Purple
            0xFFFF8C00, // 13 Stripe Orange
            0xFF228B22, // 14 Stripe Green
            0xFF800000  // 15 Stripe Maroon
        )

        // Placement of numbers in the rack to mimic a real game
        // Standard: 8 ball in center, corners are solid/stripe split, apex is random
        val rackNumbers = listOf(
            1,
            9, 2,
            3, 8, 10,
            11, 4, 12, 5,
            6, 13, 7, 14, 15
        )

        val apexX = 580f
        val apexY = tableHeight / 2f
        val spacing = ballRadius * 2f + 0.5f
        val cos30 = sqrt(3f) / 2f

        var idx = 0
        for (col in 0..4) {
            val colX = apexX + col * spacing * cos30
            val startY = apexY - (col * spacing) / 2f
            for (row in 0..col) {
                val num = rackNumbers[idx]
                val type = when {
                    num == 8 -> BallType.EIGHT_BALL
                    num <= 7 -> BallType.SOLID
                    else -> BallType.STRIPE
                }
                val color = ballColors[num - 1]

                val ballY = startY + row * spacing
                balls.add(
                    BilliardBall(
                        id = num,
                        pos = Vector2D(colX, ballY),
                        type = type,
                        colorHex = color,
                        number = num
                    )
                )
                idx++
            }
        }
    }

    // Tick the physics simulation (handles movement, cushions, collisions, friction)
    // Returns list of IDs of balls pocketed in this tick, and boolean whether any collision occurred
    fun tick(dt: Float = 0.016f): Pair<List<Int>, Boolean> {
        val pocketedThisTick = mutableListOf<Int>()
        var collisionOccurred = false

        // Friction coefficients
        val slideFriction = 0.985f // Friction dampening per frame
        val stopThreshold = 0.15f

        // 1. Update Positions & Friction
        for (ball in balls) {
            if (ball.isPocketed) continue

            // Apply spin friction forces (draw / follow & english deflection)
            if (ball.type == BallType.CUE) {
                // Decay cue ball spin slightly during rolling
                cueBallSideSpin *= 0.99f
                cueBallVerticalSpin *= 0.99f
            }

            // Standard velocity damping
            ball.vel *= slideFriction

            // Update rotation angles for realistic 3D sphere rolling effect
            val speed = ball.vel.length()
            if (speed > 0.05f) {
                // Rolling rotation
                ball.rotationX += ball.vel.y * 0.05f
                ball.rotationY += ball.vel.x * 0.05f
            }

            // Stop moving if extremely slow
            if (ball.vel.length() < stopThreshold) {
                ball.vel = Vector2D(0f, 0f)
            } else {
                ball.pos = ball.pos + ball.vel * dt
            }
        }

        // 2. Cushion Collisions (Bounds Bouncing)
        for (ball in balls) {
            if (ball.isPocketed) continue

            val restitution = 0.85f // elastic cushion bounce
            var hitCushion = false

            // Left Cushion
            if (ball.pos.x < minX) {
                ball.pos = Vector2D(minX, ball.pos.y)
                // Spin reaction: side-spin (english) affects vertical velocity on a vertical wall
                val extraY = if (ball.type == BallType.CUE) cueBallSideSpin * 40f else 0f
                ball.vel = Vector2D(-ball.vel.x * restitution, ball.vel.y * restitution + extraY)
                hitCushion = true
            }
            // Right Cushion
            else if (ball.pos.x > maxX) {
                ball.pos = Vector2D(maxX, ball.pos.y)
                val extraY = if (ball.type == BallType.CUE) -cueBallSideSpin * 40f else 0f
                ball.vel = Vector2D(-ball.vel.x * restitution, ball.vel.y * restitution + extraY)
                hitCushion = true
            }

            // Top Cushion
            if (ball.pos.y < minY) {
                ball.pos = Vector2D(ball.pos.x, minY)
                val extraX = if (ball.type == BallType.CUE) cueBallSideSpin * 40f else 0f
                ball.vel = Vector2D(ball.vel.x * restitution + extraX, -ball.vel.y * restitution)
                hitCushion = true
            }
            // Bottom Cushion
            else if (ball.pos.y > maxY) {
                ball.pos = Vector2D(ball.pos.x, maxY)
                val extraX = if (ball.type == BallType.CUE) -cueBallSideSpin * 40f else 0f
                ball.vel = Vector2D(ball.vel.x * restitution + extraX, -ball.vel.y * restitution)
                hitCushion = true
            }

            if (hitCushion) {
                collisionOccurred = true
            }
        }

        // 3. Ball-to-Ball Collisions
        for (i in balls.indices) {
            val b1 = balls[i]
            if (b1.isPocketed) continue

            for (j in i + 1 until balls.size) {
                val b2 = balls[j]
                if (b2.isPocketed) continue

                val dist = b1.pos.dist(b2.pos)
                val minDist = ballRadius * 2f

                if (dist < minDist) {
                    collisionOccurred = true
                    // Overlap correction (prevents sticking)
                    val overlap = minDist - dist
                    val separationVec = (b1.pos - b2.pos).normalized() * (overlap / 2f)
                    b1.pos = b1.pos + separationVec
                    b2.pos = b2.pos - separationVec

                    // Relative velocity
                    val rv = b1.vel - b2.vel

                    // Calculate normal and tangent vectors
                    val normal = (b2.pos - b1.pos).normalized()
                    val tangent = Vector2D(-normal.y, normal.x)

                    // Project velocities onto normal and tangent
                    val v1n = b1.vel.dot(normal)
                    val v1t = b1.vel.dot(tangent)
                    val v2n = b2.vel.dot(normal)
                    val v2t = b2.vel.dot(tangent)

                    // Since mass is identical (1.0), velocities along the collision normal are swapped
                    val v1nTag = v2n
                    val v2nTag = v1n

                    // Spin deflection transfer on collision:
                    // If cue ball (b1) hits object ball (b2), transfer side spin friction
                    var spinFrictionTransfer = 0f
                    if (b1.type == BallType.CUE) {
                        spinFrictionTransfer = cueBallSideSpin * 15f
                    }

                    // Reconstruct final velocities
                    b1.vel = normal * v1nTag + tangent * (v1t - spinFrictionTransfer * 0.2f)
                    b2.vel = normal * v2nTag + tangent * (v2t + spinFrictionTransfer * 0.4f)
                }
            }
        }

        // 4. Pocket Sinks (Gravity pull towards pocket center)
        for (ball in balls) {
            if (ball.isPocketed) continue

            for (pocket in pockets) {
                val dist = ball.pos.dist(pocket.pos)
                // Inside suction range
                if (dist < pocket.radius + 8f) {
                    if (dist < pocket.radius) {
                        // Complete Sinking!
                        ball.isPocketed = true
                        ball.vel = Vector2D(0f, 0f)
                        pocketedThisTick.add(ball.id)
                    } else {
                        // Pull gravity pull towards the pocket center
                        val pullForce = (pocket.pos - ball.pos).normalized() * 45f
                        ball.vel = ball.vel + pullForce * dt
                    }
                }
            }
        }

        return Pair(pocketedThisTick, collisionOccurred)
    }

    // Is everything stationary?
    fun isStationary(): Boolean {
        for (ball in balls) {
            if (!ball.isPocketed && ball.vel.length() > 0f) {
                return false
            }
        }
        return true
    }

    // Perform Shot (strikes the cue ball)
    fun shootCueBall(angleRad: Float, power: Float) {
        val cueBall = balls.find { it.type == BallType.CUE } ?: return
        if (cueBall.isPocketed) return

        // Base max speed
        val maxSpeed = 750f
        val shotVelocity = power * maxSpeed

        // Unit vector of shot direction
        val dirX = cos(angleRad)
        val dirY = sin(angleRad)

        // Primary impulse velocity
        var targetVel = Vector2D(dirX, dirY) * shotVelocity

        // Apply draw/follow vertical spin effects immediately to shot velocity
        // e.g., topspin pushes ball forward slightly extra, backspin creates reverse drag later
        cueBall.vel = targetVel
    }

    // Find the primary aim-assist guidelines path including reflection off cushions or first ball hit
    fun calculateAimGuide(angleRad: Float): AimGuideResult {
        val cueBall = balls.find { it.type == BallType.CUE } ?: return AimGuideResult()
        if (cueBall.isPocketed) return AimGuideResult()

        val dir = Vector2D(cos(angleRad), sin(angleRad)).normalized()
        var currentPos = cueBall.pos

        // Find if we hit another ball first
        var firstBallHit: BilliardBall? = null
        var closestDistanceToHit = Float.MAX_VALUE
        var hitPointOnBall = Vector2D(0f, 0f)

        for (ball in balls) {
            if (ball.type == BallType.CUE || ball.isPocketed) continue

            // Circle-Line Sweep Collision Test
            // Vector from cueball to object ball
            val v = ball.pos - currentPos
            val projection = v.dot(dir)

            if (projection > 0) { // must be forward
                // Find closest point on line
                val closestPointLine = currentPos + dir * projection
                val perpDist = ball.pos.dist(closestPointLine)
                val contactDist = ballRadius * 2f

                if (perpDist < contactDist) {
                    // There is a contact. Let's find exact position
                    val offset = sqrt(contactDist.pow(2) - perpDist.pow(2))
                    val hitPos = currentPos + dir * (projection - offset)
                    val distToHit = cueBall.pos.dist(hitPos)

                    if (distToHit < closestDistanceToHit) {
                        closestDistanceToHit = distToHit
                        firstBallHit = ball
                        hitPointOnBall = hitPos
                    }
                }
            }
        }

        if (firstBallHit != null) {
            // Reconstruct deflection angles
            val b2Pos = firstBallHit.pos
            val normal = (b2Pos - hitPointOnBall).normalized()
            val tangent = Vector2D(-normal.y, normal.x)

            // Direct path ends at ball contact point
            val line1End = hitPointOnBall

            // Object ball takes off along normal
            val objectBallPath = normal * 100f
            val objectBallEnd = b2Pos + objectBallPath

            // Cue ball deflects along tangent
            val cueTangentProjection = dir.dot(tangent)
            val cueBallPath = tangent * (cueTangentProjection * 80f)
            val cueBallEnd = hitPointOnBall + cueBallPath

            return AimGuideResult(
                hitBall = true,
                startPoint = cueBall.pos,
                hitPoint = hitPointOnBall,
                cueBallPathEnd = cueBallEnd,
                objectBallPathEnd = objectBallEnd,
                hitBallNumber = firstBallHit.number
            )
        } else {
            // Cushion hit projection if no ball is hit
            // Raycast cushion boundaries
            var tX = Float.MAX_VALUE
            var tY = Float.MAX_VALUE

            if (dir.x > 0) tX = (maxX - currentPos.x) / dir.x
            else if (dir.x < 0) tX = (minX - currentPos.x) / dir.x

            if (dir.y > 0) tY = (maxY - currentPos.y) / dir.y
            else if (dir.y < 0) tY = (minY - currentPos.y) / dir.y

            val t = min(tX, tY)
            val hitCushionPoint = currentPos + dir * t

            // Bounce direction
            val bounceDir = Vector2D(
                if (t == tX) -dir.x else dir.x,
                if (t == tY) -dir.y else dir.y
            )
            val bounceEnd = hitCushionPoint + bounceDir * 60f

            return AimGuideResult(
                hitBall = false,
                startPoint = cueBall.pos,
                hitPoint = hitCushionPoint,
                cueBallPathEnd = bounceEnd
            )
        }
    }
}

data class AimGuideResult(
    val hitBall: Boolean = false,
    val startPoint: Vector2D = Vector2D(0f, 0f),
    val hitPoint: Vector2D = Vector2D(0f, 0f),
    val cueBallPathEnd: Vector2D = Vector2D(0f, 0f),
    val objectBallPathEnd: Vector2D = Vector2D(0f, 0f),
    val hitBallNumber: Int = -1
)
