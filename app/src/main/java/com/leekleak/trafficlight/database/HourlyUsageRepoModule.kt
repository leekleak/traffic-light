package com.leekleak.trafficlight.database

import org.koin.dsl.module

val hourlyUsageRepoModule = module {single { HourlyUsageRepo(get()) }}
