package com.srhan.newsapp.repository

import androidx.lifecycle.LiveData
import com.srhan.newsapp.db.NewsDao
import com.srhan.newsapp.models.Article
import com.srhan.newsapp.models.NewsResponse
import com.srhan.newsapp.remote.NewsApiService
import retrofit2.Response

class NewsRepoImp constructor(
    private val newsApiService: NewsApiService,
    private val newsDao: NewsDao
) : NewsRepo {

    override suspend fun getBreakingNews(
        countryCode: String,
        breakingNewsPage: Int
    ): Response<NewsResponse> =
        newsApiService.getBreakingNews(countryCode, breakingNewsPage)

    override suspend fun searchNews(searchQuery: String, searchPage: Int): Response<NewsResponse> =
        newsApiService.searchForNews(searchQuery, searchPage)


    override suspend fun insertArticle(article: Article) = newsDao.insertArticle(article)

    override fun getAllArticle(): LiveData<List<Article>> = newsDao.getAllArticle()
    override suspend fun deleteArticle(article: Article) = newsDao.deleteArticle(article)
}