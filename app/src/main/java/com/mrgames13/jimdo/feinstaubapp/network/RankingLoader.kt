/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.network

import android.content.Context
import android.util.Log
import com.mrgames13.jimdo.feinstaubapp.R
import com.mrgames13.jimdo.feinstaubapp.model.other.RankingItem
import com.mrgames13.jimdo.feinstaubapp.shared.Constants.TAG
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

const val RANKING_CITY = 1
const val RANKING_COUNTRY = 2

suspend fun loadRanking(context: Context, mode: Int): List<RankingItem> {
    try {
        val subRes = if (mode == RANKING_CITY) "/ranking/city" else "/ranking/country"
        val response = networkClient.get<HttpStatement>(context.getString(R.string.api_root) + subRes + "?compressed").execute()
        if(response.status == HttpStatusCode.OK) {
            val responseContent = URLDecoder.decode(response.readText(), StandardCharsets.UTF_8.name())
            return Json.decodeFromString<List<RankingItem>>(responseContent).map { RankingItem(it.country, it.city, it.count) }
        } else {
            Log.e(TAG, response.status.toString())
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return emptyList()
}