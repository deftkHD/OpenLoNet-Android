package de.deftk.openlonet.fragments.feature.forum

import android.os.Bundle
import android.view.*
import android.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import de.deftk.lonet.api.model.IGroup
import de.deftk.openlonet.R
import de.deftk.openlonet.adapter.recycler.ForumPostAdapter
import de.deftk.openlonet.api.Response
import de.deftk.openlonet.databinding.FragmentForumPostsBinding
import de.deftk.openlonet.viewmodel.ForumViewModel
import de.deftk.openlonet.viewmodel.UserViewModel

class ForumPostsFragment : Fragment() {

    //TODO context menu

    private val args: ForumPostsFragmentArgs by navArgs()
    private val userViewModel: UserViewModel by activityViewModels()
    private val forumViewModel: ForumViewModel by activityViewModels()

    private lateinit var binding: FragmentForumPostsBinding
    private lateinit var group: IGroup

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentForumPostsBinding.inflate(inflater, container, false)
        group = userViewModel.apiContext.value?.getUser()?.getGroups()?.firstOrNull { it.login == args.groupId } ?: error("Failed to find given group")

        val adapter = ForumPostAdapter(group)
        binding.forumList.adapter = adapter
        binding.forumList.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        forumViewModel.getForumPosts(group).observe(viewLifecycleOwner) { resource ->
            if (resource is Response.Success) {
                adapter.submitList(resource.value)
                binding.forumEmpty.isVisible = resource.value.isEmpty()
            } else if (resource is Response.Failure) {
                //TODO handle error
                resource.exception.printStackTrace()
            }
            binding.progressForum.isVisible = false
            binding.forumSwipeRefresh.isRefreshing = false
        }

        binding.forumSwipeRefresh.setOnRefreshListener {
            userViewModel.apiContext.value?.also { apiContext ->
                forumViewModel.loadForumPosts(group, null, apiContext)
            }
        }

        userViewModel.apiContext.observe(viewLifecycleOwner) { apiContext ->
            apiContext?.apply {
                forumViewModel.loadForumPosts(group, null, this)
            }
        }

        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
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
    }

}