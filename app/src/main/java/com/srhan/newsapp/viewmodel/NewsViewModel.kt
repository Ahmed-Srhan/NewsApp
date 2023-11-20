package com.srhan.newsapp.viewmodel

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.ConnectivityManager.TYPE_ETHERNET
import android.net.ConnectivityManager.TYPE_WIFI
import android.net.NetworkCapabilities.TRANSPORT_CELLULAR
import android.net.NetworkCapabilities.TRANSPORT_ETHERNET
import android.net.NetworkCapabilities.TRANSPORT_WIFI
import android.os.Build
import android.provider.ContactsContract.CommonDataKinds.Email.TYPE_MOBILE
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.srhan.newsapp.NewsApp
import com.srhan.newsapp.db.NewsDao
import com.srhan.newsapp.models.Article
import com.srhan.newsapp.models.NewsResponse
import com.srhan.newsapp.remote.NewsApiService
import com.srhan.newsapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class NewsViewModel @Inject constructor(
    app: Application,
    private val apiService: NewsApiService,
    private val newsDao: NewsDao
) : AndroidViewModel(app) {

    private val _breakingNews: MutableStateFlow<Resource<NewsResponse>?> = MutableStateFlow(null)
    val breakingNews: Flow<Resource<NewsResponse>?> = _breakingNews
    private val _searchNews: MutableStateFlow<Resource<NewsResponse>?> = MutableStateFlow(null)
    val searchNews: Flow<Resource<NewsResponse>?> = _searchNews
    var breakingNewsPage = 1
    var searchNewsPage = 1
    var breakingNewsResponse: NewsResponse? = null
    var searchNewsResponse: NewsResponse? = null


    init {
        getBreakingNews("us")
    }

    fun getBreakingNews(countryCode: String) = viewModelScope.launch {

        safeBreakingNewsCall(countryCode)
    }

    fun searchForNews(searchQuery: String) = viewModelScope.launch {
        safeSearchNewsCall(searchQuery)
    }


    private fun handelSearchNewsResponse(response: Response<NewsResponse>): Resource<NewsResponse>? {
        if (response.isSuccessful) {
            response.body()?.let { resultResponse ->
                searchNewsPage++
                if (searchNewsResponse == null) {
                    searchNewsResponse = resultResponse
                } else {
                    val oldArticle = searchNewsResponse?.articles
                    val newArticle = resultResponse.articles
                    oldArticle?.addAll(newArticle)
                }
                return Resource.Success(searchNewsResponse ?: resultResponse)
            }
        }
        return Resource.Error(response.message())
    }


    private fun handelBreakingNewsResponse(response: Response<NewsResponse>): Resource<NewsResponse> {
        if (response.isSuccessful) {
            response.body()?.let { resultResponse ->
                breakingNewsPage++
                if (breakingNewsResponse == null) {
                    breakingNewsResponse = resultResponse
                } else {
                    val oldArticle = breakingNewsResponse?.articles
                    val newArticle = resultResponse.articles
                    oldArticle?.addAll(newArticle)
                }
                return Resource.Success(breakingNewsResponse ?: resultResponse)
            }
        }
        return Resource.Error(response.message())

    }

    fun insertArticle(article: Article) = viewModelScope.launch {
        newsDao.insertArticle(article)
    }

    fun deleteArticle(article: Article) = viewModelScope.launch {
        newsDao.deleteArticle(article)
    }

    fun getAllArticle() =
        newsDao.getAllArticle()

    private suspend fun safeBreakingNewsCall(countryCode: String) {
        _breakingNews.value = (Resource.Loading())

        try {
            if (hasInternetConnection()) {
                val response = apiService.getBreakingNews(countryCode, breakingNewsPage)
                _breakingNews.value = handelBreakingNewsResponse(response)
            } else {
                _breakingNews.value = Resource.Error("No internet connection")
            }
        } catch (t: Throwable) {
            when (t) {
                is IOException -> _breakingNews.value = Resource.Error("Network Failure")
                else -> _breakingNews.value = Resource.Error("Conversion Error")
            }
        }
    }

    private suspend fun safeSearchNewsCall(searchQuery: String) {
        _searchNews.value = (Resource.Loading())

        try {
            if (hasInternetConnection()) {
                val response = apiService.searchForNews(searchQuery, searchNewsPage)
                _searchNews.value = handelSearchNewsResponse(response)
            } else {
                _searchNews.value = Resource.Error("No internet connection")
            }
        } catch (t: Throwable) {
            when (t) {
                is IOException -> _searchNews.value = Resource.Error("Network Failure")
                else -> _searchNews.value = Resource.Error("Conversion Error")
            }
        }
    }

    private fun hasInternetConnection(): Boolean {
        val connectivityManager = getApplication<NewsApp>().getSystemService(
            Context.CONNECTIVITY_SERVICE
        ) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val activeNetwork = connectivityManager.activeNetwork ?: return false
            val capabilities =
                connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false

            return when {
                capabilities.hasTransport(TRANSPORT_WIFI) -> true
                capabilities.hasTransport(TRANSPORT_CELLULAR) -> true
                capabilities.hasTransport(TRANSPORT_ETHERNET) -> true
                else -> false
            }
        } else {
            connectivityManager.activeNetworkInfo?.run {
                return when (type) {
                    TYPE_WIFI -> true
                    TYPE_MOBILE -> true
                    TYPE_ETHERNET -> true
                    else -> false
                }
            }
        }
        return false
    }


}