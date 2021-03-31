package com.clearkeep.screen.videojanus.surface_generator.impl

import android.content.Context
import com.clearkeep.screen.videojanus.surface_generator.SurfacePosition

class SixSurfaceGenerator(context: Context) : SurfaceGeneratorImpl(context) {
    override fun getLocalSurface(): SurfacePosition {
        val heightPx = getHeight()
        val widthPx = getWidth()
        val aThirdHeightPx = heightPx / 3
        val halfWidthPx = widthPx / 2
        return SurfacePosition(
            halfWidthPx,
            aThirdHeightPx,
            0,
            0
        )
    }

    override fun getRemoteSurfaces(): List<SurfacePosition> {
        val heightPx = getHeight()
        val widthPx = getWidth()
        val aThirdHeightPx = heightPx / 3
        val halfWidthPx = widthPx / 2

        return listOf(
            SurfacePosition(
                halfWidthPx,
                aThirdHeightPx,
                widthPx - halfWidthPx,
                0
            ),
            SurfacePosition(
                halfWidthPx,
                aThirdHeightPx,
                0,
                aThirdHeightPx
            ),
            SurfacePosition(
                halfWidthPx,
                aThirdHeightPx,
                widthPx - halfWidthPx,
                aThirdHeightPx
            ),
            SurfacePosition(
                halfWidthPx,
                heightPx - 2 * aThirdHeightPx,
                0,
                2 * aThirdHeightPx
            ),
            SurfacePosition(
                halfWidthPx,
                heightPx - 2 * aThirdHeightPx,
                widthPx - halfWidthPx,
                2 * aThirdHeightPx
            )
        )
    }
}