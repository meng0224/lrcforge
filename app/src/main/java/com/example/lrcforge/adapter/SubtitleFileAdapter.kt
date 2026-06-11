package com.example.lrcforge.adapter

import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.lrcforge.R
import com.example.lrcforge.model.FileStatus
import com.example.lrcforge.model.SubtitleFile
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import java.util.Locale

class SubtitleFileAdapter(
    private val files: MutableList<SubtitleFile>
) : RecyclerView.Adapter<SubtitleFileAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: MaterialCardView = itemView.findViewById(R.id.cardView)
        val tvFileName: TextView = itemView.findViewById(R.id.tvFileName)
        val tvStatus: Chip = itemView.findViewById(R.id.tvStatus)
        val tvFileMeta: TextView = itemView.findViewById(R.id.tvFileMeta)
        val tvFileFormat: TextView = itemView.findViewById(R.id.tvFileFormat)
        val tvOutputFileName: TextView = itemView.findViewById(R.id.tvOutputFileName)
        val tvErrorMessage: TextView = itemView.findViewById(R.id.tvErrorMessage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_subtitle_file, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val file = files[position]

        holder.tvFileName.text = file.fileName
        holder.tvFileMeta.text = formatFileSize(file.fileSize)
        bindFormatChip(holder.tvFileFormat, extractFormat(file.fileName))

        when (file.status) {
            FileStatus.PENDING -> bindStatus(
                holder = holder,
                label = "待處理",
                chipBackgroundRes = R.color.status_pending_container,
                chipTextColorRes = R.color.status_pending_onContainer,
                cardBackgroundRes = R.color.md_theme_light_surfaceContainerLow,
                fileNameColorRes = R.color.md_theme_light_onSurface,
                metaColorRes = R.color.md_theme_light_onSurfaceVariant,
                outputText = null,
                outputTextColorRes = null,
                errorText = null,
                errorTextColorRes = null
            )

            FileStatus.INVALID -> bindStatus(
                holder = holder,
                label = "無效",
                chipBackgroundRes = R.color.status_error_container,
                chipTextColorRes = R.color.status_error_onContainer,
                cardBackgroundRes = R.color.status_error_container,
                fileNameColorRes = R.color.status_error_onContainer,
                metaColorRes = R.color.status_error_onContainer,
                outputText = null,
                outputTextColorRes = null,
                errorText = file.errorMessage,
                errorTextColorRes = R.color.status_error_onContainer
            )

            FileStatus.PROCESSING -> bindStatus(
                holder = holder,
                label = "處理中",
                chipBackgroundRes = R.color.status_processing_container,
                chipTextColorRes = R.color.status_processing_onContainer,
                cardBackgroundRes = R.color.md_theme_light_primaryContainer,
                fileNameColorRes = R.color.md_theme_light_onPrimaryContainer,
                metaColorRes = R.color.md_theme_light_onPrimaryContainer,
                outputText = null,
                outputTextColorRes = null,
                errorText = null,
                errorTextColorRes = null
            )

            FileStatus.SUCCESS -> bindStatus(
                holder = holder,
                label = "成功",
                chipBackgroundRes = R.color.status_success_container,
                chipTextColorRes = R.color.status_success_onContainer,
                cardBackgroundRes = R.color.status_success_container,
                fileNameColorRes = R.color.status_success_onContainer,
                metaColorRes = R.color.status_success_onContainer,
                outputText = file.outputFileName?.let { "輸出: $it" },
                outputTextColorRes = R.color.status_success_onContainer,
                errorText = null,
                errorTextColorRes = null
            )

            FileStatus.ERROR -> bindStatus(
                holder = holder,
                label = "失敗",
                chipBackgroundRes = R.color.status_error_container,
                chipTextColorRes = R.color.status_error_onContainer,
                cardBackgroundRes = R.color.status_error_container,
                fileNameColorRes = R.color.status_error_onContainer,
                metaColorRes = R.color.status_error_onContainer,
                outputText = null,
                outputTextColorRes = null,
                errorText = file.errorMessage,
                errorTextColorRes = R.color.status_error_onContainer
            )
        }
    }

    override fun getItemCount(): Int = files.size

    fun updateFile(position: Int, file: SubtitleFile) {
        files[position] = file
        notifyItemChanged(position)
    }

    private fun bindStatus(
        holder: ViewHolder,
        label: String,
        chipBackgroundRes: Int,
        chipTextColorRes: Int,
        cardBackgroundRes: Int,
        fileNameColorRes: Int,
        metaColorRes: Int,
        outputText: String?,
        outputTextColorRes: Int?,
        errorText: String?,
        errorTextColorRes: Int?
    ) {
        val context = holder.itemView.context
        holder.tvStatus.text = label
        holder.tvStatus.chipBackgroundColor = ColorStateList.valueOf(
            ContextCompat.getColor(context, chipBackgroundRes)
        )
        holder.tvStatus.setTextColor(ContextCompat.getColor(context, chipTextColorRes))
        holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, cardBackgroundRes))
        holder.cardView.strokeWidth = 0
        holder.tvFileName.setTextColor(ContextCompat.getColor(context, fileNameColorRes))
        holder.tvFileMeta.setTextColor(ContextCompat.getColor(context, metaColorRes))
        holder.tvFileFormat.setTextColor(ContextCompat.getColor(context, chipTextColorRes))

        if (outputText != null) {
            holder.tvOutputFileName.text = outputText
            val outputColor = outputTextColorRes ?: metaColorRes
            holder.tvOutputFileName.setTextColor(ContextCompat.getColor(context, outputColor))
            holder.tvOutputFileName.visibility = View.VISIBLE
        } else {
            holder.tvOutputFileName.visibility = View.GONE
        }

        if (errorText != null) {
            holder.tvErrorMessage.text = errorText
            val errorColor = errorTextColorRes ?: R.color.status_error_onContainer
            holder.tvErrorMessage.setTextColor(ContextCompat.getColor(context, errorColor))
            holder.tvErrorMessage.visibility = View.VISIBLE
        } else {
            holder.tvErrorMessage.visibility = View.GONE
        }
    }

    private fun bindFormatChip(label: TextView, format: String) {
        val context = label.context
        label.text = format
        val background = (label.background?.mutate() as? GradientDrawable) ?: GradientDrawable()
        background.cornerRadius = context.resources.displayMetrics.density * 18
        background.setColor(ContextCompat.getColor(context, R.color.md_theme_light_primaryContainer))
        label.background = background
        label.setTextColor(ContextCompat.getColor(context, R.color.md_theme_light_onPrimaryContainer))
    }

    private fun extractFormat(fileName: String): String {
        return fileName.substringAfterLast('.', "")
            .ifEmpty { "未知" }
            .uppercase(Locale.ROOT)
    }

    private fun formatFileSize(sizeInBytes: Long): String {
        if (sizeInBytes <= 0L) {
            return "0 B"
        }
        if (sizeInBytes < 1024L) {
            return "$sizeInBytes B"
        }
        val sizeInKb = sizeInBytes / 1024.0
        if (sizeInKb < 1024.0) {
            return String.format(Locale.US, "%.0f KB", sizeInKb)
        }
        return String.format(Locale.US, "%.1f MB", sizeInKb / 1024.0)
    }
}
