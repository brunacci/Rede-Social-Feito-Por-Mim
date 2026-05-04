package com.rdsocial.data.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

object Base64ImageEncoder {

    private const val MAX_SIDE_PX = 800
    private const val INITIAL_JPEG_QUALITY = 55
    private const val MIN_JPEG_QUALITY = 28
    private const val QUALITY_STEP = 8

    private const val MAX_JPEG_BYTES = 600_000

    private const val MAX_BASE64_CHARS = 900_000

    fun encodeUriToBase64Jpeg(context: Context, imageUri: Uri): Result<String> = runCatching {
        var bitmap = decodeAndScaleBitmap(context, imageUri, MAX_SIDE_PX)
            ?: error("Não foi possível decodificar a imagem.")

        try {
            var jpegBytes = compressWithQualityLoop(bitmap)
            if (jpegBytes.size > MAX_JPEG_BYTES) {
                val smaller = createScaledDownCopy(bitmap, (max(bitmap.width, bitmap.height) * 2 / 3).coerceAtLeast(320))
                if (smaller != null) {
                    bitmap.recycle()
                    bitmap = smaller
                    jpegBytes = compressWithQualityLoop(bitmap)
                }
            }
            if (jpegBytes.size > MAX_JPEG_BYTES) {
                error(
                    "Não foi possível comprimir a imagem abaixo do limite seguro para o Firestore " +
                        "(${jpegBytes.size} bytes).",
                )
            }
            val base64 = Base64.encodeToString(jpegBytes, Base64.NO_WRAP)
            if (base64.length > MAX_BASE64_CHARS) {
                error(
                    "Imagem ainda grande demais após compressão (${base64.length} caracteres). " +
                        "Escolha outra imagem ou reduza a resolução.",
                )
            }
            base64
        } finally {
            bitmap.recycle()
        }
    }

    private fun decodeAndScaleBitmap(context: Context, uri: Uri, maxSide: Int): Bitmap? {
        val resolver = context.contentResolver
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, boundsOptions) }
        if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) return null

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(boundsOptions, maxSide, maxSide)
            inPreferredConfig = Bitmap.Config.RGB_565
        }

        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOptions)
        } ?: return null

        return scaleDownToMaxSideInPlace(decoded, maxSide)
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int,
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            var halfHeight = height / 2
            var halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return max(1, inSampleSize)
    }

    private fun scaleDownToMaxSideInPlace(source: Bitmap, maxSide: Int): Bitmap {
        val w = source.width
        val h = source.height
        val largest = max(w, h)
        if (largest <= maxSide) return source

        val scale = maxSide.toFloat() / largest
        val newW = max(1, (w * scale).roundToInt())
        val newH = max(1, (h * scale).roundToInt())
        val scaled = Bitmap.createScaledBitmap(source, newW, newH, true)
        if (scaled != source) {
            source.recycle()
        }
        return scaled
    }

    private fun createScaledDownCopy(source: Bitmap, maxSide: Int): Bitmap? {
        val largest = max(source.width, source.height)
        if (largest <= maxSide) return null

        val scale = maxSide.toFloat() / largest
        val newW = max(1, (source.width * scale).roundToInt())
        val newH = max(1, (source.height * scale).roundToInt())
        return Bitmap.createScaledBitmap(source, newW, newH, true)
    }

    private fun compressWithQualityLoop(bitmap: Bitmap): ByteArray {
        var q = INITIAL_JPEG_QUALITY
        var last: ByteArray? = null
        while (q >= MIN_JPEG_QUALITY) {
            ByteArrayOutputStream().use { baos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, q, baos)
                val bytes = baos.toByteArray()
                last = bytes
                if (bytes.size <= MAX_JPEG_BYTES) return bytes
            }
            q -= QUALITY_STEP
        }
        return last ?: ByteArray(0)
    }
}
