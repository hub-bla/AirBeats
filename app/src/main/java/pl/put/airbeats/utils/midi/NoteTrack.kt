package pl.put.airbeats.utils.midi

class NoteTrack {
    private var nOfPlays = 0
    val noteOnsTimesInMs = mutableListOf<Pair<Double, Double>>()

    fun addNoteOn(noteDuration: Pair<Double, Double>) {
        noteOnsTimesInMs.add(noteDuration)
        nOfPlays += 1
    }

    fun getNOfPlays(): Int {
        return nOfPlays
    }

    override fun toString(): String {
        return "Plays: $nOfPlays times, NoteOns: $noteOnsTimesInMs"
    }
}
