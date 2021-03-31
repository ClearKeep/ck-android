package com.clearkeep.screen.videojanus.surface_generator.impl

import android.content.Context
import android.widget.LinearLayout
import com.clearkeep.screen.videojanus.surface_generator.SurfacePosition

class OneSurfaceGenerator(context: Context) : SurfaceGeneratorImpl(context) {
    override fun getLocalSurface(): SurfacePosition {
        return SurfacePosition(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            0
        )
    }

    override fun getRemoteSurfaces(): List<SurfacePosition> {
        return emptyList()
    }
}