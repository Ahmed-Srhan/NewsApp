package com.srhan.newsapp.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.srhan.newsapp.R
import com.srhan.newsapp.adapters.NewsAdapter
import com.srhan.newsapp.databinding.FragmentSearchNewsBinding
import com.srhan.newsapp.models.Article
import com.srhan.newsapp.ui.MainActivity
import com.srhan.newsapp.util.Constants.Companion.PAGE_SIZE
import com.srhan.newsapp.util.Constants.Companion.SEARCH_NEWS_TIME_DELAY
import com.srhan.newsapp.util.Resource
import com.srhan.newsapp.viewmodel.NewsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint

class SearchNewsFragment : Fragment(), NewsAdapter.NewsOnItemClickListener {


    private lateinit var binding: FragmentSearchNewsBinding
    private lateinit var newsAdapter: NewsAdapter
    lateinit var newsViewModel: NewsViewModel
    lateinit var textSearch: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        newsViewModel = (activity as MainActivity).viewModel
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentSearchNewsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpRecyclerView()
        var job: Job? = null
        binding.SearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                job?.cancel()
                job = MainScope().launch {
                    delay(SEARCH_NEWS_TIME_DELAY)
                    newText?.let {
                        if (newText.toString().isNotEmpty()) {
                            newsViewModel.searchForNews(newText.toString())
                            textSearch = newText.toString()
                        } else {
                            newsAdapter.differ.submitList(emptyList())

                        }
                    }
                }
                return true
            }
        })


        lifecycleScope.launch {
            newsViewModel.searchNews.collect { response ->
                showProgressBar()
                when (response) {
                    is Resource.Success -> {
                        hideProgressBar()
                        response.data?.let { newResponse ->
                            newsAdapter.differ.submitList(newResponse.articles.toList())
                            val totalPage = newResponse.totalResults / PAGE_SIZE + 2
                            isLastPage = totalPage == newsViewModel.searchNewsPage
                            binding.rvSearchNews.setPadding(0, 0, 0, 0)
                        }
                    }

                    is Resource.Error -> {
                        hideProgressBar()
                        response.let { message ->
                            Toast.makeText(
                                activity,
                                "On Error occurred : ${message.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    is Resource.Loading -> {
                        showProgressBar()
                    }

                    else -> {

                    }
                }

            }
        }

    }


    private fun showProgressBar() {
        binding.paginationProgressBar.visibility = View.VISIBLE
        isLoading = true
    }

    private fun hideProgressBar() {
        binding.paginationProgressBar.visibility = View.GONE
        isLoading = false
    }

    var isLoading = false
    var isLastPage = false
    var isScrolling = false
    private val scrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            if (newState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                isScrolling = true
            }
        }

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            val layoutManager = recyclerView.layoutManager as LinearLayoutManager
            val visibleItemCount = layoutManager.childCount
            val totalItemCount = layoutManager.itemCount
            val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

            val isNotLoadingAndNotLastPage = !isLoading && !isLastPage
            val isAtLastItem = firstVisibleItemPosition + visibleItemCount >= totalItemCount
            val isNotAtBeginning = firstVisibleItemPosition >= 0
            val isTotalMoreThanVisible = totalItemCount >= PAGE_SIZE

            val shouldPaginate =
                isNotLoadingAndNotLastPage && isAtLastItem && isNotAtBeginning && isTotalMoreThanVisible && isScrolling
            if (shouldPaginate) {
                newsViewModel.searchForNews(textSearch)
                isScrolling = false
            }
        }
    }


    private fun setUpRecyclerView() {
        newsAdapter = NewsAdapter()
        newsAdapter.setNewsOnItemClickListener(this)
        binding.rvSearchNews.apply {
            adapter = newsAdapter
            layoutManager = LinearLayoutManager(activity)
            addOnScrollListener(this@SearchNewsFragment.scrollListener)
        }
    }


    override fun onItemClickListener(article: Article) {
        val bundle = Bundle().apply {
            putSerializable("article", article)
        }
        findNavController().navigate(
            R.id.action_searchNewsFragment_to_articleFragment,
            bundle
        )
    }


}