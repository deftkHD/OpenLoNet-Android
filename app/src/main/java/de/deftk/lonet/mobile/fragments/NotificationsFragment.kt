package de.deftk.lonet.mobile.fragments

import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import de.deftk.lonet.api.model.feature.board.BoardNotification
import de.deftk.lonet.mobile.AuthStore
import de.deftk.lonet.mobile.R
import de.deftk.lonet.mobile.abstract.FeatureFragment
import de.deftk.lonet.mobile.activities.feature.NotificationActivity
import de.deftk.lonet.mobile.adapter.NotificationAdapter
import de.deftk.lonet.mobile.feature.AppFeature
import kotlinx.android.synthetic.main.fragment_notifications.*

class NotificationsFragment: FeatureFragment(AppFeature.FEATURE_NOTIFICATIONS) {

    //TODO filters

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        NotificationLoader().execute()

        val view = inflater.inflate(R.layout.fragment_notifications, container, false)
        val list = view.findViewById<ListView>(R.id.notification_list)
        val swipeRefresh = view.findViewById<SwipeRefreshLayout>(R.id.notifications_swipe_refresh)
        swipeRefresh.setOnRefreshListener {
            list.adapter = null
            NotificationLoader().execute()
        }
        list.setOnItemClickListener { _, _, position, _ ->
            val intent = Intent(context, NotificationActivity::class.java)
            intent.putExtra(NotificationActivity.EXTRA_NOTIFICATION, list.getItemAtPosition(position) as BoardNotification)
            startActivity(intent)
        }
        return view
    }

    private inner class NotificationLoader: AsyncTask<Boolean, Void, List<BoardNotification>?>() {

        override fun doInBackground(vararg params: Boolean?): List<BoardNotification>? {
            return try {
                AuthStore.appUser.getAllBoardNotifications().sortedByDescending { it.creationDate.time }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        override fun onPostExecute(result: List<BoardNotification>?) {
            progress_notifications?.visibility = ProgressBar.INVISIBLE
            notifications_swipe_refresh?.isRefreshing = false
            if (context != null) {
                if (result != null) {
                    notification_list?.adapter = NotificationAdapter(context!!, result)
                    notifications_empty?.isVisible = result.isEmpty()
                } else {
                    Toast.makeText(context, getString(R.string.request_failed_other).format("No details"), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

}