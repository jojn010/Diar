package com.example.diar

/** Explicit opt-in tests that contact the configured Supabase project. */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class LiveProjectTest
