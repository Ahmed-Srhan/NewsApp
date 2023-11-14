package com.srhan.newsapp.repository

import androidx.lifecycle.LiveData
import com.srhan.newsapp.models.Article
import com.srhan.newsapp.models.NewsResponse
import retrofit2.Response

interface NewsRepo {
    suspend fun getBreakingNews(countryCode: String, breakingNewsPage: Int): Response<NewsResponse>
    suspend fun searchNews(searchQuery: String, searchPage: Int): Response<NewsResponse>

    //Room
    suspend fun insertArticle(article: Article)
    fun getAllArticle(): LiveData<List<Article>>
    suspend fun deleteArticle(article: Article)

}