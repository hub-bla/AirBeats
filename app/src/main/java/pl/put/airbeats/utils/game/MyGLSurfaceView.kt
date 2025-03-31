package pl.put.airbeats.utils.game

import android.content.Context
import android.opengl.GLSurfaceView
import pl.put.airbeats.utils.midi.NoteTrack

class MyGLSurfaceView(context: Context) : GLSurfaceView(context) {
    private val renderer: MyGLRenderer

    init {
        // Create an OpenGL ES 2.0 context
        setEGLContextClientVersion(2)

        renderer = MyGLRenderer()
    }

    constructor(context: Context, noteTrack: Map<String, NoteTrack>, bpm: Int) : this(context){
        renderer.passData(noteTrack, bpm)

        setRenderer(renderer)
    }
}