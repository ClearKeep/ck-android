package com.clearkeep.screen.videojanus.surface_generator.impl

import android.content.Context
import com.clearkeep.screen.videojanus.surface_generator.SurfacePosition

class EightSurfaceGenerator(context: Context) : SurfaceGeneratorImpl(context) {
    override fun getLocalSurface(): SurfacePosition {
        val heightPx = getHeight()
        val widthPx = getWidth()
        val aFourHeightPx = heightPx / 4
        val halfWidthPx = widthPx / 2
        return SurfacePosition(
            halfWidthPx,
            heightPx - 3 * aFourHeightPx,
            widthPx - halfWidthPx,
            3 * aFourHeightPx
        )
    }

    override fun getRemoteSurfaces(): List<SurfacePosition> {
        val heightPx = getHeight()
        val widthPx = getWidth()
        val aFourHeightPx = heightPx / 4
        val halfWidthPx = widthPx / 2

        return listOf(
            SurfacePosition(
                halfWidthPx,
                aFourHeightPx,
                0,
                0
            ),
            SurfacePosition(
                halfWidthPx,
                aFourHeightPx,
                widthPx - halfWidthPx,
                0
            ),
            SurfacePosition(
                halfWidthPx,
                aFourHeightPx,
                0,
                aFourHeightPx
            ),
            SurfacePosition(
                halfWidthPx,
                aFourHeightPx,
                widthPx - halfWidthPx,
                aFourHeightPx
            ),
            SurfacePosition(
                halfWidthPx,
                heightPx - 2 * aFourHeightPx,
                0,
                2 * aFourHeightPx
            ),
            SurfacePosition(
                halfWidthPx,
                heightPx - 2 * aFourHeightPx,
                widthPx - halfWidthPx,
                2 * aFourHeightPx
            ),
            SurfacePosition(
                halfWidthPx,
                heightPx - 3 * aFourHeightPx,
                0,
                3 * aFourHeightPx
            ),
        )
    }
}