package com.torahanytime.audio.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.torahanytime.audio.ui.theme.TATBlue
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Observes a LazyListState and triggers [onLoadMore] when the user scrolls
 * near the bottom of the list. Uses a threshold of 3 items from the end.
 */
@Composable
fun InfiniteScrollHandler(
    listState: LazyListState,
    isLoadingMore: Boolean,
    hasMorePages: Boolean,
    onLoadMore: () -> Unit
) {
    val shouldLoadMore = remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleIndex >= totalItems - 3
        }
    }

    LaunchedEffect(shouldLoadMore) {
        snapshotFlow { shouldLoadMore.value }
            .distinctUntilChanged()
            .collect { shouldLoad ->
                if (shouldLoad && !isLoadingMore && hasMorePages) {
                    onLoadMore()
                }
            }
    }
}

/**
 * A loading indicator shown at the bottom of a paginated list.
 */
@Composable
fun PaginationLoadingItem() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = TATBlue,
            modifier = Modifier.size(24.dp),
            strokeWidth = 2.dp
        )
    }
}
