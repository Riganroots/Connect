package com.example.ui.viewmodel

internal fun validateActivityForm(
    title: String, location: String, date: String, time: String, price: String,
    participants: String, description: String
): String? = when {
    title.isBlank() || location.isBlank() || date.isBlank() || description.isBlank() ->
        "Please add a title, meeting point, date and short description."
    title.length > 200 -> "Keep the title within 200 characters."
    location.length > 200 -> "Keep the meeting point within 200 characters."
    date.length > 80 || time.length > 80 -> "Keep the date and time within 80 characters each."
    price.length > 80 -> "Keep the cost within 80 characters."
    description.length > 5000 -> "Keep the description within 5,000 characters."
    participants.toIntOrNull()?.let { it in 1..10000 } != true ->
        "Enter a number of people from 1 to 10,000."
    else -> null
}
