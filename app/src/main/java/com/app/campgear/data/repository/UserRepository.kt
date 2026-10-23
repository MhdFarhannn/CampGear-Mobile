package com.app.campgear.data.repository

import com.app.campgear.data.ApiResult
import com.app.campgear.data.ApiService
import com.app.campgear.data.PagedResponse
import com.app.campgear.data.UserRequest
import com.app.campgear.data.UserResponse
import com.app.campgear.data.actionCall
import com.app.campgear.data.apiCall

class UserRepository(private val api: ApiService) {
    suspend fun getUsers(
        page: Int,
        search: String?,
        idRole: Long? = null
    ): ApiResult<PagedResponse<UserResponse>> =
        apiCall { api.getUsers(page, PAGE_SIZE, search, idRole) }

    suspend fun getById(id: Long): ApiResult<UserResponse> =
        apiCall { api.getUserById(id) }

    suspend fun getAllForPicker(): ApiResult<List<UserResponse>> {
        val users = linkedMapOf<Long, UserResponse>()
        var page = 1
        while (true) {
            when (val result = apiCall { api.getUsers(page, PICKER_PAGE_SIZE, null, null) }) {
                is ApiResult.Failure -> return result
                is ApiResult.Success -> {
                    result.data.items.forEach { users[it.id] = it }
                    if (result.data.items.isEmpty() || page.toLong() * result.data.pageSize >= result.data.total) break
                    page++
                }
            }
        }
        return ApiResult.Success(users.values.toList())
    }

    suspend fun create(request: UserRequest): ApiResult<Unit> =
        actionCall { api.createUser(request) }

    suspend fun update(id: Long, request: UserRequest): ApiResult<Unit> =
        actionCall { api.updateUser(id, request) }

    suspend fun delete(id: Long): ApiResult<Unit> =
        actionCall { api.deleteUser(id) }

    companion object {
        const val PAGE_SIZE = 20
        const val PICKER_PAGE_SIZE = 100
    }
}
