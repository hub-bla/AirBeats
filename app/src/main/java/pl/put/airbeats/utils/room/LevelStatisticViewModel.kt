package pl.put.airbeats.utils.room

import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class LevelStatisticViewModel(
    private val dao: LevelStatisticDao
): ViewModel() {
    fun insert(levelStatistics: LevelStatisticEntity) {
        viewModelScope.launch {
            dao.insert(levelStatistics)
        }
    }

    fun selectUser(userID: String): Flow<List<LevelStatisticEntity>> {
        return dao.getUser(userID)
    }
}