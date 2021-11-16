package com.clearkeep.presentation.screen.videojanus.surfacegenerator

interface SurfaceGenerator {
    fun getLocalSurface(): SurfacePosition
    fun getRemoteSurfaces(): List<SurfacePosition>
}