package com.clearkeep.screen.videojanus.surface_generator.impl

import android.content.Context
import android.widget.LinearLayout
import com.clearkeep.screen.videojanus.surface_generator.SurfacePosition

class ThreeSurfaceGenerator(context: Context) : SurfaceGeneratorImpl(context) {
    override fun getLocalSurface(): SurfacePosition {
        return SurfacePosition(
            200, 300,
            20,
            20
        )
    }

    override fun getRemoteSurfaces(): List<SurfacePosition> {
        val heightPx = getHeight()
        val halfHeightPx = heightPx / 2
        return listOf(
            SurfacePosition(
                LinearLayout.LayoutParams.MATCH_PARENT,
                halfHeightPx,
                0, 0
            ),
            SurfacePosition(
                LinearLayout.LayoutParams.MATCH_PARENT,
                heightPx - halfHeightPx,
                0, halfHeightPx
            ),
        )
    }
}