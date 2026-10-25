package com.tostrr.domain.di

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

/** Koin module of the domain layer: its use cases. The repositories they use are implemented by the data layer. */
@Module
@ComponentScan("com.tostrr.domain")
class DomainModule
