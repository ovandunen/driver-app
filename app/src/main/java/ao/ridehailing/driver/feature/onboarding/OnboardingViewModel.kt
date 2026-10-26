package ao.ridehailing.driver.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ao.ridehailing.driver.core.network.NetworkResult
import ao.ridehailing.driver.core.network.OnboardingApi
import ao.ridehailing.driver.core.storage.OnboardingDao
import ao.ridehailing.driver.core.storage.OnboardingEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUi(
    val step: String = "documents",
    val vtc: Boolean = false,
    val insurance: Boolean = false,
    val registration: Boolean = false,
    val identity: Boolean = false,
    val background: String = "pending",
    val video: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val api: OnboardingApi,
    private val dao: OnboardingDao,
    private val schedule: OnboardingSchedule,
) : ViewModel() {
    private val states = MutableStateFlow(OnboardingUi())
    val state: StateFlow<OnboardingUi> = states.asStateFlow()
    private var driverId = ""

    fun resume(id: String) {
        driverId = id
        viewModelScope.launch {
            val saved = dao.find(id) ?: return@launch
            states.value = saved.toUi()
        }
    }

    fun upload(kind: String, bytes: ByteArray) {
        viewModelScope.launch {
            when (val result = api.uploadDocument(kind, bytes)) {
                is NetworkResult.Failure -> states.value = states.value.copy(message = result.message)
                is NetworkResult.Success -> applyDocument(kind)
            }
        }
    }

    fun poll() {
        schedule.enqueue(driverId)
        viewModelScope.launch {
            when (val result = api.status()) {
                is NetworkResult.Failure -> states.value = states.value.copy(message = result.message)
                is NetworkResult.Success -> {
                    val next = if (result.value == "approved") "video" else states.value.step
                    states.value = states.value.copy(background = result.value, step = next, message = null)
                    persist()
                }
            }
        }
    }

    fun submitVideo(bytes: ByteArray) {
        viewModelScope.launch {
            when (val result = api.uploadVideo(bytes)) {
                is NetworkResult.Failure -> states.value = states.value.copy(message = result.message)
                is NetworkResult.Success -> {
                    states.value = states.value.copy(video = true, step = "complete", message = null)
                    persist()
                }
            }
        }
    }

    private suspend fun applyDocument(kind: String) {
        val current = states.value
        val next = when (kind) {
            "vtc" -> current.copy(vtc = true)
            "insurance" -> current.copy(insurance = true)
            "registration" -> current.copy(registration = true)
            "id" -> current.copy(identity = true)
            else -> current.copy(message = kind)
        }
        val ready = next.vtc && next.insurance && next.registration && next.identity
        states.value = next.copy(step = if (ready) "background" else "documents", message = null)
        persist()
    }

    private suspend fun persist() {
        if (driverId.isEmpty()) return
        dao.save(states.value.toEntity(driverId))
    }
}

private fun OnboardingEntity.toUi() = OnboardingUi(step, vtc, insurance, registration, identity, backgroundStatus, videoUploaded)

private fun OnboardingUi.toEntity(driverId: String) = OnboardingEntity(
    driverId, step, vtc, insurance, registration, identity, background, video,
)
