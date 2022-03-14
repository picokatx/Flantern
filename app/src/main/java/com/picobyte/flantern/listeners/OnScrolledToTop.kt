package com.picobyte.flantern.listeners

import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager

//https://gist.github.com/ssinss/e06f12ef66c51252563e
abstract class OnScrolledToTop(
    linearLayoutManager: LinearLayoutManager,
    var prevEntryNamespace: String
) :
    RecyclerView.OnScrollListener() {
    private var previousTotal = 0 // The total number of items in the dataset after the last load
    private var loading = true // True if we are still waiting for the last set of data to load.
    private var currentPage: Int = 0
    private val visibleThreshold =
        5 // The minimum amount of items to have below your current scroll position before loading more.
    private var firstVisibleItem = 0
    private var visibleItemCount = 0
    var totalItemCount = 0
    private val mLinearLayoutManager: LinearLayoutManager = linearLayoutManager
    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)
        visibleItemCount = recyclerView.childCount
        totalItemCount = mLinearLayoutManager.itemCount
        firstVisibleItem = mLinearLayoutManager.findFirstVisibleItemPosition()
        if (loading) {
            if (totalItemCount > previousTotal) {
                loading = false
                previousTotal = totalItemCount
            }
        }
        Log.e("Flantern",firstVisibleItem.toString())
        if (!loading && firstVisibleItem == 0) {
            currentPage++
            onLoadMore(currentPage)
            loading = true
        }
    }

    abstract fun onLoadMore(current_page: Int)
}
