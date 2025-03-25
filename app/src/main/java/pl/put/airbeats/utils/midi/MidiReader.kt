package pl.put.airbeats.utils.midi

import android.util.Log
import dev.atsushieno.ktmidi.Midi1Music
import dev.atsushieno.ktmidi.read
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import java.io.InputStream

val gmDrumMap = hashMapOf(
    35 to "Acoustic Bass Drum", 36 to "Bass Drum 1", 37 to "Side Stick", 38 to "Acoustic Snare",
    39 to "Hand Clap", 40 to "Electric Snare", 41 to "Low Floor Tom", 42 to "Closed Hi-Hat",
    43 to "High Floor Tom", 44 to "Pedal Hi-Hat", 45 to "Low Tom", 46 to "Open Hi-Hat",
    47 to "Low-Mid Tom", 48 to "Hi-Mid Tom", 49 to "Crash Cymbal 1", 50 to "High Tom",
    51 to "Ride Cymbal 1", 52 to "Chinese Cymbal", 53 to "Ride Bell", 54 to "Tambourine",
    55 to "Splash Cymbal", 56 to "Cowbell", 57 to "Crash Cymbal 2", 58 to "Vibraslap",
    59 to "Ride Cymbal 2", 60 to "Hi Bongo", 61 to "Low Bongo", 62 to "Mute Hi Conga",
    63 to "Open Hi Conga", 64 to "Low Conga", 65 to "High Timbale", 66 to "Low Timbale",
    67 to "High Agogo", 68 to "Low Agogo", 69 to "Cabasa", 70 to "Maracas", 71 to "Short Whistle",
    72 to "Long Whistle", 73 to "Short Guiro", 74 to "Long Guiro", 75 to "Claves",
    76 to "Hi Wood Block", 77 to "Low Wood Block", 78 to "Mute Cuica", 79 to "Open Cuica",
    80 to "Mute Triangle", 81 to "Open Triangle"
)


class MidiReader {
    var noteTracks = HashMap<String, NoteTrack>()
    val client = HttpClient()


    suspend fun read(url: String, bpm: Int): HashMap<String, NoteTrack> {
        val response: HttpResponse = client.get(url)
        val bytes = response.bodyAsBytes().toList()

        val music = Midi1Music()
        music.read(bytes)

        noteTracks = convertMidiToNoteTracks(music, bpm)

        for ((noteName, noteTrack) in noteTracks) {
            Log.d("Note data", "Note: $noteName, $noteTrack")
        }

        return noteTracks
    }

    // Note: bpm should be in file name or sent as metadata from api
    // following default value is just for testing purposes
    private fun convertMidiToNoteTracks(music: Midi1Music, bpm: Int): HashMap<String, NoteTrack> {
        val noteTracks = HashMap<String, NoteTrack>()
        val ppqn = music.deltaTimeSpec.toDouble()
        val MILISECONDS_IN_MIN = 60000.0

        var culTime = 0.0

        music.tracks.forEachIndexed { index, track ->
            Log.d("MidiTrack", "Track $index: ${track.events}")

            track.events.forEach { event ->
                val type = when (event.message.statusCode.toInt() and 0xF0) {
                    0x80 -> "Note Off"
                    0x90 -> "Note On"
                    0xA0 -> "Aftertouch"
                    0xB0 -> "Control Change"
                    0xC0 -> "Program Change"
                    0xD0 -> "Channel Pressure"
                    0xE0 -> "Pitch Bend"
                    else -> "Unknown"
                }
                if (type == "Note On" || type == "Note Off") { // Note On event
                    val noteNumber = event.message.msb // First data byte: note number
//                val velocity = event.message.lsb  // Second data byte: velocity
                    val time =
                        event.deltaTime.toDouble() * (MILISECONDS_IN_MIN / (ppqn * bpm.toDouble()))
                    culTime += time
                    if (type == "Note On") {
                        val noteName = gmDrumMap.getOrDefault(noteNumber.toInt(), "No name")

                        val noteTrack = noteTracks.getOrDefault(noteName, NoteTrack())
                        noteTrack.addNoteOn(culTime)
                        noteTracks[noteName] = noteTrack
                    }
                    Log.d(
                        "MidiEvent",
                        "${event.deltaTime} : $type - Note: $noteNumber When to run: ${culTime}ms"
                    )
                }
            }
        }

        return noteTracks
    }

}