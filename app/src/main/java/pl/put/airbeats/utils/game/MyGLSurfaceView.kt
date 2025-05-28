package pl.put.airbeats.utils.game

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import android.util.Log
import pl.put.airbeats.utils.midi.NoteTrack

class MyGLSurfaceView(context: Context) : GLSurfaceView(context) {
    lateinit var renderer: MyGLRenderer



//    constructor(
//        context: Context,
//    ) : super(context) {
//        val noteTrack: Map<String, NoteTrack> = emptyMap<String, NoteTrack>()
//        val bpm = 0
//
//        // Create an OpenGL ES 2.0 context
//        setEGLContextClientVersion(2)
//
//
//
//
//
//        renderer = MyGLRenderer(noteTrack, bpm)
//
//        setRenderer(renderer)
//
//
//    }

    constructor(
        context: Context,
        noteTrack: Map<String, NoteTrack> = emptyMap<String, NoteTrack>(),
        bpm: Int = 0,
        onLevelEnd: (LevelStatistics) -> Unit,
    ) : this(context) {
        // Create an OpenGL ES 2.0 context
        setEGLContextClientVersion(2)

        renderer = MyGLRenderer(noteTrack, bpm, onLevelEnd)

        setRenderer(renderer)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        // MotionEvent reports input details from the touch screen
        // and other input controls. In this case, you are only
        // interested in events where the touch position changed.

        val x: Float = e.x / width * 2 - 1 // Change coordinate to range [-1, 1]
        val y: Float = e.y / height * 2 - 1

        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                Log.d("Touch event", "touched at ($x, $y)")
                renderer.columnEvent = when {
                    x < -0.5 -> 0
                    x < 0 -> 1
                    x < 0.5 -> 2
                    else -> 3
                }
                renderer.hasEventOccured = true
            }
        }

        return true
    }
}