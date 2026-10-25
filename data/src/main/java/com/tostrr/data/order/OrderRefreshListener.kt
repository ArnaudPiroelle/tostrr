package com.tostrr.data.order

/** Told by [OrderRepository] after each successful sync. */
fun interface OrderRefreshListener {
    fun onOrdersRefreshed()
}
