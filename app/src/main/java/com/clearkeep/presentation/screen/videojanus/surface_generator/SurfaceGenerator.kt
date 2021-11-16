package com.clearkeep.presentation.screen.videojanus.surface_generator

interface SurfaceGenerator {
    fun getLocalSurface(): SurfacePosition
    fun getRemoteSurfaces(): List<SurfacePosition>
}