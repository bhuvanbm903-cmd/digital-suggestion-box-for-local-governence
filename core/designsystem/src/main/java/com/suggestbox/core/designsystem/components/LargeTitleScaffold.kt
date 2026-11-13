package com.suggestbox.core.designsystem.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.LargeTopAppBar
import com.suggestbox.core.designsystem.icons.SuggestIcons
import com.suggestbox.core.designsystem.motion.SuggestMotion
import com.suggestbox.core.designsystem.motion.rememberSuggestHaptics
import com.suggestbox.core.designsystem.theme.SuggestTheme

/**
 * Apple HIG-style scaffold featuring a Large Title header that smoothly collapses
 * into a compact translucent bar as the user scrolls, plus optional bottom navigation bar.
 *
 * @param title Screen title shown large when expanded and centered/compact when collapsed.
 * @param subtitle Optional small eyebrow text (e.g. Ward name) shown above the large title.
 * @param onBackClick Optional back navigation button callback.
 * @param backContentDescription Accessibility label for the back button.
 * @param actions Optional trailing top bar actions.
 * @param bottomBar Optional translucent bottom bar.
 * @param snackbarHost Optional snackbar host composable.
 * @param content Main scrollable content receiving [PaddingValues].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LargeTitleScaffold(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBackClick: (() -> Unit)? = null,
    backContentDescription: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val colors = SuggestTheme.colors
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val collapsedFraction = scrollBehavior.state.collapsedFraction

    val barContainerColor by animateColorAsState(
        targetValue = if (collapsedFraction > 0.6f) colors.barTranslucent else colors.background,
        label = "topBarColor",
    )

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = colors.background,
        topBar = {
            Column(modifier = Modifier.background(barContainerColor)) {
                LargeTopAppBar(
                    title = {
                        Column(
                            modifier = Modifier.semantics { heading() },
                        ) {
                            if (!subtitle.isNullOrBlank() && collapsedFraction < 0.5f) {
                                Text(
                                    text = subtitle.uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                    ),
                                    color = colors.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Text(
                                text = title,
                                style = if (collapsedFraction > 0.65f) {
                                    MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                                } else {
                                    MaterialTheme.typography.displayLarge
                                },
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    },
                    navigationIcon = {
                        if (onBackClick != null) {
                            IconButton(
                                onClick = onBackClick,
                                modifier = Modifier.size(48.dp),
                            ) {
                                Icon(
                                    imageVector = SuggestIcons.Back,
                                    contentDescription = backContentDescription,
                                    tint = colors.primary,
                                )
                            }
                        }
                    },
                    actions = actions,
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                        titleContentColor = colors.textPrimary,
                    ),
                )
                AnimatedVisibility(
                    visible = collapsedFraction > 0.6f,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    HorizontalDivider(
                        thickness = SuggestTheme.spacing.hairline,
                        color = colors.separator.copy(alpha = 0.7f),
                    )
                }
            }
        },
        bottomBar = bottomBar,
        snackbarHost = snackbarHost,
        content = content,
    )
}

/**
 * Tab descriptor for the 4-tab Apple-style bottom bar.
 */
@Immutable
data class BottomBarItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

/**
 * Translucent bottom tab bar mimicking iOS UITabBar with 48dp+ touch targets,
 * subtle hairline separator, and animated scale feedback on selection.
 */
@Composable
fun TranslucentBottomBar(
    items: List<BottomBarItem>,
    selectedRoute: String?,
    onItemSelected: (BottomBarItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SuggestTheme.colors
    val haptics = rememberSuggestHaptics()

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = colors.barTranslucent,
        shadowElevation = 0.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(
                thickness = SuggestTheme.spacing.hairline,
                color = colors.separator.copy(alpha = 0.8f),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .heightIn(min = 60.dp)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEach { item ->
                    val isSelected = selectedRoute == item.route
                    val tint by animateColorAsState(
                        targetValue = if (isSelected) colors.primary else colors.textTertiary,
                        label = "tabTint",
                    )
                    val scale by animateFloatAsState(
                        targetValue = if (isSelected) 1.06f else 1f,
                        animationSpec = SuggestMotion.SnappySpring,
                        label = "tabScale",
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .semantics { this.selected = isSelected }
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                role = Role.Tab,
                            ) {
                                if (!isSelected) {
                                    haptics.select()
                                    onItemSelected(item)
                                }
                            }
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier
                                .size(24.dp)
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                },
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            ),
                            color = tint,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
