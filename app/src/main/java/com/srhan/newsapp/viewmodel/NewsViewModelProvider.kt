package com.srhan.newsapp.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.srhan.newsapp.repository.NewsRepo
import javax.inject.Inject

class NewsViewModelProvider @Inject constructor(
    private val app: Application,
    private val newsRepo: NewsRepo
) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        if (modelClass.isAssignableFrom(NewsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NewsViewModel(app, newsRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}