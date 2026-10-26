package ao.ridehailing.driver.core.architecture

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

abstract class BaseViewModel<S : ViewState>(initialState: S) : ViewModel() {
    private val states = MutableStateFlow(initialState)
    val state: StateFlow<S> = states.asStateFlow()

    protected val currentState: S
        get() = states.value

    protected fun setState(next: S) {
        states.value = next
    }
}
