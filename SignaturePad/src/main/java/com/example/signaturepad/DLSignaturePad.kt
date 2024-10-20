package com.example.signaturepad

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitDragOrCancellation
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

@Composable
fun DLSignaturePad(
    state: DLSignaturePadState,
    modifier: Modifier = Modifier,
    signatureColor: Color = Color.Black,
    signatureThickness: Dp = 6.dp,
) {
    val currentDrawingPosition = remember { mutableStateOf<Offset?>(null) }
    Column {
        Box(
            modifier = modifier
                .pointerInput(Unit) {
                    while (true) {
                        awaitPointerEventScope {
                            // It suspends the coroutine until the first touch event
                            // (or "down" event) is detected
                            val down = awaitFirstDown()
                            // Handle the start of drawing (dot or start of a drag)
                            state.signaturePath.moveTo(down.position.x, down.position.y)
                            currentDrawingPosition.value = down.position
                            // Check if it's a drag gesture or just a tap
                            var dragEvent = awaitTouchSlopOrCancellation(down.id) { change, _ ->
                                change.consume()
                            }

                            if (dragEvent != null) {
                                // Handle the drag gesture
                                var previousPoint = down.position
                                do {
                                    val newPoint = dragEvent?.position
                                    // Add smooth curve for drag gesture
                                    if (newPoint != null) {
                                        val path = state.signaturePath.apply {
                                            this.quadraticBezierTo(
                                                previousPoint.x,
                                                previousPoint.y,  // Control point (previous point)
                                                (previousPoint.x + newPoint.x) / 2f,  // Midpoint for smooth curve
                                                (previousPoint.y + newPoint.y) / 2f
                                            )
                                        }
                                        state.updateSignaturePath(path)
                                    }

                                    if (newPoint != null) {
                                        previousPoint = newPoint
                                    }
                                    currentDrawingPosition.value = newPoint
                                    dragEvent?.consume()

                                    // Await the next drag event
                                    if (dragEvent != null) {
                                        // awaitDragOrCancellation is used to await the next drag event or
                                        // cancellation (e.g., lifting the finger off the screen)
                                        dragEvent = awaitDragOrCancellation(dragEvent.id)
                                    }
                                } while (dragEvent != null)

                                // After drag ends, reset the position
                                currentDrawingPosition.value = null
                            } else {
                                // If no drag, it's a single tap (dot)
                                val path = state.signaturePath.apply {
                                    this.lineTo(down.position.x, down.position.y)
                                }

                                state.updateSignaturePath(path)
                            }
                        }
                    }
                }
                .drawWithContent {
                    drawContent()
                    state.updateSignatureBitmap(
                        toImageBitmap(
                            signaturePath = state.signaturePath,
                            width = size.width.toInt(),
                            height = size.height.toInt(),
                            signatureColor = signatureColor,
                            signatureThickness = signatureThickness,
                        ),
                    )
                },
        ) {
            state.signatureBitmap?.let {
                Image(
                    bitmap = it,
                    contentDescription = "Signature",
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

/**
 * Converts a signature path into an [ImageBitmap] with the specified width, height, color, and
 * thickness.
 *
 * @param signaturePath The [Path] object representing the signature.
 * @param width The width of the resulting [ImageBitmap].
 * @param height The height of the resulting [ImageBitmap].
 * @param signatureColor The [Color] of the signature path to be drawn.
 * @param signatureThickness The thickness of the signature path, in [Dp].
 * @return An [ImageBitmap] containing the rendered signature.
 */
private fun toImageBitmap(
    signaturePath: Path,
    width: Int,
    height: Int,
    signatureColor: Color,
    signatureThickness: Dp,
): ImageBitmap {
    val imgBitmap = ImageBitmap(width, height)
    Canvas(imgBitmap).apply {
        CanvasDrawScope().draw(
            density = Density(1f, 1f),
            layoutDirection = LayoutDirection.Ltr,
            canvas = this,
            size = Size(width.toFloat(), height.toFloat()),
        ) {
            drawPath(
                path = signaturePath,
                color = signatureColor,
                style = Stroke(
                    width = signatureThickness.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
    return imgBitmap
}

/**
 * A state holder class for managing the signature pad state.
 */
@Stable
class DLSignaturePadState {
    var signaturePath by mutableStateOf(Path())
        private set
    var signatureBitmap by mutableStateOf<ImageBitmap?>(null)
        private set

    /**
     * Clears the current signature by resetting the path to an empty one.
     */
    fun clearSignature() {
        signaturePath = Path()
    }

    /**
     * Updates the signature path with a new path.
     *
     * @param path The path to add to the current signature.
     */
    fun updateSignaturePath(path: Path) {
        signaturePath = Path().apply { addPath(path) }
    }

    /**
     * Updates the signature bitmap with the provided image.
     *
     * @param bitmap The bitmap representing the signature.
     */
    fun updateSignatureBitmap(bitmap: ImageBitmap) {
        signatureBitmap = bitmap
    }
}