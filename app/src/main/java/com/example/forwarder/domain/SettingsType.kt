package com.example.forwarder.domain

/**
 * @brief Enum representing different types of settings in the application.
 */
enum class SettingsType : java.io.Serializable {
    /**
     * Uses AndroidX's PreferenceFragmentCompat for managing settings.
     * This approach is chosen because PreferenceFragmentCompat is optimized for
     * simple key-value storage and provides a built-in UI for preferences, making
     * it ideal for straightforward settings like user interface preferences.
     */
    INTERFACE,

    /**
     * Implements MVVM (Model-View-ViewModel) with DataBinding.
     * This is used because API settings may involve more complex interactions,
     * such as dynamic validation, user input, or real-time updates, which are
     * better handled with the flexibility and separation of concerns provided by
     * MVVM and DataBinding.
     */
    API
}

