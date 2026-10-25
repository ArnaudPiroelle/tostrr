package com.tostrr.domain.checklist.usecase

import com.tostrr.domain.checklist.ChecklistRepository
import org.koin.core.annotation.Factory
import java.io.File

/** A fresh file to take the photo of a question into. */
@Factory
class CreateChecklistPhotoFileUseCase(private val repository: ChecklistRepository) {
    operator fun invoke(ref: String, questionId: String): File = repository.newPhotoFile(ref, questionId)
}
