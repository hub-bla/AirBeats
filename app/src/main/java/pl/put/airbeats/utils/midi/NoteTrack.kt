package pl.put.airbeats.utils.midi

class NoteTrack {
    private var nOfPlays = 0
    val noteOnsTimesInMs = mutableListOf<Double>()

    fun addNoteOn(timeInMs: Double) {
        noteOnsTimesInMs.add(timeInMs)
        nOfPlays += 1
    }

    fun getNOfPlays(): Int {
        return nOfPlays
    }

    override fun toString(): String {
        return "Plays: $nOfPlays times, NoteOns: $noteOnsTimesInMs"
    }
}
