package com.suggestbox.ui.screens.submit

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.suggestbox.BuildConfig
import com.suggestbox.R
import com.suggestbox.core.designsystem.components.GroupedSection
import com.suggestbox.core.designsystem.components.GroupedToggleRow
import com.suggestbox.core.designsystem.components.SecondaryButton
import com.suggestbox.core.designsystem.components.SegmentedControl
import com.suggestbox.core.designsystem.components.SuggestCard
import com.suggestbox.core.designsystem.components.SuggestChip
import com.suggestbox.core.designsystem.components.SuggestTextField
import com.suggestbox.core.designsystem.icons.SuggestIcons
import com.suggestbox.core.designsystem.theme.SuggestTheme
import com.suggestbox.core.designsystem.theme.TabularNumeralStyle
import com.suggestbox.domain.model.SubmissionKind
import com.suggestbox.domain.model.SuggestionCategory
import com.suggestbox.domain.usecase.DuplicateMatch
import com.suggestbox.domain.usecase.ValidationError
import com.suggestbox.ui.components.VoiceErrorType
import com.suggestbox.ui.components.icon
import com.suggestbox.ui.components.rememberVoiceInputController

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SubmitStepCategoryView(
    state: SubmitUiState,
    onKindChange: (SubmissionKind) -> Unit,
    onCategoryChange: (SuggestionCategory) -> Unit,
) {
    val colors = SuggestTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = stringResource(R.string.submit_step_1_title),
            style = MaterialTheme.typography.headlineSmall,
            color = colors.textPrimary,
        )

        SegmentedControl(
            options = listOf(
                stringResource(R.string.kind_issue),
                stringResource(R.string.kind_idea),
            ),
            selectedIndex = state.kind.ordinal,
            onOptionSelected = { onKindChange(SubmissionKind.entries[it]) },
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SuggestionCategory.entries.forEach { cat ->
                SuggestChip(
                    label = stringResource(cat.labelRes),
                    selected = state.category == cat,
                    leadingIcon = cat.icon(),
                    onClick = { onCategoryChange(cat) },
                )
            }
        }
    }
}

