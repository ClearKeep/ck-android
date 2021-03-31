package com.clearkeep.screen.videojanus.surface_generator.impl

import android.content.Context
import android.widget.LinearLayout
import com.clearkeep.screen.videojanus.surface_generator.SurfacePosition

class TwoSurfaceGenerator(context: Context) : SurfaceGeneratorImpl(context) {
    override fun getLocalSurface(): SurfacePosition {
        return SurfacePosition(
            200, 300,
            20,
            20
        )
    }

    override fun getRemoteSurfaces(): List<SurfacePosition> {
        return listOf(
            SurfacePosition(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT,
                0, 0
            )
        )
    }
}