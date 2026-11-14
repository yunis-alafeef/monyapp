package com.example.export

enum class PdfInvoiceTemplate(
    val titleAr: String,
    val subtitleAr: String
) {
    CLASSIC_FORMAL("الكلاسيكي الرسمي", "ترويسة داكنة متكاملة، جدول مؤطر، وختم رسمي"),
    MODERN_LUXURY("العصري الفاخر", "تدرج لوني حديث مع بطاقات ملخصة ناعمة"),
    DOUBLE_FRAME_VOUCHER("السند المحاسبي المؤطر", "إطار مزدوج وشعار بارز بمظهر السندات المعتمدة"),
    MINIMAL_EXECUTIVE("التنفيذي الاقتصادي", "أبيض نقي مع شريط جانبي ملون لطباعة واضحة")
}

enum class PdfDocumentKind(val titleAr: String) {
    DEBT_VOUCHER("سند قيد دين"),
    PAYMENT_RECEIPT("إيصال استلام دفعة"),
    STATEMENT("كشف حساب تفصيلي"),
    GENERAL_REPORT("التقارير المالية")
}

data class PdfColorSwatch(
    val nameAr: String,
    val primaryHex: Long,
    val accentHex: Long
)

val PdfInvoiceColorSwatches: List<PdfColorSwatch> = listOf(
    PdfColorSwatch("كحلي ملكي", 0xFF0B1B32L, 0xFF10B981L),
    PdfColorSwatch("زمردي مالي", 0xFF065F46L, 0xFF10B981L),
    PdfColorSwatch("أزرق مؤسسي", 0xFF1E3A8AL, 0xFF06B6D4L),
    PdfColorSwatch("ذهبي كهرماني", 0xFF78350FL, 0xFFD97706L),
    PdfColorSwatch("عنابي فاخر", 0xFF881337L, 0xFFF59E0BL),
    PdfColorSwatch("فيروزي حديث", 0xFF115E59L, 0xFF14B8A6L),
    PdfColorSwatch("بنفسجي تنفيذي", 0xFF4C1D95L, 0xFF8B5CF6L),
    PdfColorSwatch("فحمي اقتصادي", 0xFF1E293BL, 0xFF64748BL)
)