@Composable
fun SubmitStepDetailsView(
    state: SubmitUiState,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onVoiceResult: (String) -> Unit,
    onUpvoteDuplicate: (String) -> Unit,
) {
    val colors = SuggestTheme.colors
    val context = LocalContext.current
    val promptText = stringResource(R.string.submit_voice_prompt)
    val controllerHolder = remember { arrayOfNulls<com.suggestbox.ui.components.VoiceInputController>(1) }

    val speechFallbackLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val text = matches?.firstOrNull().orEmpty().trim()
            if (text.isNotEmpty()) {
                onVoiceResult(text)
            } else {
                controllerHolder[0]?.reportExternalError(VoiceErrorType.NO_MATCH)
            }
        } else if (result.resultCode != Activity.RESULT_CANCELED) {
            controllerHolder[0]?.reportExternalError(VoiceErrorType.NO_MATCH)
        }
    }

    val voiceController = rememberVoiceInputController(
        onFinalTranscript = onVoiceResult,
        onFallbackToSystemDialog = { intent ->
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, promptText)
            runCatching {
                speechFallbackLauncher.launch(intent)
            }.onFailure {
                controllerHolder[0]?.reportExternalError(VoiceErrorType.UNAVAILABLE)
            }
        },
    )
    controllerHolder[0] = voiceController

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            voiceController.startListening()
        } else {
            voiceController.reportExternalError(VoiceErrorType.PERMISSION_DENIED)
        }
    }

    val titleError = when (state.validationError) {
        ValidationError.TITLE_TOO_SHORT -> stringResource(R.string.err_title_short)
        ValidationError.PROFANITY_OR_SPAM_DETECTED -> stringResource(R.string.err_profanity)
        else -> null
    }
    val descError = when (state.validationError) {
        ValidationError.DESCRIPTION_TOO_SHORT -> stringResource(R.string.err_desc_short)
        else -> null
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = stringResource(R.string.submit_step_2_title),
            style = MaterialTheme.typography.headlineSmall,
            color = colors.textPrimary,
        )

        SuggestTextField(
            value = state.title,
            onValueChange = onTitleChange,
            label = stringResource(R.string.submit_title_label),
            placeholder = stringResource(R.string.submit_title_hint),
            errorMessage = titleError,
        )

        SuggestTextField(
            value = state.description,
            onValueChange = onDescriptionChange,
            label = stringResource(R.string.submit_desc_label),
            placeholder = stringResource(R.string.submit_desc_hint),
            errorMessage = descError,
            singleLine = false,
            minLines = 4,
            maxLines = 8,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SecondaryButton(
                text = stringResource(
                    if (voiceController.isListening) R.string.submit_voice_stop else R.string.submit_voice_btn,
                ),
                leadingIcon = SuggestIcons.Mic,
                onClick = {
                    if (voiceController.isListening) {
                        voiceController.stopListening()
                    } else {
                        val hasMicPermission = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO,
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasMicPermission) {
                            voiceController.startListening()
                        } else {
                            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                },
                modifier = Modifier.weight(1f),
            )

            SecondaryButton(
                text = stringResource(R.string.submit_voice_system_dialog),
                leadingIcon = SuggestIcons.Mic,
                onClick = { voiceController.launchSystemVoiceDialog() },
                modifier = Modifier.weight(1f),
            )
        }

        // Quick Speech Dictation Presets (1-tap input for emulators & noisy environments)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.submit_voice_quick_presets),
                style = MaterialTheme.typography.labelSmall,
                color = colors.textTertiary,
            )
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val presets = listOf(
                    stringResource(R.string.submit_voice_preset_1),
                    stringResource(R.string.submit_voice_preset_2),
                    stringResource(R.string.submit_voice_preset_3),
                    stringResource(R.string.submit_voice_preset_4),
                )
                presets.forEach { preset ->
                    androidx.compose.material3.AssistChip(
                        onClick = { onVoiceResult(preset) },
                        label = {
                            Text(
                                text = preset,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = SuggestIcons.Mic,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = colors.primary,
                            )
                        },
                    )
                }
            }
        }

        AnimatedVisibility(visible = voiceController.isListening) {
            val animatedLevel by animateFloatAsState(
                targetValue = voiceController.audioLevel.coerceAtLeast(0.08f),
                label = "micLevel",
            )
            SuggestCard(
                containerColor = colors.primaryContainer,
                contentPadding = PaddingValues(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.submit_voice_listening),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.primary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { animatedLevel },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = colors.primary,
                    trackColor = colors.surface,
                )
                if (voiceController.partialText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = voiceController.partialText,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textPrimary,
                    )
                }
            }
        }

        val voiceErrorRes = when (voiceController.errorType) {
            VoiceErrorType.NO_MATCH -> R.string.submit_voice_no_match
            VoiceErrorType.NETWORK -> R.string.submit_voice_network_error
            VoiceErrorType.PERMISSION_DENIED -> R.string.submit_voice_permission_denied
            VoiceErrorType.UNAVAILABLE -> R.string.submit_voice_unavailable
            VoiceErrorType.NONE -> null
        }
        if (voiceErrorRes != null && !voiceController.isListening) {
            Text(
                text = stringResource(voiceErrorRes),
                style = MaterialTheme.typography.bodySmall,
                color = colors.destructive,
            )
        }

        // Synopsis feature: 150m spatial + semantic duplicate warning banner
        AnimatedVisibility(visible = state.duplicateMatches.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.duplicateMatches.forEach { match ->
                    DuplicateAlertBanner(
                        match = match,
                        onUpvote = { onUpvoteDuplicate(match.suggestion.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DuplicateAlertBanner(
    match: DuplicateMatch,
    onUpvote: () -> Unit,
) {
    val colors = SuggestTheme.colors
    SuggestCard(
        containerColor = colors.warningBg,
        contentPadding = PaddingValues(12.dp),
    ) {
        Text(
            text = stringResource(R.string.submit_duplicate_title, match.distanceMeters),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = colors.warning,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${match.suggestion.trackingId} • ${match.suggestion.title}",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textPrimary,
        )
        Spacer(modifier = Modifier.height(6.dp))
        TextButton(onClick = onUpvote, contentPadding = PaddingValues(0.dp)) {
            Icon(
                imageVector = SuggestIcons.Upvote,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "${stringResource(R.string.submit_duplicate_cta)} (${match.suggestion.upvoteCount})",
                color = colors.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
fun SubmitStepMediaView(
    state: SubmitUiState,
    onAddUris: (List<android.net.Uri>) -> Unit,
    onRemoveUri: (String) -> Unit,
) {
    val colors = SuggestTheme.colors
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 4),
    ) { uris ->
        onAddUris(uris)
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = stringResource(R.string.submit_step_3_title),
            style = MaterialTheme.typography.headlineSmall,
            color = colors.textPrimary,
        )
        Text(
            text = stringResource(R.string.submit_media_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
        )

        SecondaryButton(
            text = stringResource(R.string.submit_pick_media, state.mediaUris.size),
            leadingIcon = SuggestIcons.Camera,
            isLoading = state.isCompressingMedia,
            enabled = state.mediaUris.size < 4,
            onClick = {
                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
            },
            modifier = Modifier.fillMaxWidth(),
        )

        if (state.mediaUris.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.mediaUris) { uriString ->
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, colors.separator, RoundedCornerShape(14.dp)),
                    ) {
                        AsyncImage(
                            model = uriString,
                            contentDescription = "Attached media",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                        IconButton(
                            onClick = { onRemoveUri(uriString) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.65f)),
                        ) {
                            Icon(
                                imageVector = SuggestIcons.Close,
                                contentDescription = "Remove",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun SubmitStepLocationView(
    state: SubmitUiState,
    onCoordinatesChange: (Double, Double) -> Unit,
    onAddressChange: (String) -> Unit,
) {
    val colors = SuggestTheme.colors
    val context = LocalContext.current

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            runCatching {
                val client = LocationServices.getFusedLocationProviderClient(context)
                client.lastLocation.addOnSuccessListener { loc ->
                    if (loc != null) {
                        onCoordinatesChange(loc.latitude, loc.longitude)
                    }
                }
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.submit_step_4_title),
            style = MaterialTheme.typography.headlineSmall,
            color = colors.textPrimary,
        )

        Text(
            text = stringResource(R.string.submit_location_tap_hint),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
        )

        if (BuildConfig.HAS_MAPS_KEY) {
            val target = LatLng(state.location.latitude, state.location.longitude)
            val cameraState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(target, 16f)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(18.dp)),
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraState,
                    onMapClick = { latLng ->
                        onCoordinatesChange(latLng.latitude, latLng.longitude)
                    },
                ) {
                    Marker(
                        state = MarkerState(position = target),
                        title = state.location.addressLabel,
                    )
                }
            }
        } else {
            // Interactive local ward map canvas fallback when running without a Google Maps API key
            WardInteractivePinCanvas(
                latitude = state.location.latitude,
                longitude = state.location.longitude,
                onPinMoved = onCoordinatesChange,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "%.4f° N, %.4f° E".format(state.location.latitude, state.location.longitude),
                style = MaterialTheme.typography.labelMedium.merge(TabularNumeralStyle),
                color = colors.primary,
            )
            SecondaryButton(
                text = stringResource(R.string.submit_location_gps),
                leadingIcon = SuggestIcons.Gps,
                onClick = {
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                        ),
                    )
                },
            )
        }

        SuggestTextField(
            value = state.location.addressLabel,
            onValueChange = onAddressChange,
            label = stringResource(R.string.submit_address_label),
            placeholder = "e.g. Near Ward School, 12th Main",
            leadingIcon = SuggestIcons.Location,
        )
    }
}

@Composable
private fun WardInteractivePinCanvas(
    latitude: Double,
    longitude: Double,
    onPinMoved: (Double, Double) -> Unit,
) {
    val colors = SuggestTheme.colors
    var pinFraction by remember(latitude, longitude) {
        mutableStateOf(Offset(0.5f, 0.5f))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(colors.primaryContainer.copy(alpha = 0.55f))
            .border(1.dp, colors.primary.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val fx = (offset.x / size.width).coerceIn(0.05f, 0.95f)
                    val fy = (offset.y / size.height).coerceIn(0.05f, 0.95f)
                    pinFraction = Offset(fx, fy)
                    val deltaLat = (0.5f - fy) * 0.01
                    val deltaLng = (fx - 0.5f) * 0.01
                    onPinMoved(12.9784 + deltaLat, 77.6408 + deltaLng)
                }
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridLines = 5
            for (i in 1 until gridLines) {
                val x = size.width * (i / gridLines.toFloat())
                val y = size.height * (i / gridLines.toFloat())
                drawLine(colors.primary.copy(alpha = 0.12f), Offset(x, 0f), Offset(x, size.height), strokeWidth = 2f)
                drawLine(colors.primary.copy(alpha = 0.12f), Offset(0f, y), Offset(size.width, y), strokeWidth = 2f)
            }
            val px = size.width * pinFraction.x
            val py = size.height * pinFraction.y
            drawCircle(colors.primary.copy(alpha = 0.2f), radius = 42f, center = Offset(px, py))
            drawCircle(colors.primary, radius = 14f, center = Offset(px, py))
            drawCircle(Color.White, radius = 5f, center = Offset(px, py))
        }
        Text(
            text = "Ward Map Pin Preview",
            style = MaterialTheme.typography.labelSmall,
            color = colors.primary,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp),
        )
    }
}

@Composable
fun SubmitStepReviewView(
    state: SubmitUiState,
    onAnonymousChange: (Boolean) -> Unit,
) {
    val colors = SuggestTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = stringResource(R.string.submit_step_5_title),
            style = MaterialTheme.typography.headlineSmall,
            color = colors.textPrimary,
        )

        SuggestCard {
            Text(
                text = "${stringResource(state.kind.labelRes)} • ${stringResource(state.category.labelRes)}",
                style = MaterialTheme.typography.labelLarge,
                color = colors.primary,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = state.title,
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = state.description,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "📍 ${state.location.addressLabel} • ${state.mediaUris.size} media attached",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textTertiary,
            )
        }

        GroupedSection {
            GroupedToggleRow(
                title = stringResource(R.string.submit_anon_toggle),
                subtitle = stringResource(R.string.submit_anon_toggle_sub),
                checked = state.isAnonymous,
                onCheckedChange = onAnonymousChange,
                icon = SuggestIcons.Anonymous,
                iconTint = colors.statusUnderReview,
            )
        }

        if (state.validationError == ValidationError.RATE_LIMIT_EXCEEDED) {
            Text(
                text = stringResource(R.string.err_rate_limit),
                style = MaterialTheme.typography.bodySmall,
                color = colors.destructive,
            )
        }
    }
}
