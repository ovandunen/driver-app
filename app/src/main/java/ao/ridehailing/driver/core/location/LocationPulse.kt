package ao.ridehailing.driver.core.location

import ao.ridehailing.driver.di.AppScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

interface LocationPulse {
    fun start(block: suspend () -> Unit)
    fun stop()
}

@Singleton
class IntervalPulse @Inject constructor(
    @AppScope private val scope: CoroutineScope,
) : LocationPulse {
    private var job: Job? = null

    override fun start(block: suspend () -> Unit) {
        job?.cancel()
        job = scope.launch {
            while (true) {
                delay(4_000)
                block()
            }
        }
    }

    override fun stop() {
        job?.cancel()
        job = null
    }
}
