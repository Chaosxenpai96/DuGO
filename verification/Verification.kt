package com.example.dugo.login

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val VerifyBg = Color(0xFFFCFCFD)
private val VerifyBorder = Color(0xFFE1EAF2)
private val VerifyText = Color(0xFF111827)
private val VerifyMuted = Color(0xFF6B7280)
private val VerifyError = Color(0xFFD32F2F)
private val VerifyOk = Color(0xFF2E7D32)

/**
 * Optional "verify my ID and blood type" block for the sign-up forms.
 *
 * The user flips the switch to opt in, then attaches a photo of a valid ID and a
 * photo of a document that proves their blood type (donor card, lab result, etc.).
 * Nothing is uploaded here – the picked [Uri]s are handed back through the form so
 * your backend / Firebase Storage code can upload them on submit.
 */
@Composable
fun VerificationSection(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    idUri: Uri?,
    onIdChange: (Uri?) -> Unit,
    bloodProofUri: Uri?,
    onBloodProofChange: (Uri?) -> Unit,
    accent: Color,
    bloodProofTitle: String = "Proof of Blood Type",
    bloodProofHint: String = "Blood donor card, lab result or medical certificate",
    showErrors: Boolean = false
) {
    val shape = RoundedCornerShape(14.dp)

    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 22.dp)
            .clip(shape)
            .background(VerifyBg)
            .border(1.dp, if (enabled) accent else VerifyBorder, shape)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Verify my ID & blood type",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = VerifyText
                )
                Text(
                    "Optional. Get a verified badge so others can trust your profile.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = VerifyMuted,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = accent,
                    checkedThumbColor = Color.White
                )
            )
        }

        if (enabled) {
            UploadSlot(
                title = "Valid Government ID",
                hint = "Driver's license, passport, UMID, PhilSys or school ID",
                uri = idUri,
                onPicked = onIdChange,
                accent = accent,
                error = if (showErrors && idUri == null) "Please attach a photo of your ID" else null
            )
            UploadSlot(
                title = bloodProofTitle,
                hint = bloodProofHint,
                uri = bloodProofUri,
                onPicked = onBloodProofChange,
                accent = accent,
                error = if (showErrors && bloodProofUri == null) "Please attach your proof of blood type" else null
            )
            Text(
                "Your documents are only used to verify your account and are never shown to other users.",
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = VerifyMuted,
                modifier = Modifier.padding(top = 14.dp)
            )
        }
    }
}

@Composable
private fun UploadSlot(
    title: String,
    hint: String,
    uri: Uri?,
    onPicked: (Uri?) -> Unit,
    accent: Color,
    error: String?
) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { picked ->
        if (picked != null) onPicked(picked)
    }
    val shape = RoundedCornerShape(12.dp)
    val context = LocalContext.current

    Column(Modifier.padding(top = 16.dp)) {
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = VerifyText)

        Row(
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth()
                .clip(shape)
                .background(Color.White)
                .border(1.dp, if (error != null) VerifyError else VerifyBorder, shape)
                .clickable { launcher.launch("image/*") }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF1F5F9)),
                contentAlignment = Alignment.Center
            ) {
                val thumb by produceState<Bitmap?>(initialValue = null, key1 = uri) {
                    value = if (uri == null) null else loadThumbnail(context, uri)
                }
                val bmp = thumb
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(64.dp)
                    )
                } else {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = accent)
                }
            }

            Column(Modifier.weight(1f)) {
                if (uri == null) {
                    Text("Tap to upload a photo", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = accent)
                    Text(hint, fontSize = 12.sp, lineHeight = 16.sp, color = VerifyMuted)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = VerifyOk,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Photo attached", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = VerifyOk)
                    }
                    Text("Tap to replace", fontSize = 12.sp, color = VerifyMuted)
                }
            }

            if (uri != null) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Remove photo",
                    tint = VerifyMuted,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onPicked(null) }
                )
            }
        }

        if (error != null) {
            Text(
                error,
                color = VerifyError,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }
    }
}

/** Decodes a small preview of the picked image off the main thread. */
private suspend fun loadThumbnail(context: Context, uri: Uri, targetPx: Int = 256): Bitmap? =
    withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }

            var sample = 1
            while (bounds.outWidth / (sample * 2) >= targetPx && bounds.outHeight / (sample * 2) >= targetPx) {
                sample *= 2
            }
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        } catch (e: Exception) {
            null
        }
    }
