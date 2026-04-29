package esprit.tn.blog.service;

import esprit.tn.blog.entity.ForumPost;
import esprit.tn.blog.repository.ForumPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ForumPostService {

    private final ForumPostRepository forumPostRepository;

    public ForumPost createPost(ForumPost post) {
        return forumPostRepository.save(post);
    }

    public List<ForumPost> getAllPosts() {
        return forumPostRepository.findAll();
    }

    public ForumPost getPostById(Long id) {
        return forumPostRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + id));
    }

    public List<ForumPost> getTopLevelPosts() {
        return forumPostRepository.findByParentPostIdIsNullOrderByCreatedAtDesc();
    }

    public List<ForumPost> getPostsByTopic(Long topicId) {
        return forumPostRepository.findByTopicId(topicId);
    }

    public List<ForumPost> getPostsByUser(Long userId) {
        return forumPostRepository.findByUserId(userId);
    }

    public List<ForumPost> getReplies(Long parentPostId) {
        return forumPostRepository.findByParentPostId(parentPostId);
    }

    public ForumPost updatePost(Long id, ForumPost updatedPost) {
        ForumPost existing = getPostById(id);
        existing.setTopicId(updatedPost.getTopicId());
        existing.setContent(updatedPost.getContent());
        existing.setImage(updatedPost.getImage());
        existing.setAuthor(updatedPost.getAuthor());
        existing.setUsername(updatedPost.getUsername());
        existing.setAvatar(updatedPost.getAvatar());
        existing.setIsEdited(true);
        return forumPostRepository.save(existing);
    }

    public void deletePost(Long id) {
        if (!forumPostRepository.existsById(id))
            throw new RuntimeException("Post not found with id: " + id);
        forumPostRepository.deleteById(id);
    }

    public ForumPost likePost(Long id) {
        ForumPost post = getPostById(id);
        post.setLikes(post.getLikes() + 1);
        return forumPostRepository.save(post);
    }

    public ForumPost unlikePost(Long id) {
        ForumPost post = getPostById(id);
        post.setLikes(Math.max(0, post.getLikes() - 1));
        return forumPostRepository.save(post);
    }

    public ForumPost repostPost(Long id) {
        ForumPost post = getPostById(id);
        post.setReposts(post.getReposts() + 1);
        return forumPostRepository.save(post);
    }
}
