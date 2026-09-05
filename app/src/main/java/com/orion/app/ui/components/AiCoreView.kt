package com.orion.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import com.orion.app.core.CoreState
import com.orion.app.ui.theme.AlertRed
import com.orion.app.ui.theme.CoreBlue
import com.orion.app.ui.theme.CoreBlueDim
import com.orion.app.ui.theme.CoreCyan
import com.orion.app.ui.theme.InkPrimary
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * The real Orion / Avcı Takımyıldızı asterism, expressed as fractions of the
 * drawing box (0..1) so it scales with the canvas but keeps its true relative
 * shape: shoulders (Betelgeuse, Bellatrix), the three belt stars (Mintaka,
 * Alnilam, Alnitak), and the feet (Rigel, Saiph). This is NOT a random
 * scatter — the positions and connecting lines mirror the actual asterism.
 */
private data class StarPoint(val x: Float, val y: Float, val radius: Float, val label: String)

private val orionStars = listOf(
    StarPoint(0.30f, 0.14f, 3.2f, "Bellatrix"),
    StarPoint(0.74f, 0.20f, 4.2f, "Betelgeuse"),
    StarPoint(0.38f, 0.50f, 2.6f, "Mintaka"),
    StarPoint(0.50f, 0.53f, 2.4f, "Alnilam"),
    StarPoint(0.62f, 0.50f, 2.6f, "Alnitak"),
    StarPoint(0.20f, 0.86f, 3.6f, "Rigel"),
    StarPoint(0.80f, 0.90f, 3.0f, "Saiph")
)

private val orionLines = listOf(
    0 to 2, 1 to 4,               // shoulders down to belt ends
    2 to 3, 3 to 4,               // the belt itself
    2 to 5, 4 to 6                // belt down to the feet
)

