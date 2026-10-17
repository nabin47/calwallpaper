package io.github.nabin47.calwallpaper.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.nabin47.calwallpaper.R
import io.github.nabin47.calwallpaper.ui.ScreenState

/**
 * A settings page. With a [preview], the preview stays pinned under a compact app bar while the controls scroll
 * below it, so every change is visible as it is made. Without one, a large app bar collapses as the content
 * scrolls. The snackbar host shows the app-wide [ScreenState.snackbar].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageScaffold(
    state: ScreenState,
    title: String,
    modifier: Modifier = Modifier,
    preview: (@Composable () -> Unit)? = null,
    content: LazyListScope.() -> Unit,
) {
    val scrollBehavior =
        if (preview == null) TopAppBarDefaults.exitUntilCollapsedScrollBehavior() else null
    Scaffold(
        modifier =
            if (scrollBehavior !=
                null
            ) {
                modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
            } else {
                modifier
            },
        snackbarHost = { SnackbarHost(state.snackbar) },
        topBar = {
            val back: @Composable () -> Unit = {
                IconButton(onClick = state.back) {
                    Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.back))
                }
            }
            if (scrollBehavior != null) {
                LargeTopAppBar(title = { Text(title) }, navigationIcon = back, scrollBehavior = scrollBehavior)
            } else {
                TopAppBar(title = { Text(title) }, navigationIcon = back)
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (preview != null) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(PREVIEW_HEIGHT_FRACTION)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                ) { preview() }
                HorizontalDivider()
            }
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
                content = content,
            )
        }
    }
}

/** Share of the page height the pinned preview takes; the controls scroll in the rest. */
private const val PREVIEW_HEIGHT_FRACTION = 0.42f
