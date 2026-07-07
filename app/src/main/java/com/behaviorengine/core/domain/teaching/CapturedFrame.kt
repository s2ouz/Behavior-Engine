package com.behaviorengine.core.domain.teaching

/**
 * One raw frame handed from [ScreenCaptureManager] to [TeachingRecorder], already WEBP-encoded.
 * Transient only — never stored in a [TeachingSession] or serialized itself; [TeachingRecorder]
 * writes [bytes] to disk via [TeachingStorage] and keeps only the resulting [ScreenFrame] metadata.
 * Not `@Serializable`/`@Immutable`: this is a short-lived value passed once through a `Flow` and
 * discarded, not stored or compared, so neither annotation would do anything useful here.
 */
data class CapturedFrame(
    val bytes: ByteArray,
    val width: Int,
    val height: Int,
    val timestampMillis: Long,
    val rotation: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CapturedFrame) return false
        return bytes.contentEquals(other.bytes) &&
            width == other.width &&
            height == other.height &&
            timestampMillis == other.timestampMillis &&
            rotation == other.rotation
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + timestampMillis.hashCode()
        result = 31 * result + rotation
        return result
    }
}
