package ao.ridehailing.driver.feature.onboarding

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

interface OnboardingSchedule {
    fun enqueue(driverId: String)
}

object OnboardingPoll {
    const val SECONDS = 5L
}

class OnboardingWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = Result.success()
}

@Singleton
class WorkOnboardingSchedule @Inject constructor(
    @ApplicationContext private val context: Context,
) : OnboardingSchedule {
    override fun enqueue(driverId: String) {
        val request = OneTimeWorkRequestBuilder<OnboardingWorker>()
            .setInitialDelay(OnboardingPoll.SECONDS, TimeUnit.SECONDS)
            .setInputData(workDataOf("driverId" to driverId))
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }
}
