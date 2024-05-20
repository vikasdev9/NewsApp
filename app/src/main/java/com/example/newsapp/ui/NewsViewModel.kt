package com.example.newsapp.ui

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.newsapp.models.Article
import com.example.newsapp.models.NewsResponse
import com.example.newsapp.repository.NewsRepository
import com.example.newsapp.util.Resources
import kotlinx.coroutines.launch
import retrofit2.Response
import java.io.IOException

class NewsViewModel(app:Application,val newsRepository: NewsRepository):AndroidViewModel(app) {

    val headlines:MutableLiveData<Resources<NewsResponse>> = MutableLiveData()
    var headlinesPage = 1
    var headlinesResponse:NewsResponse? = null

    val searchNews:MutableLiveData<Resources<NewsResponse>> = MutableLiveData()
    var searchNewsPage = 1
    var searchNewsResponse:NewsResponse? = null
    var newSearchQuery:String? = null
    var oldSearchQuery:String? = null

    init {
        getHeadlines("us")
    }

    fun getHeadlines(countryCode:String) = viewModelScope.launch {
        headlineInternet(countryCode)
    }

    fun searchNews(searchQuery:String) = viewModelScope.launch {
        searchNewsInternet(searchQuery)
    }

    private fun handleHeadlinesResponse(response:Response<NewsResponse>):Resources<NewsResponse>{
        if (response.isSuccessful){
            response.body()?.let { resultResponse ->
                headlinesPage++
                if (headlinesResponse == null){
                    headlinesResponse = resultResponse
                }else{
                    val oldArticles = headlinesResponse?.articles
                    val newArticles = resultResponse.articles
                    oldArticles?.addAll(newArticles)
            }
                return Resources.Success(headlinesResponse ?: resultResponse)
            }

        }
        return Resources.Error(response.message())
    }

    private fun handleSearchNewsResponse(response:Response<NewsResponse>):Resources<NewsResponse> {
        if (response.isSuccessful) {
            response.body()?.let { resultResponse ->
                searchNewsPage++
                if (searchNewsResponse == null || newSearchQuery != oldSearchQuery) {
                    searchNewsPage = 1
                    oldSearchQuery = newSearchQuery
                    searchNewsResponse = resultResponse
                } else {
                    searchNewsPage++
                    val oldArticles = headlinesResponse?.articles
                    val newArticles = resultResponse.articles
                    oldArticles?.addAll(newArticles)
                }
                return Resources.Success(headlinesResponse ?: resultResponse)
            }
        }

        return Resources.Error(response.message())
    }
    fun addToFavourites(article: Article) = viewModelScope.launch {
        newsRepository.upsert(article)
    }
    fun getFavourites() = newsRepository.getFavouriteNews()

    fun deleteArticle(article: Article) = viewModelScope.launch {
        newsRepository.deleteArticle(article)
    }

    fun internetConnection(context: Context):Boolean{
        (context.getSystemService(Context.CONNECTIVITY_SERVICE)as ConnectivityManager).apply {
            return getNetworkCapabilities(activeNetwork)?.run {
                when{
                    hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                    hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                    hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                    else -> false
                }
            } ?:false
        }
    }
    private suspend fun headlineInternet(countryCode:String){
        headlines.postValue(Resources.Loading())
        try {
            if (internetConnection(this.getApplication())) {
                val response = newsRepository.getHeadlines(countryCode, headlinesPage)
                headlines.postValue(handleHeadlinesResponse(response))
            }else{
                headlines.postValue(Resources.Error("No Internet Connection"))

            }
        }catch (t:Throwable){
            when(t){
                is IOException -> headlines.postValue(Resources.Error("Unable To Network Failure"))
                else -> headlines.postValue(Resources.Error("No Signal"))
            }
        }
    }

private suspend fun searchNewsInternet(searchQuery:String){
    newSearchQuery = searchQuery
    searchNews.postValue(Resources.Loading())
    try {
        if (internetConnection(this.getApplication())) {
            val response = newsRepository.searchNews(searchQuery, searchNewsPage)
            searchNews.postValue(handleSearchNewsResponse(response))
        } else {
            searchNews.postValue(Resources.Error("No Internet Connection"))
        }
    } catch (t:Throwable) {
        when(t){
            is IOException -> searchNews.postValue(Resources.Error("Unable To Network Failure"))
            else -> searchNews.postValue(Resources.Error("No Signal"))
        }
    }
}

}