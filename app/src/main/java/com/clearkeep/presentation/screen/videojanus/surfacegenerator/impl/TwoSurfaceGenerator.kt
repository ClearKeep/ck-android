package com.clearkeep.presentation.screen.videojanus.surfacegenerator.impl

import android.content.Context
import android.widget.LinearLayout
import com.clearkeep.presentation.screen.videojanus.surfacegenerator.SurfacePosition

class TwoSurfaceGenerator(context: Context, width: Int, height: Int) : SurfaceGeneratorImpl(
    width,
    height
) {
    override fun getLocalSurface(): SurfacePosition {
        val heightPx = getHeight()
        val halfHeightPx = heightPx / 2
        return SurfacePosition(
            LinearLayout.LayoutParams.MATCH_PARENT,
            halfHeightPx,
            0, 0
        )
    }

    override fun getRemoteSurfaces(): List<SurfacePosition> {
        val heightPx = getHeight()
        val halfHeightPx = heightPx / 2
        return listOf(
            SurfacePosition(
                LinearLayout.LayoutParams.MATCH_PARENT,
                heightPx - halfHeightPx,
                0, halfHeightPx
            )
        )
    }
}