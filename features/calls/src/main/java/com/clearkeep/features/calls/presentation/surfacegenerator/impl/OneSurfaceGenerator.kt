package com.clearkeep.features.calls.presentation.surfacegenerator.impl

import android.content.Context
import android.widget.LinearLayout
import com.clearkeep.features.calls.presentation.surfacegenerator.SurfacePosition

class OneSurfaceGenerator(context: Context, width: Int, height: Int) : SurfaceGeneratorImpl(
    width,
    height
) {
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