package com.tostrr.data

/** Told by [OrderRepository] after each successful sync. */
fun interface OrderRefreshListener {
    fun onOrdersRefreshed()
}
