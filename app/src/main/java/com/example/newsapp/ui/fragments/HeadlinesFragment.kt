package com.example.newsapp.ui.fragments

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.newsapp.R
import com.example.newsapp.adapters.NewsAdapters
import com.example.newsapp.databinding.FragmentHeadlinesBinding
import com.example.newsapp.ui.NewsActivity
import com.example.newsapp.ui.NewsViewModel
import com.example.newsapp.util.Constants
import com.example.newsapp.util.Resources

class HeadlinesFragment : Fragment(R.layout.fragment_headlines) {

    lateinit var newsViewModel: NewsViewModel
    lateinit var newsAdapters: NewsAdapters
    lateinit var retryButton:Button
    lateinit var errorText:TextView
    lateinit var itemHeadlinesError:CardView
    lateinit var binding: FragmentHeadlinesBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding=FragmentHeadlinesBinding.bind(view)
        itemHeadlinesError=view.findViewById(R.id.itemHeadlinesError)
        val inflater=requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view:View=inflater.inflate(R.layout.item_error,null)

        retryButton=view.findViewById(R.id.retryButton)
        errorText=view.findViewById(R.id.errorText)

       newsViewModel = (activity as NewsActivity).newsViewModel
        setupHeadlinesRecyclerView()

        newsAdapters.setOnItemClickListener {
            val bundle=Bundle().apply {
                putSerializable("article",it)
            }
            findNavController().navigate(R.id.action_headlinesFragment_to_articleFragment,bundle)
        }
        newsViewModel.headlines.observe(viewLifecycleOwner, Observer { response->
            when(response){
               is Resources.Success<*> ->{
                   hideProgressBar()
                   hideErrorMessage()
                   response.data?.let { newsResponse ->
                       newsAdapters.differ.submitList(newsResponse.articles.toList())
                       val totalPages=newsResponse.totalResults/Constants.QUERY_PAGE_SIZE+2
                       isLastPage=newsViewModel.headlinesPage==totalPages
                       if (isLastPage)
                           binding.recyclerHeadlines.setPadding(0,0,0,0)
                   }
               }
                is Resources.Error<*> ->{
                    hideProgressBar()
                    response.message?.let { message->
                        Toast.makeText(activity,"An error occured:$message", Toast.LENGTH_LONG).show()
                        showErrorMessage(message)
                    }
                }
                is Resources.Loading<*> ->{
                    showProgressBar()
                }
            }
        })
        retryButton.setOnClickListener {
            newsViewModel.getHeadlines("us")
        }
    }

    var isError=false
    var isLoading=false
    var isLastPage=false
    var isScrolling=false

    private fun hideProgressBar(){
        binding.paginationProgressBar.visibility=View.INVISIBLE
        isLoading=false
    }
    private fun showProgressBar(){
        binding.paginationProgressBar.visibility=View.VISIBLE
        isLoading=true
    }

    private fun hideErrorMessage(){
        itemHeadlinesError.visibility=View.INVISIBLE
        isError=false
    }
    private fun showErrorMessage(message:String){
        itemHeadlinesError.visibility=View.VISIBLE
        errorText.text=message
        isError=true
    }

    val scrollListner=object :RecyclerView.OnScrollListener(){

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
           val layoutManager=recyclerView.layoutManager as LinearLayoutManager
            val firstvisibleItemPosition=layoutManager.findFirstVisibleItemPosition()
            val visibleItemCount=layoutManager.childCount
            val totalItemCount=layoutManager.itemCount

            val isNoErrors =!isError
            val isNotLoadingAndNotLastPage=!isLoading && !isLastPage
            val isAtLastItem=firstvisibleItemPosition+visibleItemCount >=totalItemCount
            val isNotAtBeginning=firstvisibleItemPosition >=0
            val isTotalMoreThanVisible=totalItemCount >=Constants.QUERY_PAGE_SIZE
            val shouldPaginate=isError && isNotLoadingAndNotLastPage && isAtLastItem && isNotAtBeginning && isTotalMoreThanVisible && isScrolling

            if(shouldPaginate){
                newsViewModel.getHeadlines("us")
                isScrolling=false
            }
        }

        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)

            if (newState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL){
                isScrolling=true
            }

        }
    }

        private fun setupHeadlinesRecyclerView() {
            newsAdapters = NewsAdapters()
            binding.recyclerHeadlines.apply {
                adapter=newsAdapters
                layoutManager=LinearLayoutManager(activity)
                addOnScrollListener(this@HeadlinesFragment.scrollListner)
            }

            }
        }


