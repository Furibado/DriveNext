package ru.mtuci.drivenext

import android.content.Context
import ru.mtuci.drivenext.data.AppStateRepositoryImpl
import ru.mtuci.drivenext.data.ConnectionRepositoryImpl
import ru.mtuci.drivenext.domain.CompleteOnboardingUseCase
import ru.mtuci.drivenext.domain.GetStartDestinationUseCase

class AppContainer(context: Context) {
    private val supabaseClient = ru.mtuci.drivenext.data.SupabaseClient(context)
    val profileRepository: ru.mtuci.drivenext.domain.ProfileRepository = ru.mtuci.drivenext.data.ProfileRepositoryImpl(context, supabaseClient)
    val carRepository: ru.mtuci.drivenext.domain.CarRepository = ru.mtuci.drivenext.data.CarRepositoryImpl(supabaseClient)
    val authRepository: ru.mtuci.drivenext.domain.AuthRepository = ru.mtuci.drivenext.data.RegistrationRepository(context, supabaseClient)
    private val connectionRepository = ConnectionRepositoryImpl(context)
    private val appStateRepository = AppStateRepositoryImpl(context)

    val getStartDestinationUseCase = GetStartDestinationUseCase(connectionRepository, appStateRepository)
    val completeOnboardingUseCase = CompleteOnboardingUseCase(appStateRepository)
}
