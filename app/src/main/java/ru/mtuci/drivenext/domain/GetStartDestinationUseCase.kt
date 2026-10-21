package ru.mtuci.drivenext.domain

class GetStartDestinationUseCase(
    private val connectionRepository: ConnectionRepository,
    private val appStateRepository: AppStateRepository
) {
    operator fun invoke(): StartDestination = when {
        !connectionRepository.isInternetAvailable() -> StartDestination.NO_CONNECTION
        !appStateRepository.isOnboardingCompleted() -> StartDestination.ONBOARDING
        else -> StartDestination.LOGIN
    }
}