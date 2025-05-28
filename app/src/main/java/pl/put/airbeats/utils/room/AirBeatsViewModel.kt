package pl.put.airbeats.utils.room

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AirBeatsViewModel(
    private val dao: LevelStatisticDao
): ViewModel() {
    private val _isSavingEnergy = MutableStateFlow(false)
    val isSavingEnergy = _isSavingEnergy.asStateFlow()

    fun insert(levelStatistics: LevelStatisticEntity) {
        viewModelScope.launch {
            dao.insert(levelStatistics)
        }
    }

    fun delete(levelStatistics: LevelStatisticEntity) {
        viewModelScope.launch {
            dao.delete(levelStatistics)
        }
    }

    fun selectUser(userID: String): Flow<List<LevelStatisticEntity>> {
        return dao.getUser(userID)
    }

    fun changeSavingMode(value: Boolean) {
        _isSavingEnergy.value = value
    }
}