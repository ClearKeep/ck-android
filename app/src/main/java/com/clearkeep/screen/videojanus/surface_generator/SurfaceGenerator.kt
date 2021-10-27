package com.clearkeep.screen.videojanus.surface_generator

interface SurfaceGenerator {
    fun getLocalSurface(): SurfacePosition
    fun getRemoteSurfaces(): List<SurfacePosition>
}