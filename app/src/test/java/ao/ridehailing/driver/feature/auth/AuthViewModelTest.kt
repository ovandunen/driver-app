package ao.ridehailing.driver.feature.auth

import ao.ridehailing.driver.core.network.NetworkResult
import ao.ridehailing.driver.core.storage.TokenStore
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<AuthRepository>()
    private val tokenStore = mockk<TokenStore>(relaxUnitFun = true)

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun startsIdle() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.state.test {
            assertEquals(AuthState.Idle, awaitItem())
        }
    }

    @Test
    fun sendOtp_rejectsInvalidPhone() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.state.test {
            assertEquals(AuthState.Idle, awaitItem())
            viewModel.sendOtp("812345678")
            assertEquals(AuthState.Error(AuthState.Error.Reason.InvalidPhone), awaitItem())
        }
        coVerify(exactly = 0) { repository.sendOtp(any()) }
    }

    @Test
    fun sendOtp_normalizesLocalNumber() = runTest(dispatcher) {
        coEvery { repository.sendOtp("+244912345678") } returns NetworkResult.Success(Unit)
        val viewModel = viewModel()
        viewModel.state.test {
            assertEquals(AuthState.Idle, awaitItem())
            viewModel.sendOtp("912 345 678")
            assertEquals(AuthState.SendingOtp, awaitItem())
            advanceUntilIdle()
            assertEquals(AuthState.OtpSent("+244912345678"), awaitItem())
        }
    }

    @Test
    fun sendOtp_acceptsCountryCode() = runTest(dispatcher) {
        coEvery { repository.sendOtp("+244923000111") } returns NetworkResult.Success(Unit)
        val viewModel = viewModel()
        viewModel.state.test {
            assertEquals(AuthState.Idle, awaitItem())
            viewModel.sendOtp("+244923000111")
            assertEquals(AuthState.SendingOtp, awaitItem())
            advanceUntilIdle()
            assertEquals(AuthState.OtpSent("+244923000111"), awaitItem())
        }
    }

    @Test
    fun sendOtp_requestFailure() = runTest(dispatcher) {
        coEvery { repository.sendOtp("+244912345678") } returns NetworkResult.Failure()
        val viewModel = viewModel()
        viewModel.state.test {
            assertEquals(AuthState.Idle, awaitItem())
            viewModel.sendOtp("912345678")
            assertEquals(AuthState.SendingOtp, awaitItem())
            advanceUntilIdle()
            assertEquals(AuthState.Error(AuthState.Error.Reason.RequestFailed), awaitItem())
        }
        verify(exactly = 0) { tokenStore.save(any(), any(), any()) }
    }

    @Test
    fun sendOtp_ignoresSecondTapWhileSending() = runTest(dispatcher) {
        coEvery { repository.sendOtp("+244912345678") } returns NetworkResult.Success(Unit)
        val viewModel = viewModel()
        viewModel.state.test {
            assertEquals(AuthState.Idle, awaitItem())
            viewModel.sendOtp("912345678")
            assertEquals(AuthState.SendingOtp, awaitItem())
            viewModel.sendOtp("912345678")
            advanceUntilIdle()
            assertEquals(AuthState.OtpSent("+244912345678"), awaitItem())
        }
        coVerify(exactly = 1) { repository.sendOtp("+244912345678") }
    }

    @Test
    fun verifyOtp_doesNothingBeforeCodeIsSent() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.verifyOtp("1234")
        advanceUntilIdle()
        viewModel.state.test {
            assertEquals(AuthState.Idle, awaitItem())
        }
        coVerify(exactly = 0) { repository.verifyOtp(any(), any()) }
    }

    @Test
    fun verifyOtp_rejectsShortCode() = runTest(dispatcher) {
        coEvery { repository.sendOtp("+244912345678") } returns NetworkResult.Success(Unit)
        val viewModel = viewModel()
        viewModel.sendOtp("912345678")
        advanceUntilIdle()
        viewModel.state.test {
            assertEquals(AuthState.OtpSent("+244912345678"), awaitItem())
            viewModel.verifyOtp("12")
            assertEquals(
                AuthState.Error(AuthState.Error.Reason.InvalidCode, "+244912345678"),
                awaitItem(),
            )
        }
        coVerify(exactly = 0) { repository.verifyOtp(any(), any()) }
    }

    @Test
    fun verifyOtp_storesTokenAndAuthenticates() = runTest(dispatcher) {
        coEvery { repository.sendOtp("+244912345678") } returns NetworkResult.Success(Unit)
        coEvery { repository.verifyOtp("+244912345678", "1234") } returns NetworkResult.Success(
            VerifiedSession("tok", "drv-1", "2026-12-01T00:00:00Z"),
        )
        val viewModel = viewModel()
        viewModel.sendOtp("912345678")
        advanceUntilIdle()
        viewModel.state.test {
            assertEquals(AuthState.OtpSent("+244912345678"), awaitItem())
            viewModel.verifyOtp("1234")
            assertEquals(AuthState.Verifying("+244912345678"), awaitItem())
            advanceUntilIdle()
            assertEquals(AuthState.Authenticated("drv-1"), awaitItem())
        }
        verify { tokenStore.save("tok", "drv-1", "2026-12-01T00:00:00Z") }
    }

    @Test
    fun verifyOtp_keepsPhoneAfterFailure() = runTest(dispatcher) {
        coEvery { repository.sendOtp("+244912345678") } returns NetworkResult.Success(Unit)
        coEvery { repository.verifyOtp("+244912345678", "1234") } returns NetworkResult.Failure()
        val viewModel = viewModel()
        viewModel.sendOtp("912345678")
        advanceUntilIdle()
        viewModel.state.test {
            assertEquals(AuthState.OtpSent("+244912345678"), awaitItem())
            viewModel.verifyOtp("1234")
            assertEquals(AuthState.Verifying("+244912345678"), awaitItem())
            advanceUntilIdle()
            assertEquals(
                AuthState.Error(AuthState.Error.Reason.RequestFailed, "+244912345678"),
                awaitItem(),
            )
        }
        verify(exactly = 0) { tokenStore.save(any(), any(), any()) }
    }

    @Test
    fun verifyOtp_canRetryAfterFailure() = runTest(dispatcher) {
        coEvery { repository.sendOtp("+244912345678") } returns NetworkResult.Success(Unit)
        coEvery { repository.verifyOtp("+244912345678", "0000") } returns NetworkResult.Failure()
        coEvery { repository.verifyOtp("+244912345678", "1234") } returns NetworkResult.Success(
            VerifiedSession("tok", "drv-1", "2026-12-01T00:00:00Z"),
        )
        val viewModel = viewModel()
        viewModel.sendOtp("912345678")
        advanceUntilIdle()
        viewModel.verifyOtp("0000")
        advanceUntilIdle()
        viewModel.state.test {
            assertEquals(
                AuthState.Error(AuthState.Error.Reason.RequestFailed, "+244912345678"),
                awaitItem(),
            )
            viewModel.verifyOtp("1234")
            assertEquals(AuthState.Verifying("+244912345678"), awaitItem())
            advanceUntilIdle()
            assertEquals(AuthState.Authenticated("drv-1"), awaitItem())
        }
    }

    @Test
    fun editPhone_returnsToIdle() = runTest(dispatcher) {
        coEvery { repository.sendOtp("+244912345678") } returns NetworkResult.Success(Unit)
        val viewModel = viewModel()
        viewModel.sendOtp("912345678")
        advanceUntilIdle()
        viewModel.state.test {
            assertEquals(AuthState.OtpSent("+244912345678"), awaitItem())
            viewModel.editPhone()
            assertEquals(AuthState.Idle, awaitItem())
        }
    }

    private fun viewModel() = AuthViewModel(repository, tokenStore)
}
