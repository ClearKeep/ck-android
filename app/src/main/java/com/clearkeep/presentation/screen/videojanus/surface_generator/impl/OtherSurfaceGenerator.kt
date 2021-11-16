package com.clearkeep.presentation.screen.videojanus.surface_generator.impl

import android.content.Context
import com.clearkeep.presentation.screen.videojanus.surface_generator.SurfacePosition
import com.clearkeep.utilities.isOdd
import com.clearkeep.utilities.printlnCK
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

class OtherSurfaceGenerator(
    var size: Int,
    val context: Context,
    width: Int,
    height: Int
) : SurfaceGeneratorImpl(width, height) {
    private val numberOfRow: Int = ceil(size / 2.0).roundToInt()

    override fun getLocalSurface(): SurfacePosition {
        val heightPx = getHeight()
        val widthPx = getWidth()
        val itemHeightPx = heightPx / 4
        val halfWidthPx = widthPx / 2
        return SurfacePosition(
            halfWidthPx,
            itemHeightPx,
            widthPx - halfWidthPx,
            (numberOfRow - 1) * itemHeightPx
        )
    }

    override fun getRemoteSurfaces(): List<SurfacePosition> {
        val heightPx = getHeight()
        val widthPx = getWidth()
        val itemHeightPx = heightPx / 4
        val halfWidthPx = widthPx / 2

        val result = mutableListOf<SurfacePosition>()
        if (isOdd(size)) {
            val firstItem = SurfacePosition(
                widthPx,
                itemHeightPx,
                0,
                0
            )
            result.add(firstItem)

            for (index in 1 until size - 1) {
                val top = ceil(index / 2.0).roundToInt() * itemHeightPx
                val start = if (isOdd(index)) 0 else widthPx - halfWidthPx
                result.add(
                    SurfacePosition(
                        halfWidthPx,
                        itemHeightPx,
                        start,
                        top
                    )
                )
            }
        } else {
            for (index in 0 until size - 1) {
                val top = floor(index / 2.0).roundToInt() * itemHeightPx
                printlnCK("top = $top")
                val start = if (!isOdd(index)) 0 else widthPx - halfWidthPx
                result.add(
                    SurfacePosition(
                        halfWidthPx,
                        itemHeightPx,
                        start,
                        top
                    )
                )
            }
        }
        return result
    }
}