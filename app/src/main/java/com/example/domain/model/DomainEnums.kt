package com.example.domain.model

enum class DebtDirection(val labelAr: String) {
    RECEIVABLE("مستحق لي"),
    PAYABLE("مستحق عليّ")
}

enum class DebtStatus(val labelAr: String) {
    OPEN("مفتوح"),
    PARTIAL("مدفوع جزئياً"),
    PAID("مدفوع بالكامل"),
    OVERDUE("متأخر"),
    SCHEDULED("مجدول"),
    SETTLED("تمت تسويته")
}

enum class PaymentMethod(val labelAr: String) {
    CASH("نقدي"),
    TRANSFER("تحويل بنكي"),
    CARD("بطاقة"),
    OTHER("أخرى")
}

enum class InstallmentStatus(val labelAr: String) {
    PENDING("قيد الانتظار"),
    PAID("مدفوع"),
    OVERDUE("متأخر")
}

enum class RepeatType(val labelAr: String) {
    ONCE("مرة واحدة"),
    DAILY("يومياً"),
    WEEKLY("أسبوعياً"),
    MONTHLY("شهرياً")
}

enum class ReminderPriority(val labelAr: String) {
    HIGH("عالية"),
    NORMAL("متوسطة"),
    LOW("منخفضة")
}

enum class MessageChannel(val labelAr: String) {
    WHATSAPP("واتساب"),
    SMS("رسالة نصية SMS"),
    BOTH("واتساب و SMS")
}

enum class AccountType(val labelAr: String) {
    PERSONAL("حساب شخصي"),
    BUSINESS("حساب تجاري")
}

enum class WorkspaceRole(
    val labelAr: String,
    val defaultAddDebt: Boolean,
    val defaultEditDebt: Boolean,
    val defaultRecordPayment: Boolean,
    val defaultViewReports: Boolean
) {
    OWNER("مالك الحساب", true, true, true, true),
    MANAGER("مدير", true, true, true, true),
    STAFF("موظف", true, false, true, false),
    VIEWER("للقراءة فقط", false, false, false, true)
}

enum class SyncStatus(val labelAr: String) {
    PENDING("بانتظار المزامنة"),
    SYNCED("متزامن"),
    FAILED("فشلت المزامنة")
}
