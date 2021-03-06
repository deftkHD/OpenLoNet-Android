package de.deftk.openww.android.fragments.feature.board

import android.os.Bundle
import android.view.*
import android.widget.ProgressBar
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import de.deftk.openww.api.model.Permission
import de.deftk.openww.android.R
import de.deftk.openww.android.adapter.recycler.BoardNotificationAdapter
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.components.ContextMenuRecyclerView
import de.deftk.openww.android.databinding.FragmentNotificationsBinding
import de.deftk.openww.android.utils.Reporter
import de.deftk.openww.android.viewmodel.BoardViewModel
import de.deftk.openww.android.viewmodel.UserViewModel

class NotificationsFragment: Fragment() {

    private val userViewModel: UserViewModel by activityViewModels()
    private val boardViewModel: BoardViewModel by activityViewModels()
    private val navController by lazy { findNavController() }
    private lateinit var binding: FragmentNotificationsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        (requireActivity() as AppCompatActivity).supportActionBar?.show()
        context ?: return binding.root

        val adapter = BoardNotificationAdapter()
        binding.notificationList.adapter = adapter
        binding.notificationList.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        boardViewModel.notificationsResponse.observe(viewLifecycleOwner) { response ->
            if (response is Response.Success) {
                adapter.submitList(response.value)
                binding.notificationsEmpty.isVisible = response.value.isEmpty()
            } else if (response is Response.Failure) {
                Reporter.reportException(R.string.error_get_notifications_failed, response.exception, requireContext())
                binding.notificationsEmpty.isVisible = false
            }
            binding.progressNotifications.visibility = ProgressBar.INVISIBLE
            binding.notificationsSwipeRefresh.isRefreshing = false
        }

        binding.notificationsSwipeRefresh.setOnRefreshListener {
            userViewModel.apiContext.value?.also { apiContext ->
                boardViewModel.loadBoardNotifications(apiContext)
            }
        }

        binding.fabAddNotification.setOnClickListener {
            val action = NotificationsFragmentDirections.actionNotificationsFragmentToEditNotificationFragment(null, null, getString(R.string.new_notification))
            navController.navigate(action)
        }

        userViewModel.apiContext.observe(viewLifecycleOwner) { apiContext ->
            if (apiContext != null) {
                boardViewModel.loadBoardNotifications(apiContext)
                binding.fabAddNotification.isVisible = apiContext.user.getGroups().any { it.effectiveRights.contains(Permission.BOARD_WRITE) || it.effectiveRights.contains(Permission.BOARD_ADMIN) }
            } else {
                binding.fabAddNotification.isVisible = false
                binding.notificationsEmpty.isVisible = false
                adapter.submitList(emptyList())
                binding.progressNotifications.isVisible = true
            }
        }

        boardViewModel.postResponse.observe(viewLifecycleOwner) { response ->
            if (response != null)
                boardViewModel.resetPostResponse() // mark as handled

            if (response is Response.Failure) {
                Reporter.reportException(R.string.error_delete_failed, response.exception, requireContext())
            }
        }

        setHasOptionsMenu(true)
        registerForContextMenu(binding.notificationList)
        return binding.root
    }

    /*override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.list_filter_menu, menu)
        val searchItem = menu.findItem(R.id.filter_item_search)
        val searchView = searchItem.actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchView.clearFocus()
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                boardViewModel.setSearchText(newText)
                return false
            }
        })
        super.onCreateOptionsMenu(menu, inflater)
    }*/

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        if (menuInfo is ContextMenuRecyclerView.RecyclerViewContextMenuInfo) {
            val (_, group) = (binding.notificationList.adapter as BoardNotificationAdapter).getItem(menuInfo.position)
            if (group.effectiveRights.contains(Permission.BOARD_WRITE) || group.effectiveRights.contains(Permission.BOARD_ADMIN)) {
                requireActivity().menuInflater.inflate(R.menu.simple_edit_item_menu, menu)
            }
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val menuInfo = item.menuInfo as ContextMenuRecyclerView.RecyclerViewContextMenuInfo
        val adapter = binding.notificationList.adapter as BoardNotificationAdapter
        return when (item.itemId) {
            R.id.menu_item_edit -> {
                val (notification, group) = adapter.getItem(menuInfo.position)
                val action = NotificationsFragmentDirections.actionNotificationsFragmentToEditNotificationFragment(notification.id, group.login, getString(R.string.edit_notification))
                navController.navigate(action)
                true
            }
            R.id.menu_item_delete -> {
                val (notification, group) = adapter.getItem(menuInfo.position)
                val apiContext = userViewModel.apiContext.value ?: return false
                boardViewModel.deleteBoardNotification(notification, group, apiContext)
                true
            }
            else -> false
        }
    }

}