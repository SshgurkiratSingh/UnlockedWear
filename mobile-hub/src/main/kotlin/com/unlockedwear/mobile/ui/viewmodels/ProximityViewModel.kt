package com.unlockedwear.mobile.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unlockedwear.mobile.data.ProximityRepository
import com.unlockedwear.mobile.data.ProximityState
import com.unlockedwear.mobile.data.WallCondition
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProximityViewModel @Inject constructor(
    private val repository: ProximityRepository
) : ViewModel() {
    val state: StateFlow<ProximityState> = repository.state

    fun scan() = repository.scan()
    fun stopScan() = repository.stopScan()
    fun setCondition(condition: WallCondition) = repository.setCondition(condition)

    fun addCalibrationSample(distanceMeters: Float) {
        viewModelScope.launch { repository.addCalibrationSample(distanceMeters) }
    }

    fun hasRequiredPermissions(): Boolean = repository.hasRequiredPermissions()

    override fun onCleared() {
        repository.stopScan()
        super.onCleared()
    }
}
