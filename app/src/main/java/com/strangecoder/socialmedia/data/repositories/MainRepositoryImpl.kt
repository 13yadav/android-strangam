package com.strangecoder.socialmedia.data.repositories

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.strangecoder.socialmedia.data.entities.*
import com.strangecoder.socialmedia.commons.Constants.DEFAULT_PROFILE_PICTURE_URL
import com.strangecoder.socialmedia.commons.Resource
import com.strangecoder.socialmedia.commons.getConversationId
import com.strangecoder.socialmedia.commons.safeCall
import com.strangecoder.socialmedia.domain.MainRepository
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject

@ViewModelScoped
class MainRepositoryImpl @Inject constructor() : MainRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = Firebase.storage
    private val users = firestore.collection("users")
    private val posts = firestore.collection("posts")
    private val comments = firestore.collection("comments")
    private val chats = firestore.collection("chats")

    override suspend fun createPost(imageUri: Uri, text: String) = withContext(Dispatchers.IO) {
        safeCall {
            val uid = auth.uid!!
            val postId = UUID.randomUUID().toString()
            val imageUploadResult = storage.getReference(postId).putFile(imageUri).await()
            val imageUrl = imageUploadResult?.metadata?.reference?.downloadUrl?.await().toString()
            val post = Post(
                id = postId,
                authorUid = uid,
                text = text,
                imageUrl = imageUrl,
                date = System.currentTimeMillis()
            )
            posts.document(postId).set(post).await()
            Resource.Success(Any())
        }
    }

    override suspend fun getAllUsers() = withContext(Dispatchers.IO) {
        safeCall {
            val userResults = users.get().await().toObjects(User::class.java)
            Resource.Success(userResults)
        }
    }

    override suspend fun getUsers(uids: List<String>) = withContext(Dispatchers.IO) {
        safeCall {
            val chunks = uids.chunked(10)
            val resultList = mutableListOf<User>()
            chunks.forEach { chunk ->
                val usersList = users.whereIn("uid", chunk).orderBy("username").get().await()
                    .toObjects(User::class.java)
                resultList.addAll(usersList)
            }
            Resource.Success(resultList.toList())
        }
    }

    override suspend fun getUser(uid: String) = withContext(Dispatchers.IO) {
        safeCall {
            val user = users.document(uid).get().await().toObject(User::class.java)
                ?: throw IllegalStateException()
            val currentUid = FirebaseAuth.getInstance().uid!!
            val currentUser = users.document(currentUid).get().await().toObject(User::class.java)
                ?: throw IllegalStateException()
            user.isFollowing = uid in currentUser.follows
            Resource.Success(user)
        }
    }

    override suspend fun getPostsForFollows() = withContext(Dispatchers.IO) {
        safeCall {
            val uid = FirebaseAuth.getInstance().uid!!
            val follows = getUser(uid).data!!.follows
            val allPosts = posts.whereIn("authorUid", follows)
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .await()
                .toObjects(Post::class.java)
                .onEach { post ->
                    val user = getUser(post.authorUid).data!!
                    post.authorProfilePictureUrl = user.profilePictureUrl
                    post.authorUsername = user.username
                    post.isLiked = uid in post.likedBy
                }
            Resource.Success(allPosts)
        }
    }

    override suspend fun getPostForProfile(uid: String) = withContext(Dispatchers.IO) {
        safeCall {
            val profilePosts = posts.whereEqualTo("authorUid", uid)
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .await()
                .toObjects(Post::class.java)
                .onEach { post ->
                    val user = getUser(post.authorUid).data!!
                    post.authorProfilePictureUrl = user.profilePictureUrl
                    post.authorUsername = user.username
                    post.isLiked = uid in post.likedBy
                }
            Resource.Success(profilePosts)
        }
    }

    override suspend fun toggleLikeForPost(post: Post) = withContext(Dispatchers.IO) {
        safeCall {
            var isLiked = false
            firestore.runTransaction { transaction ->
                val uid = FirebaseAuth.getInstance().uid!!
                val postResult = transaction.get(posts.document(post.id))
                val currentLikes = postResult.toObject(Post::class.java)?.likedBy ?: listOf()
                transaction.update(
                    posts.document(post.id),
                    "likedBy",
                    if (uid in currentLikes)
                        currentLikes - uid
                    else {
                        isLiked = true
                        currentLikes + uid
                    }
                )
            }.await()
            Resource.Success(isLiked)
        }
    }

    override suspend fun deletePost(post: Post) = withContext(Dispatchers.IO) {
        safeCall {
            posts.document(post.id).delete().await()
            storage.getReferenceFromUrl(post.imageUrl).delete().await()
            Resource.Success(post)
        }
    }

    override suspend fun toggleFollowForUser(uid: String) = withContext(Dispatchers.IO) {
        safeCall {
            var isFollowing = false
            firestore.runTransaction { transaction ->
                val currentUid = auth.uid!!
                val currentUser =
                    transaction.get(users.document(currentUid)).toObject(User::class.java)!!
                isFollowing = uid in currentUser.follows
                val newFollows =
                    if (isFollowing) currentUser.follows - uid else currentUser.follows + uid
                transaction.update(users.document(currentUid), "follows", newFollows)
            }.await()
            Resource.Success(!isFollowing)
        }
    }

    override suspend fun searchUser(query: String) = withContext(Dispatchers.IO) {
        safeCall {
            val userResults =
                users.whereGreaterThanOrEqualTo("username", query.uppercase(Locale.ROOT))
                    .get()
                    .await()
                    .toObjects(User::class.java)
            Resource.Success(userResults)
        }
    }

    override suspend fun createComment(commentText: String, postId: String) =
        withContext(Dispatchers.IO) {
            safeCall {
                val uid = auth.uid!!
                val commentId = UUID.randomUUID().toString()
                val user = getUser(uid).data!!
                val comment = Comment(
                    commentId = commentId,
                    postId = postId,
                    uid = uid,
                    username = user.username,
                    profilePictureUrl = user.profilePictureUrl,
                    comment = commentText
                )
                comments.document(commentId).set(comment).await()
                Resource.Success(comment)
            }
        }

    override suspend fun getCommentForPost(postId: String) = withContext(Dispatchers.IO) {
        safeCall {
            val commentsForPost = comments
                .whereEqualTo("postId", postId)
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .await()
                .toObjects(Comment::class.java)
                .onEach { comment ->
                    val user = getUser(comment.uid).data!!
                    comment.username = user.username
                    comment.profilePictureUrl = user.profilePictureUrl
                }
            Resource.Success(commentsForPost)
        }
    }

    override suspend fun deleteComment(comment: Comment) = withContext(Dispatchers.IO) {
        safeCall {
            comments.document(comment.commentId).delete().await()
            Resource.Success(comment)
        }
    }

    override suspend fun updateProfilePicture(uid: String, imageUri: Uri) =
        withContext(Dispatchers.IO) {
            val storageRef = storage.getReference(uid)
            val user = getUser(uid).data!!
            if (user.profilePictureUrl != DEFAULT_PROFILE_PICTURE_URL) {
                storage.getReferenceFromUrl(user.profilePictureUrl).delete().await()
            }
            storageRef.putFile(imageUri).await().metadata?.reference?.downloadUrl?.await()
        }

    override suspend fun updateProfile(profileUpdate: ProfileUpdate) =
        withContext(Dispatchers.IO) {
            safeCall {
                val imageUrl = profileUpdate.profilePictureUri?.let { uri ->
                    updateProfilePicture(profileUpdate.uidToUpdate, uri).toString()
                }
                val map = mutableMapOf(
                    "username" to profileUpdate.username,
                    "bio" to profileUpdate.bio
                )
                imageUrl?.let { url ->
                    map["profilePictureUrl"] = url
                }
                users.document(profileUpdate.uidToUpdate).update(map.toMap()).await()
                Resource.Success(Any())
            }
        }

    override suspend fun sendMessage(message: Message) =
        withContext(Dispatchers.IO) {
            safeCall {
                val idFrom = auth.uid!!
                val chatID = getConversationId(idFrom, message.idTo)
                chats.document(chatID)
                    .collection(chatID)
                    .add(message).await()
                Resource.Success(message)
            }
        }

    override suspend fun loadMessages(idFrom: String, idTo: String) = withContext(Dispatchers.IO) {
        safeCall {
            val messages = mutableListOf<Message>()
            val chatID = getConversationId(idFrom, idTo)
            val result = chats.document(chatID)
                .collection(chatID)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
                .toObjects(Message::class.java)
            messages.addAll(result)
            Resource.Success(messages.toList())
        }
    }

    override suspend fun updateLastMessage(
        idFrom: String,
        idTo: String,
        lastMessage: LastMessage
    ): Resource<Any> {
        TODO("Not yet implemented")
    }

    override suspend fun getLastMessage(idFrom: String, idTo: String): Resource<LastMessage> {
        TODO("Not yet implemented")
    }
}