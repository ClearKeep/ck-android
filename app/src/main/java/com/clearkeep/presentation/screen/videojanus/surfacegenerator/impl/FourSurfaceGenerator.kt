package com.clearkeep.presentation.screen.videojanus.surfacegenerator.impl

import android.content.Context
import com.clearkeep.presentation.screen.videojanus.surfacegenerator.SurfacePosition

class FourSurfaceGenerator(context: Context, width: Int, height: Int) : SurfaceGeneratorImpl(
    width,
    height
) {
    override fun getLocalSurface(): SurfacePosition {
        val heightPx = getHeight()
        val widthPx = getWidth()
        val halfHeightPx = heightPx / 2
        val halfWidthPx = widthPx / 2
        return SurfacePosition(
            halfWidthPx,
            halfHeightPx,
            halfWidthPx,
            halfHeightPx
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
                0,
                0
            ),
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
        )
    }
}