@Composable
fun AiCoreView(
    state: CoreState,
    modifier: Modifier = Modifier,
    isCharging: Boolean = false,
    isAwake: Boolean = true
) {
    val infinite = rememberInfiniteTransition(label = "core-infinite")

    // Base breathing — always running, every other state layers on top of it.
    val breathing by infinite.animateFloat(
        initialValue = 0.92f, targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Reverse),
        label = "breathing"
    )

    // Orbit speed: idle is slow, THINKING speeds it up noticeably.
    val orbitSpeed = when (state) {
        CoreState.THINKING -> 5200
        CoreState.EXECUTING -> 6000
        else -> 14000
    }
    val orbitAngle by infinite.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(orbitSpeed, easing = LinearEasing)),
        label = "orbit"
    )

    // SEARCHING: a radar-style sweep ring.
    val sweepAngle by infinite.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing)),
        label = "sweep"
    )

    // LISTENING: concentric rings traveling INWARD (progress 1 -> 0 loops).
    val inwardProgress by infinite.animateFloat(
        initialValue = 1f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing)),
        label = "inward"
    )

    // EXECUTING: energy lines traveling OUTWARD (progress 0 -> 1 loops).
    val outwardProgress by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
        label = "outward"
    )

    // Whole constellation slowly rotates as one rigid shape — the asterism itself
    // never distorts, only its orientation around the core changes. Speeds up
    // slightly while the core is actively "busy" (THINKING/EXECUTING), same idea
    // as the particle orbit above.
    val constellationSpeed = when (state) {
        CoreState.THINKING, CoreState.EXECUTING -> 32000
        else -> 90000
    }
    val constellationAngle by infinite.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(constellationSpeed, easing = LinearEasing)),
        label = "constellation-rotation"
    )

    // MANDATORY answering effect: while EXECUTING, lines grow from every star
    // straight into the core, and three stars (the belt) flare brighter — then
    // both fade back out the moment EXECUTING ends (COMPLETE / IDLE).
    val coreLinkProgress by animateFloatAsState(
        targetValue = if (state == CoreState.EXECUTING) 1f else 0f,
        animationSpec = tween(650, easing = LinearEasing),
        label = "core-link"
    )

    // COMPLETE: a single decaying pulse rendered via animateFloatAsState (not infinite).
    val completePulse by animateFloatAsState(
        targetValue = if (state == CoreState.COMPLETE) 1f else 0f,
        animationSpec = tween(900),
        label = "complete-pulse"
    )

    // ALERT: fast red/dark blink — used to recolor the ring + nucleus while a
    // disaster notice is active. Deliberately jarring; this should be hard to miss.
    val alertBlink by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(500, easing = LinearEasing), RepeatMode.Reverse),
        label = "alert-blink"
    )
    val coreColor = if (state == CoreState.ALERT) {
        lerp(Color(0xFF3A0000), AlertRed, alertBlink)
    } else CoreBlue

    // Charging: green "electricity" arcs orbiting the core, only while plugged in.
    val chargeBoltPhase by infinite.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing)),
        label = "charge-bolt-phase"
    )
    val chargeFlicker by infinite.animateFloat(
        initialValue = 0.35f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(220, easing = LinearEasing), RepeatMode.Reverse),
        label = "charge-flicker"
    )

    // Sleeping: whole core dims to a faint standby glow instead of running every
    // animation at full brightness — cheaper and clearer than threading a dim
    // factor through every single drawCircle call above.
    val awakeAlpha by animateFloatAsState(
        targetValue = if (isAwake) 1f else 0.22f,
        animationSpec = tween(500),
        label = "awake-alpha"
    )

    Box(modifier = modifier.aspectRatio(1.05f), contentAlignment = androidx.compose.ui.Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize().alpha(awakeAlpha)) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f
            val boxSize = min(w, h)
            val nucleusRadius = boxSize * 0.11f * breathing

            // Pseudo-3D: everything orbit-shaped is squashed vertically and tilted,
            // as if seen at a slight angle rather than face-on. No real 3D geometry —
            // just perspective cues (squash, depth-sort, offset shading).
            val tiltY = 0.42f

            // --- outer structural rings, drawn as tilted ellipses (like a disc, not a flat ring) ---
            scale(scaleX = 1f, scaleY = tiltY, pivot = Offset(cx, cy)) {
                drawCircle(
                    color = CoreBlueDim.copy(alpha = 0.20f),
                    radius = boxSize * 0.46f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.2f)
                )
            }
            rotate(degrees = orbitAngle * 0.4f, pivot = Offset(cx, cy)) {
                scale(scaleX = 1f, scaleY = tiltY, pivot = Offset(cx, cy)) {
                    drawCircle(
                        color = coreColor.copy(alpha = 0.28f),
                        radius = boxSize * 0.37f,
                        center = Offset(cx, cy),
                        style = Stroke(width = 1f, cap = StrokeCap.Round)
                    )
                }
            }

            // --- ALERT: an extra hard flashing ring, impossible to miss ---
            if (state == CoreState.ALERT) {
                drawCircle(
                    color = AlertRed.copy(alpha = 0.25f + alertBlink * 0.55f),
                    radius = boxSize * 0.44f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 3f)
                )
            }

            // --- CHARGING: jagged green energy bolts orbiting the core ---
            if (isCharging) {
                val boltCount = 5
                for (i in 0 until boltCount) {
                    val angle = Math.toRadians((chargeBoltPhase + 360f / boltCount * i).toDouble())
                    val innerR = boxSize * 0.30f
                    val outerR = boxSize * 0.44f
                    val baseX = cx + (innerR * cos(angle)).toFloat()
                    val baseY = cy + (innerR * sin(angle) * tiltY).toFloat()
                    val tipX = cx + (outerR * cos(angle)).toFloat()
                    val tipY = cy + (outerR * sin(angle) * tiltY).toFloat()
                    val midX = (baseX + tipX) / 2f + (tipY - baseY) * 0.18f
                    val midY = (baseY + tipY) / 2f - (tipX - baseX) * 0.18f
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(baseX, baseY)
                        lineTo(midX, midY)
                        lineTo(tipX, tipY)
                    }
                    drawPath(
                        path = path,
                        color = androidx.compose.ui.graphics.Color(0xFF4EE6A0).copy(alpha = chargeFlicker),
                        style = Stroke(width = 1.6f, cap = StrokeCap.Round)
                    )
                }
            }

            // --- soft contact shadow beneath the core, grounding it in space ---
            scale(scaleX = 1f, scaleY = tiltY * 0.5f, pivot = Offset(cx, cy + nucleusRadius * 2.2f)) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.35f), Color.Transparent),
                        center = Offset(cx, cy + nucleusRadius * 2.2f),
                        radius = nucleusRadius * 2.4f
                    ),
                    radius = nucleusRadius * 2.4f,
                    center = Offset(cx, cy + nucleusRadius * 2.2f)
                )
            }

            // --- SEARCHING: radar sweep ring ---
            if (state == CoreState.SEARCHING) {
                rotate(degrees = sweepAngle, pivot = Offset(cx, cy)) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(Color.Transparent, CoreCyan.copy(alpha = 0.65f), Color.Transparent)
                        ),
                        startAngle = 0f,
                        sweepAngle = 90f,
                        useCenter = false,
                        style = Stroke(width = 3f),
                        topLeft = Offset(cx - boxSize * 0.42f, cy - boxSize * 0.42f),
                        size = androidx.compose.ui.geometry.Size(boxSize * 0.84f, boxSize * 0.84f)
                    )
                }
            }

            // --- Orion constellation: rigid shape, slowly rotating as one piece around the core ---
            val ox = cx - boxSize * 0.42f
            val oy = cy - boxSize * 0.42f
            val oSize = boxSize * 0.84f
            // The three belt stars are the ones that flare when O.R.I.O.N. is answering.
            val highlightedStarIndices = setOf(2, 3, 4) // Mintaka, Alnilam, Alnitak

            rotate(degrees = constellationAngle, pivot = Offset(cx, cy)) {
                // 1) star-to-star lines — always present, unchanged shape, idle or busy.
                orionLines.forEach { (a, b) ->
                    val p1 = orionStars[a]; val p2 = orionStars[b]
                    drawLine(
                        color = CoreBlue.copy(alpha = 0.35f),
                        start = Offset(ox + p1.x * oSize, oy + p1.y * oSize),
                        end = Offset(ox + p2.x * oSize, oy + p2.y * oSize),
                        strokeWidth = 1f
                    )
                }

                // 2) star-to-core lines — ONLY appear while answering (coreLinkProgress > 0),
                //    fully absent at rest. This is the required "answering" line effect.
                if (coreLinkProgress > 0f) {
                    orionStars.forEach { star ->
                        val sx = ox + star.x * oSize
                        val sy = oy + star.y * oSize
                        drawLine(
                            color = CoreCyan.copy(alpha = coreLinkProgress * 0.55f),
                            start = Offset(sx, sy),
                            end = Offset(cx, cy),
                            strokeWidth = 1f
                        )
                    }
                }

                // 3) the stars themselves — the 3 belt stars brighten and grow while answering,
                //    then relax back to their normal size/brightness once it ends.
                orionStars.forEachIndexed { idx, star ->
                    val boost = if (idx in highlightedStarIndices) coreLinkProgress else 0f
                    val starColor = lerp(InkPrimary, Color.White, boost)
                    val starRadius = star.radius * (1f + boost * 0.7f)
                    drawCircle(
                        color = starColor,
                        radius = starRadius,
                        center = Offset(ox + star.x * oSize, oy + star.y * oSize)
                    )
                }
            }

            // --- orbiting particles on a tilted ellipse, depth-sorted around the nucleus ---
            // Each particle's "depth" (sin of its angle) decides three things at once:
            //   1) size — nearer particles read as bigger
            //   2) brightness — nearer particles are brighter
            //   3) draw order — particles behind the nucleus are painted BEFORE it
            //      (so the core visually occludes them) and particles in front are
            //      painted AFTER it. That occlusion is what sells the depth illusion.
            val particleCount = if (state == CoreState.THINKING) 14 else 8
            val orbitRadius = boxSize * 0.30f

            data class Particle(val x: Float, val y: Float, val depth: Float)

            val particles = (0 until particleCount).map { i ->
                val angle = Math.toRadians((orbitAngle + (360f / particleCount) * i).toDouble())
                val depth = sin(angle).toFloat() // -1 (far/back) .. +1 (near/front)
                Particle(
                    x = cx + (orbitRadius * cos(angle)).toFloat(),
                    y = cy + (orbitRadius * sin(angle) * tiltY).toFloat(),
                    depth = depth
                )
            }

            fun drawParticle(p: Particle) {
                val depthNorm = (p.depth + 1f) / 2f // 0 = far, 1 = near
                val radius = 1.1f + depthNorm * 1.6f
                val alpha = 0.22f + depthNorm * 0.55f
                drawCircle(color = CoreCyan.copy(alpha = alpha), radius = radius, center = Offset(p.x, p.y))
            }

            particles.filter { it.depth < 0f }.forEach(::drawParticle)   // behind the core — draw first
            // front-of-core particles (depth >= 0) are drawn further below, AFTER the nucleus

            // --- LISTENING: rings traveling inward ---
            if (state == CoreState.LISTENING) {
                for (ring in 0..2) {
                    val t = (inwardProgress + ring * 0.33f) % 1f
                    val r = nucleusRadius + t * boxSize * 0.32f
                    drawCircle(
                        color = CoreBlue.copy(alpha = (1f - t) * 0.5f),
                        radius = r,
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.4f)
                    )
                }
            }

            // --- EXECUTING: energy lines radiating outward ---
            if (state == CoreState.EXECUTING) {
                val rayCount = 10
                for (i in 0 until rayCount) {
                    val angle = Math.toRadians((360f / rayCount * i).toDouble())
                    val startR = nucleusRadius + outwardProgress * boxSize * 0.05f
                    val endR = nucleusRadius + boxSize * 0.30f * outwardProgress + boxSize * 0.08f
                    val sx = cx + (startR * cos(angle)).toFloat()
                    val sy = cy + (startR * sin(angle)).toFloat()
                    val ex = cx + (endR * cos(angle)).toFloat()
                    val ey = cy + (endR * sin(angle)).toFloat()
                    drawLine(
                        color = CoreCyan.copy(alpha = (1f - outwardProgress) * 0.8f),
                        start = Offset(sx, sy), end = Offset(ex, ey),
                        strokeWidth = 1.6f, cap = StrokeCap.Round
                    )
                }
            }

            // --- COMPLETE: one decaying outward light wave ---
            if (completePulse > 0f) {
                drawCircle(
                    color = CoreBlue.copy(alpha = (1f - completePulse) * 0.6f),
                    radius = nucleusRadius + completePulse * boxSize * 0.4f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 2f)
                )
            }

            // --- the nucleus, shaded as a lit sphere rather than a flat glow disc ---
            // Base volume gradient, off-center toward the (fake) light source at top-left.
            val lightOffset = Offset(cx - nucleusRadius * 0.35f, cy - nucleusRadius * 0.35f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(if (state == CoreState.ALERT) coreColor else CoreCyan, coreColor, CoreBlueDim.copy(alpha = 0f)),
                    center = lightOffset,
                    radius = nucleusRadius * 2.4f
                ),
                radius = nucleusRadius * 1.4f,
                center = Offset(cx, cy)
            )
            // Dark rim shadow on the opposite side — the cue that reads as "curved surface".
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.28f)),
                    center = Offset(cx + nucleusRadius * 0.4f, cy + nucleusRadius * 0.4f),
                    radius = nucleusRadius * 1.6f
                ),
                radius = nucleusRadius * 1.4f,
                center = Offset(cx, cy)
            )
            // Specular highlight — small, bright, offset toward the light. This single
            // detail is what makes the eye read "sphere" instead of "flat circle".
            drawCircle(
                color = Color.White.copy(alpha = 0.9f),
                radius = nucleusRadius * 0.22f,
                center = Offset(cx - nucleusRadius * 0.4f, cy - nucleusRadius * 0.4f)
            )

            // Front-of-core particles paint last so they visibly pass in front of the sphere.
            particles.filter { it.depth >= 0f }.forEach(::drawParticle)
        }
    }
}
