package com.clearkeep.presentation.screen.videojanus.surface_generator.impl

import android.content.Context
import com.clearkeep.presentation.screen.videojanus.surface_generator.SurfacePosition

class ThreeSurfaceGenerator(context: Context, width: Int, height: Int) : SurfaceGeneratorImpl(
    width,
    height
) {
    override fun getLocalSurface(): SurfacePosition {
        val heightPx = getHeight()
        val halfHeightPx = heightPx / 2
        val widthPx = getWidth()
        val halfWidthPx = widthPx / 2
        return SurfacePosition(
            halfWidthPx,
            heightPx - halfHeightPx,
            halfWidthPx,
            halfHeightPx
        )
    }

    override fun getRemoteSurfaces(): List<SurfacePosition> {
        val heightPx = getHeight()
        val halfHeightPx = heightPx / 2
        val widthPx = getWidth()
        val halfWidthPx = widthPx / 2
        return listOf(
            SurfacePosition(
                widthPx,
                halfHeightPx,
                0,
                0
            ),
            SurfacePosition(
                halfWidthPx,
                heightPx - halfHeightPx,
                0,
                halfHeightPx
            )
        )
    }
}