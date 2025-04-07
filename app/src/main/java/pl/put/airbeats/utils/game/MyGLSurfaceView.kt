package pl.put.airbeats.utils.game

import android.content.Context
import android.opengl.GLSurfaceView
import pl.put.airbeats.utils.midi.NoteTrack

class MyGLSurfaceView : GLSurfaceView {
    private val renderer: MyGLRenderer

    constructor(
        context: Context,
    ) : super(context){
        val noteTrack: Map<String, NoteTrack> = emptyMap<String, NoteTrack>()
        val bpm = 0

        // Create an OpenGL ES 2.0 context
        setEGLContextClientVersion(2)

        renderer = MyGLRenderer(noteTrack, bpm)

        setRenderer(renderer)
    }

    constructor(
        context: Context,
        noteTrack: Map<String, NoteTrack> = emptyMap<String, NoteTrack>(),
        bpm: Int = 0
    ) : super(context){
        // Create an OpenGL ES 2.0 context
        setEGLContextClientVersion(2)

        renderer = MyGLRenderer(noteTrack, bpm)

        setRenderer(renderer)
    }
}