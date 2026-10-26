package ao.ridehailing.driver.feature.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ao.ridehailing.driver.R

@Composable
fun OnboardingRoute(driverId: String, viewModel: OnboardingViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(driverId) { viewModel.resume(driverId) }
    when (state.step) {
        "background" -> BackgroundCheckScreen(state, viewModel::poll)
        "video" -> VideoVerificationScreen(state, viewModel::submitVideo)
        "complete" -> OnboardingCompleteScreen()
        else -> DocumentUploadScreen(state, viewModel::upload)
    }
}

@Composable
fun DocumentUploadScreen(state: OnboardingUi, onUpload: (String, ByteArray) -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text(stringResource(R.string.onboarding_documents))
        DocButton(R.string.doc_vtc, state.vtc) { onUpload("vtc", byteArrayOf(1)) }
        DocButton(R.string.doc_insurance, state.insurance) { onUpload("insurance", byteArrayOf(1)) }
        DocButton(R.string.doc_registration, state.registration) { onUpload("registration", byteArrayOf(1)) }
        DocButton(R.string.doc_id, state.identity) { onUpload("id", byteArrayOf(1)) }
        state.message?.let { Text(it) }
    }
}

@Composable
fun BackgroundCheckScreen(state: OnboardingUi, onPoll: () -> Unit) {
    Column(Modifier.padding(24.dp)) {
        Text(stringResource(R.string.onboarding_background))
        Text(state.background)
        Button(onClick = onPoll) { Text(stringResource(R.string.onboarding_refresh)) }
    }
}

@Composable
fun VideoVerificationScreen(state: OnboardingUi, onVideo: (ByteArray) -> Unit) {
    Column(Modifier.padding(24.dp)) {
        Text(stringResource(R.string.onboarding_video))
        Button(onClick = { onVideo(ByteArray(16)) }) { Text(stringResource(R.string.onboarding_send_video)) }
        state.message?.let { Text(it) }
    }
}

@Composable
fun OnboardingCompleteScreen() {
    Text(stringResource(R.string.onboarding_complete), Modifier.padding(24.dp))
}

@Composable
private fun DocButton(label: Int, done: Boolean, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = !done) { Text(stringResource(label)) }
}
