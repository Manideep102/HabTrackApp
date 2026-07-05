package com.example.habtrack.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModel
import com.example.habtrack.data.HabitRepository

/**
 * Factory for creating HabitViewModel instances with the required HabitRepository.
 * This factory resolves the dependency injection issue by providing the repository
 * when the ViewModel is instantiated.
 */
class HabitViewModelFactory(private val repository: HabitRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HabitViewModel::class.java)) {
            return HabitViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
