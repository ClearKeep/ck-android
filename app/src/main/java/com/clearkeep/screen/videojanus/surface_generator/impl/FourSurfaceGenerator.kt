package com.clearkeep.screen.videojanus.surface_generator.impl

import android.content.Context
import com.clearkeep.screen.videojanus.surface_generator.SurfacePosition

class FourSurfaceGenerator(context: Context) : SurfaceGeneratorImpl(context) {
    override fun getLocalSurface(): SurfacePosition {
        val heightPx = getHeight()
        val widthPx = getWidth()
        val halfHeightPx = heightPx / 2
        val halfWidthPx = widthPx / 2
        return SurfacePosition(
            halfWidthPx,
            halfHeightPx,
            0,
            0
        )
    }

    override fun getRemoteSurfaces(): List<SurfacePosition> {
        val heightPx = getHeight()
        val widthPx = getWidth()
        val halfHeightPx = heightPx / 2
        val halfWidthPx = widthPx / 2
        return listOf(
            SurfacePosition(
                halfWidthPx,
                halfHeightPx,
                widthPx - halfWidthPx,
                0
            ),
            SurfacePosition(
                halfWidthPx,
                halfHeightPx,
                0,
                heightPx - halfHeightPx
            ),
            SurfacePosition(
                halfWidthPx,
                halfHeightPx,
                widthPx - halfWidthPx,
                heightPx - halfHeightPx
            )
        )
    }
}