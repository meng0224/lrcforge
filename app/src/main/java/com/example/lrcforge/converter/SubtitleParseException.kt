package com.example.lrcforge.converter

import com.example.lrcforge.R

class SubtitleParseException(
    val format: String,
    val lineNumber: Int,
    val reason: Reason
) : IllegalArgumentException("$format line $lineNumber: ${reason.name}") {
    enum class Reason(val messageResId: Int) {
        INVALID_TIMESTAMP(R.string.parse_invalid_timestamp),
        INVALID_TIME_RANGE(R.string.parse_invalid_time_range),
        INVALID_CUE(R.string.parse_invalid_cue),
        INVALID_ASS_FORMAT(R.string.parse_invalid_ass_format),
        INVALID_ASS_DIALOGUE(R.string.parse_invalid_ass_dialogue),
        INVALID_SYNC(R.string.parse_invalid_sync),
        MISSING_FRAME_RATE(R.string.parse_missing_frame_rate),
        INVALID_FRAME_RATE(R.string.parse_invalid_frame_rate),
        INVALID_SUB_ENTRY(R.string.parse_invalid_sub_entry)
    }
}
