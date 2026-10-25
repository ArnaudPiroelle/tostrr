package com.tostrr.domain.sync.usecase

import com.tostrr.domain.auth.AuthRepository
import com.tostrr.domain.sync.SyncScheduler
import org.koin.core.annotation.Factory

/** Makes sure the background sync is scheduled while logged in; called at startup. */
@Factory
class StartBackgroundSyncUseCase(
    private val auth: AuthRepository,
    private val scheduler: SyncScheduler,
) {
    operator fun invoke() {
        if (auth.isLoggedIn()) scheduler.schedule()
    }
}
