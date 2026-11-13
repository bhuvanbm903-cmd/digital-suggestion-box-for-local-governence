package com.suggestbox.data.remote.firebase

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.suggestbox.data.mock.MockSeedData
import com.suggestbox.domain.model.AdminAnalytics
import com.suggestbox.domain.model.Announcement
import com.suggestbox.domain.model.Comment
import com.suggestbox.domain.model.GeoLocation
import com.suggestbox.domain.model.ResolutionRating
import com.suggestbox.domain.model.StatusUpdateEvent
import com.suggestbox.domain.model.SubmissionKind
import com.suggestbox.domain.model.Suggestion
import com.suggestbox.domain.model.SuggestionCategory
import com.suggestbox.domain.model.SuggestionDraft
import com.suggestbox.domain.model.SuggestionPriority
import com.suggestbox.domain.model.SuggestionStatus
import com.suggestbox.domain.model.WardInfo
import com.suggestbox.domain.repository.AdminRepository
import com.suggestbox.domain.repository.SuggestionRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore + Firebase Storage implementation of [SuggestionRepository] and [AdminRepository].
 * Keeps all backend details isolated behind clean domain contracts.
 */
@Singleton
class FirebaseGovernanceRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val auth: FirebaseAuth,
) : SuggestionRepository, AdminRepository {

    override fun observeSuggestions(): Flow<List<Suggestion>> = callbackFlow {
        val currentUid = auth.currentUser?.uid.orEmpty()
        val registration = firestore.collection("suggestions")
            .orderBy("createdAtMillis", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull { doc ->
                    doc.toSuggestion(currentUid)
                }.orEmpty()
                trySend(items)
            }
        awaitClose { registration.remove() }
    }

    override fun observeSuggestionById(suggestionId: String): Flow<Suggestion?> =
        observeSuggestions().map { list -> list.firstOrNull { it.id == suggestionId } }

    override fun observeMySuggestions(userId: String): Flow<List<Suggestion>> =
        observeSuggestions().map { list -> list.filter { it.authorId == userId } }

    override fun observeAnnouncements(wardId: String): Flow<List<Announcement>> = callbackFlow {
        val registration = firestore.collection("announcements")
            .whereEqualTo("wardId", wardId)
            .orderBy("publishedAtMillis", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                val list = snapshot?.documents?.mapNotNull { doc ->
                    Announcement(
                        id = doc.id,
                        wardId = doc.getString("wardId") ?: wardId,
                        title = doc.getString("title").orEmpty(),
                        body = doc.getString("body").orEmpty(),
                        department = doc.getString("department").orEmpty(),
                        publishedAtMillis = doc.getLong("publishedAtMillis") ?: 0L,
                        isPinned = doc.getBoolean("isPinned") ?: false,
                    )
                }.orEmpty()
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    override fun observeWards(): Flow<List<WardInfo>> = flowOf(MockSeedData.wards)

    override suspend fun findByTrackingId(trackingId: String): Suggestion? {
        val uid = auth.currentUser?.uid.orEmpty()
        val snap = firestore.collection("suggestions")
            .whereEqualTo("trackingId", trackingId.trim().uppercase())
            .limit(1)
            .get()
            .await()
        return snap.documents.firstOrNull()?.toSuggestion(uid)
    }

    override suspend fun submitSuggestion(draft: SuggestionDraft): Result<Suggestion> = runCatching {
        val uid = auth.currentUser?.uid ?: "anonymous"
        val docRef = firestore.collection("suggestions").document()
        val trackingId = "SBX-W14-${(2100..9999).random()}"
        val now = System.currentTimeMillis()

        val uploadedUrls = draft.localMediaUris.mapIndexed { idx, uriStr ->
            val ref = storage.reference.child("suggestions/${docRef.id}/media_$idx.jpg")
            ref.putFile(Uri.parse(uriStr)).await()
            ref.downloadUrl.await().toString()
        }

        val loc = draft.location ?: GeoLocation(12.9784, 77.6408, "Ward 14")
        val payload = hashMapOf(
            "trackingId" to trackingId,
            "kind" to draft.kind.name,
            "title" to draft.title.trim(),
            "description" to draft.description.trim(),
            "category" to draft.category.name,
            "status" to SuggestionStatus.SUBMITTED.name,
            "priority" to SuggestionPriority.MEDIUM.name,
            "wardId" to "ward-14",
            "wardName" to "Ward 14 • Indiranagar",
            "latitude" to loc.latitude,
            "longitude" to loc.longitude,
            "addressLabel" to loc.addressLabel,
            "mediaUrls" to uploadedUrls,
            "isAnonymous" to draft.isAnonymous,
            "authorId" to if (draft.isAnonymous) "anonymous" else uid,
            "authorDisplayName" to if (draft.isAnonymous) "Anonymous Citizen" else "Verified Citizen",
            "upvoteCount" to 1L,
            "upvoterIds" to listOf(uid),
            "createdAtMillis" to now,
            "slaDeadlineMillis" to now + SuggestionPriority.MEDIUM.slaHours * 3_600_000L,
        )
        docRef.set(payload).await()
        docRef.get().await().toSuggestion(uid) ?: error("Failed to read created suggestion")
    }

    override suspend fun toggleUpvote(suggestionId: String): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: "anonymous"
        val docRef = firestore.collection("suggestions").document(suggestionId)
        firestore.runTransaction { tx ->
            val snap = tx.get(docRef)
            val upvoters = (snap.get("upvoterIds") as? List<*>)?.filterIsInstance<String>().orEmpty()
            if (uid in upvoters) {
                tx.update(docRef, "upvoterIds", FieldValue.arrayRemove(uid))
                tx.update(docRef, "upvoteCount", FieldValue.increment(-1))
            } else {
                tx.update(docRef, "upvoterIds", FieldValue.arrayUnion(uid))
                tx.update(docRef, "upvoteCount", FieldValue.increment(1))
            }
        }.await()
    }

    override suspend fun addComment(suggestionId: String, body: String): Result<Unit> = runCatching {
        val commentMap = mapOf(
            "id" to UUID.randomUUID().toString(),
            "authorLabel" to "Citizen",
            "isOfficial" to false,
            "body" to body.trim(),
            "createdAtMillis" to System.currentTimeMillis(),
        )
        firestore.collection("suggestions").document(suggestionId)
            .update("comments", FieldValue.arrayUnion(commentMap))
            .await()
    }

    override suspend fun submitRating(suggestionId: String, stars: Int, feedback: String): Result<Unit> = runCatching {
        val ratingMap = mapOf(
            "stars" to stars.coerceIn(1, 5),
            "feedback" to feedback.trim(),
            "submittedAtMillis" to System.currentTimeMillis(),
        )
        firestore.collection("suggestions").document(suggestionId)
            .update("rating", ratingMap)
            .await()
    }

    override fun observeAnalytics(wardId: String?): Flow<AdminAnalytics> =
        observeSuggestions().map { all ->
            val filtered = if (wardId == null) all else all.filter { it.wardId == wardId }
            AdminAnalytics(
                totalCount = filtered.size,
                countByStatus = SuggestionStatus.entries.associateWith { st -> filtered.count { it.status == st } },
                countByCategory = filtered.groupingBy { it.category }.eachCount(),
                overdueCount = filtered.count { it.isOverdue },
                averageResolutionHours = 36.0,
            )
        }

    override suspend fun updateSuggestionStatus(
        suggestionId: String,
        newStatus: SuggestionStatus,
        officialResponse: String,
    ): Result<Unit> = runCatching {
        val eventMap = mapOf(
            "id" to UUID.randomUUID().toString(),
            "status" to newStatus.name,
            "timestampMillis" to System.currentTimeMillis(),
            "note" to officialResponse.trim(),
            "actorName" to "Ward Officer",
        )
        firestore.collection("suggestions").document(suggestionId).update(
            mapOf(
                "status" to newStatus.name,
                "officialResponse" to officialResponse.trim(),
                "timeline" to FieldValue.arrayUnion(eventMap),
            ),
        ).await()
    }

    override suspend fun assignDepartmentAndPriority(
        suggestionId: String,
        department: String,
        officerName: String,
        priority: SuggestionPriority,
    ): Result<Unit> = runCatching {
        firestore.collection("suggestions").document(suggestionId).update(
            mapOf(
                "assignedDepartment" to department,
                "assignedOfficerName" to officerName,
                "priority" to priority.name,
            ),
        ).await()
    }

    override suspend fun markDuplicateAndMerge(
        duplicateSuggestionId: String,
        primaryTrackingId: String,
    ): Result<Unit> = runCatching {
        firestore.collection("suggestions").document(duplicateSuggestionId).update(
            mapOf(
                "status" to SuggestionStatus.REJECTED.name,
                "duplicateOfTrackingId" to primaryTrackingId,
                "officialResponse" to "Merged as duplicate of $primaryTrackingId.",
            ),
        ).await()
    }

    override suspend fun broadcastAnnouncement(
        wardId: String,
        title: String,
        body: String,
        department: String,
    ): Result<Announcement> = runCatching {
        val now = System.currentTimeMillis()
        val ref = firestore.collection("announcements").document()
        val payload = mapOf(
            "wardId" to wardId,
            "title" to title.trim(),
            "body" to body.trim(),
            "department" to department.trim(),
            "publishedAtMillis" to now,
            "isPinned" to true,
        )
        ref.set(payload).await()
        Announcement(ref.id, wardId, title.trim(), body.trim(), department.trim(), now, true)
    }

    override suspend fun exportSummaryReportCsv(wardId: String?): Result<String> = runCatching {
        val snap = firestore.collection("suggestions").get().await()
        val uid = auth.currentUser?.uid.orEmpty()
        val rows = snap.documents.mapNotNull { it.toSuggestion(uid) }
            .filter { wardId == null || it.wardId == wardId }
            .joinToString("\n") { s ->
                "${s.trackingId},\"${s.title.replace("\"", "'")}\",${s.category},${s.status},${s.priority},${s.wardName},${s.upvoteCount}"
            }
        "TrackingId,Title,Category,Status,Priority,Ward,Upvotes\n$rows"
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toSuggestion(currentUid: String): Suggestion? {
        val title = getString("title") ?: return null
        val upvoterIds = (get("upvoterIds") as? List<*>)?.filterIsInstance<String>().orEmpty()
        return Suggestion(
            id = id,
            trackingId = getString("trackingId") ?: id.take(8).uppercase(),
            kind = runCatching { SubmissionKind.valueOf(getString("kind") ?: "ISSUE") }.getOrDefault(SubmissionKind.ISSUE),
            title = title,
            description = getString("description").orEmpty(),
            category = runCatching { SuggestionCategory.valueOf(getString("category") ?: "OTHER") }
                .getOrDefault(SuggestionCategory.OTHER),
            status = runCatching { SuggestionStatus.valueOf(getString("status") ?: "SUBMITTED") }
                .getOrDefault(SuggestionStatus.SUBMITTED),
            priority = runCatching { SuggestionPriority.valueOf(getString("priority") ?: "MEDIUM") }
                .getOrDefault(SuggestionPriority.MEDIUM),
            wardId = getString("wardId") ?: "ward-14",
            wardName = getString("wardName") ?: "Ward 14",
            location = GeoLocation(
                latitude = getDouble("latitude") ?: 12.9784,
                longitude = getDouble("longitude") ?: 77.6408,
                addressLabel = getString("addressLabel").orEmpty(),
            ),
            mediaUrls = (get("mediaUrls") as? List<*>)?.filterIsInstance<String>().orEmpty(),
            afterPhotoUrl = getString("afterPhotoUrl"),
            isAnonymous = getBoolean("isAnonymous") ?: false,
            authorId = getString("authorId").orEmpty(),
            authorDisplayName = getString("authorDisplayName") ?: "Citizen",
            assignedDepartment = getString("assignedDepartment"),
            assignedOfficerName = getString("assignedOfficerName"),
            upvoteCount = (getLong("upvoteCount") ?: 0L).toInt(),
            upvotedByCurrentUser = currentUid in upvoterIds,
            comments = emptyList(),
            timeline = listOf(
                StatusUpdateEvent(
                    id = "init",
                    status = SuggestionStatus.SUBMITTED,
                    timestampMillis = getLong("createdAtMillis") ?: 0L,
                    note = "Submitted to local governance portal",
                    actorName = "System",
                ),
            ),
            createdAtMillis = getLong("createdAtMillis") ?: 0L,
            slaDeadlineMillis = getLong("slaDeadlineMillis") ?: System.currentTimeMillis(),
            officialResponse = getString("officialResponse"),
            duplicateOfTrackingId = getString("duplicateOfTrackingId"),
        )
    }
}
