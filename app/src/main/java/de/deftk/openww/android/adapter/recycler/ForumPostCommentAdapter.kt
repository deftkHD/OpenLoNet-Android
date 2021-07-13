package de.deftk.openww.android.adapter.recycler

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import de.deftk.openww.android.R
import de.deftk.openww.android.databinding.RecyclerItemForumCommentBinding
import de.deftk.openww.android.fragments.feature.forum.ForumPostFragmentDirections
import de.deftk.openww.android.viewmodel.ForumViewModel
import de.deftk.openww.api.model.IGroup
import de.deftk.openww.api.model.feature.forum.IForumPost

class ForumPostCommentAdapter(private val group: IGroup, private val path: Array<String>, private val forumViewModel: ForumViewModel): ListAdapter<IForumPost, ForumPostCommentAdapter.CommentViewHolder>(ForumPostDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val binding = RecyclerItemForumCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CommentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = getItem(position)
        val children = forumViewModel.getComments(group, comment.id)
        holder.bind(comment, group, path, children.isNotEmpty())
    }

    public override fun getItem(position: Int): IForumPost {
        return super.getItem(position)
    }

    class CommentViewHolder(val binding: RecyclerItemForumCommentBinding): RecyclerView.ViewHolder(binding.root) {

        init {
            binding.setMenuClickListener {
                itemView.showContextMenu()
            }
        }

        fun bind(post: IForumPost, group: IGroup, path: Array<String>, hasChildren: Boolean) {
            binding.post = post
            binding.group = group
            binding.hasChildren = hasChildren
            binding.setShowMoreClickListener { view ->
                val action = ForumPostFragmentDirections.actionForumPostFragmentSelf(group.login, post.id, path, view.context.getString(R.string.see_comment))
                view.findNavController().navigate(action)
            }
            binding.executePendingBindings()
        }

    }

}