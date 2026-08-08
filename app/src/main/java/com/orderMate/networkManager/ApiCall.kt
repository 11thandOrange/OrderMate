package com.orderMate.networkManager


import com.orderMate.modals.ConversationsResponse
import com.orderMate.modals.CreateEmailConversationRequest
import com.orderMate.modals.CreateSmsConversationRequest
import com.orderMate.modals.MessagesResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query


/*
* Every Bird Account have multiple workSpace.Where Each Workspace has multiple channels
* Such as Email , instagram and sms . Each channel has Unique Channel Id and you need the access
* token to share the sms , email etc that access token will remain same for all
* */
interface ApiCall {

    /**
     * Sends via the Conversations API's Create Conversation endpoint (not the
     * simpler Channels API) so the resulting conversation can carry a `resource`
     * tag - required for notification history to later be queryable by order ID.
     */
    @POST("/workspaces/{workspaceId}/conversations")
    suspend fun createEmailConversation(
        @Path("workspaceId") workspaceId: String,
        @Body request: CreateEmailConversationRequest
    ): Response<Any>

    @POST("/workspaces/{workspaceId}/conversations")
    suspend fun createSmsConversation(
        @Path("workspaceId") workspaceId: String,
        @Body request: CreateSmsConversationRequest
    ): Response<Any>

    /**
     * Get conversations filtered by resource (order ID)
     * Use resource format: "order:{orderId}"
     */
    @GET("/workspaces/{workspaceId}/conversations")
    suspend fun getConversationsByOrder(
        @Path("workspaceId") workspaceId: String,
        @Query("resource") resource: String,
        @Query("limit") limit: Int = 100
    ): Response<ConversationsResponse>

    /**
     * Get all messages in a conversation
     */
    @GET("/workspaces/{workspaceId}/conversations/{conversationId}/messages")
    suspend fun getConversationMessages(
        @Path("workspaceId") workspaceId: String,
        @Path("conversationId") conversationId: String,
        @Query("limit") limit: Int = 100
    ): Response<MessagesResponse>

}