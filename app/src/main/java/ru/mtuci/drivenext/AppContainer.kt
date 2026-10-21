package ru.mtuci.drivenext

import android.content.Context
import ru.mtuci.drivenext.data.AppStateRepositoryImpl
import ru.mtuci.drivenext.data.ConnectionRepositoryImpl
import ru.mtuci.drivenext.domain.CompleteOnboardingUseCase
import ru.mtuci.drivenext.domain.GetStartDestinationUseCase

class AppContainer(context: Context) {
    private val connectionRepository = ConnectionRepositoryImpl(context)
    private val appStateRepository = AppStateRepositoryImpl(context)

    val getStartDestinationUseCase = GetStartDestinationUseCase(connectionRepository, appStateRepository)
    val completeOnboardingUseCase = CompleteOnboardingUseCase(appStateRepository)
}