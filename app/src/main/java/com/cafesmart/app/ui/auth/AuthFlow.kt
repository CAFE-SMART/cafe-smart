
package com.cafesmart.app.ui.auth

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun AuthFlow(
    onLogin: (LoginCredentials) -> Unit,
    onRegister: (RegistrationData) -> Unit,
    onGoogle: () -> Unit,
    onForgotPassword: () -> Unit,
    onHelp: (AuthHelpContext) -> Unit,
    onSupport: () -> Unit,
    onExit: () -> Unit,
    isSubmitting: Boolean = false,
    viewModel: AuthViewModel = viewModel()
) {

    val state = viewModel.uiState

    when (state.page) {

        AuthPage.LOGIN -> {

            LoginScreen(
                state = state,
                onChange = viewModel::update,

                onSubmit = {
                    viewModel.submitLogin(onLogin)
                },

                onRegister = viewModel::goToRegister,
                onGoogle = onGoogle,
                onForgotPassword = onForgotPassword,

                onHelp = {
                    onHelp(AuthHelpContext.LOGIN)
                },

                onSupport = onSupport,
                onExit = onExit,
                isSubmitting = isSubmitting
            )
        }

        AuthPage.REGISTER_STEP_1 -> {

            RegisterStep1Screen(
                state = state,
                onChange = viewModel::update,
                onBack = viewModel::back,
                onContinue = viewModel::nextStep,

                onHelp = {
                    onHelp(
                        AuthHelpContext.REGISTER_STEP_1
                    )
                },

                onSupport = onSupport
            )
        }

        AuthPage.REGISTER_STEP_2 -> {

            RegisterStep2Screen(
                state = state,
                onChange = viewModel::update,
                onBack = viewModel::back,

                onCreateAccount = {
                    viewModel.submitRegistration(
                        onRegister
                    )
                },

                onGoogle = onGoogle,

                onHelp = {
                    onHelp(
                        AuthHelpContext.REGISTER_STEP_2
                    )
                },

                onSupport = onSupport,
                isSubmitting = isSubmitting
            )
        }
    }
}
