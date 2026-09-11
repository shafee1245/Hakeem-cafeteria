package com.example.data.remote

import com.example.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.functions.Functions

object SupabaseService {
    val client = createSupabaseClient(
        supabaseUrl = BuildConfig.NEXT_PUBLIC_SUPABASE_URL,
        supabaseKey = BuildConfig.NEXT_PUBLIC_SUPABASE_ANON_KEY
    ) {
        install(Postgrest)
        install(Storage)
        install(Auth)
        install(Functions)
    }
}
