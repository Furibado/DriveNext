package ru.mtuci.drivenext.domain

class CompleteOnboardingUseCase(
    private val appStateRepository: AppStateRepository
) {
    operator fun invoke() = appStateRepository.setOnboardingCompleted()
}