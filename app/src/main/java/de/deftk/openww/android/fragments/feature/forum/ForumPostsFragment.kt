package de.deftk.openww.android.fragments.feature.forum

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import de.deftk.openww.android.R
import de.deftk.openww.android.adapter.recycler.ActionModeAdapter
import de.deftk.openww.android.adapter.recycler.ForumPostAdapter
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.components.ContextMenuRecyclerView
import de.deftk.openww.android.databinding.FragmentForumPostsBinding
import de.deftk.openww.android.fragments.ActionModeFragment
import de.deftk.openww.android.utils.Reporter
import de.deftk.openww.android.viewmodel.ForumViewModel
import de.deftk.openww.android.viewmodel.UserViewModel
import de.deftk.openww.api.model.IGroup
import de.deftk.openww.api.model.Permission
import de.deftk.openww.api.model.feature.forum.IForumPost

class ForumPostsFragment : ActionModeFragment<IForumPost, ForumPostAdapter.ForumPostViewHolder>(R.menu.forum_actionmode_menu) {

    //TODO context menu

    private val args: ForumPostsFragmentArgs by navArgs()
    private val userViewModel: UserViewModel by activityViewModels()
    private val forumViewModel: ForumViewModel by activityViewModels()
    private val navController by lazy { findNavController() }

    private lateinit var binding: FragmentForumPostsBinding
    private lateinit var group: IGroup

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentForumPostsBinding.inflate(inflater, container, false)
        (requireActivity() as AppCompatActivity).supportActionBar?.show()
        val group = userViewModel.apiContext.value?.user?.getGroups()?.firstOrNull { it.login == args.groupId }
        if (group == null) {
            Reporter.reportException(R.string.error_scope_not_found, args.groupId, requireContext())
            navController.popBackStack(R.id.forumGroupFragment, false)
            return binding.root
        }
        this.group = group

        binding.forumList.adapter = adapter
        binding.forumList.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        forumViewModel.getForumPosts(group).observe(viewLifecycleOwner) { response ->
            if (response is Response.Success) {
                val posts = forumViewModel.filterRootPosts(response.value)
                adapter.submitList(posts)
                binding.forumEmpty.isVisible = posts.isEmpty()
            } else if (response is Response.Failure) {
                Reporter.reportException(R.string.error_get_posts_failed, response.exception, requireContext())
            }
            binding.progressForum.isVisible = false
            binding.forumSwipeRefresh.isRefreshing = false
        }

        binding.forumSwipeRefresh.setOnRefreshListener {
            userViewModel.apiContext.value?.also { apiContext ->
                forumViewModel.loadForumPosts(group, null, apiContext)
            }
        }

        forumViewModel.deleteResponse.observe(viewLifecycleOwner) { response ->
            if (response != null)
                forumViewModel.resetDeleteResponse() // mark as handled

            if (response is Response.Failure) {
                Reporter.reportException(R.string.error_delete_failed, response.exception, requireContext())
            }
        }

        forumViewModel.batchDeleteResponse.observe(viewLifecycleOwner) { response ->
            if (response != null)
                forumViewModel.resetBatchDeleteResponse()

            val failure = response?.filterIsInstance<Response.Failure>() ?: return@observe
            if (failure.isNotEmpty()) {
                Reporter.reportException(R.string.error_delete_failed, failure.first().exception, requireContext())
                binding.progressForum.isVisible = false
            } else {
                actionMode?.finish()
            }
        }

        userViewModel.apiContext.observe(viewLifecycleOwner) { apiContext ->
            if (apiContext != null) {
                val newGroup = userViewModel.apiContext.value?.user?.getGroups()?.firstOrNull { it.login == args.groupId }
                if (newGroup != null) {
                    forumViewModel.loadForumPosts(group, null, apiContext)
                } else {
                    navController.popBackStack(R.id.forumGroupFragment, false)
                }
            } else {
                binding.forumEmpty.isVisible = false
                adapter.submitList(emptyList())
                binding.progressForum.isVisible = true
            }
        }

        registerForContextMenu(binding.forumList)
        return binding.root
    }

    override fun createAdapter(): ActionModeAdapter<IForumPost, ForumPostAdapter.ForumPostViewHolder> {
        return ForumPostAdapter(group, this)
    }

    override fun onItemClick(view: View, viewHolder: ForumPostAdapter.ForumPostViewHolder) {
        navController.navigate(ForumPostsFragmentDirections.actionForumPostsFragmentToForumPostFragment(viewHolder.binding.group!!.login, viewHolder.binding.post!!.id, null, getString(R.string.see_post)))
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.forum_action_delete -> {
                userViewModel.apiContext.value?.also { apiContext ->
                    forumViewModel.batchDelete(adapter.selectedItems.map { it.binding.post!! }, group, apiContext)
                }
            }
            else -> return false
        }
        return true
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        if (group.effectiveRights.contains(Permission.FORUM_WRITE) || group.effectiveRights.contains(Permission.FORUM_ADMIN)) {
            requireActivity().menuInflater.inflate(R.menu.forum_post_options_menu, menu)
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val menuInfo = item.menuInfo as ContextMenuRecyclerView.RecyclerViewContextMenuInfo
        val adapter = binding.forumList.adapter as ForumPostAdapter
        when (item.itemId) {
            R.id.menu_item_delete -> {
                val comment = adapter.getItem(menuInfo.position)
                userViewModel.apiContext.value?.also { apiContext ->
                    forumViewModel.deletePost(comment, null, group, apiContext)
                }
            }
            else -> super.onContextItemSelected(item)
        }
        return true
    }

    /*override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.list_filter_menu, menu)
        val searchItem = menu.findItem(R.id.filter_item_search)
        val searchView = searchItem.actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchView.clearFocus()
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                //TODO search
                return false
            }
        })
    }*/

}