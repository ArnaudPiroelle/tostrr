package com.tostrr.domain.checklist.usecase

import com.tostrr.domain.checklist.ChecklistRepository
import org.koin.core.annotation.Factory

/** Removes a photo that was taken but not kept. */
@Factory
class DeleteChecklistPhotoUseCase(private val repository: ChecklistRepository) {
    suspend operator fun invoke(path: String) = repository.deletePhoto(path)
}
