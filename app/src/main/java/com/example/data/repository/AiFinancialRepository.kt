package com.example.data.repository

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

data class AiHistoryTurn(
    val isUser: Boolean,
    val text: String
)

class AiFinancialRepository {
    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    """
                    أنت المساعد المالي الذكي لتطبيق "حساباتي" لإدارة الديون والأقساط والمحافظ المالية.
                    قواعد صارمة:
                    1. أجب باللغة العربية الفصحى الواضحة والمختصرة وبنبرة مالية مهنية.
                    2. اعتمد حصرياً على البيانات الرقمية الحقيقية المقدمة لك في سياق الحساب.
                    3. يُمنع منعاً باتاً اختراع أي أرقام أو أسماء غير موجودة في السياق.
                    4. إذا لم تتوفر معلومة محددة في البيانات، اذكر ذلك بوضوح.
                    5. قدّم ملخصاً منظماً مع نقاط واضحة وتوصيات عملية لتحصيل المستحقات أو جدولة الالتزامات.
                    """.trimIndent()
                )
            }
        )
    }

    fun streamFinancialAnswer(
        question: String,
        accountContextSummary: String,
        chatHistory: List<AiHistoryTurn> = emptyList()
    ): Flow<AiStreamEvent> = flow {
        emit(AiStreamEvent.Loading)
        val prompt = """
            بيانات الحساب الفعلية الموثقة من قاعدة البيانات (محدّثة الآن):
            $accountContextSummary

            سؤال المستخدم:
            $question
        """.trimIndent()

        val accumulated = StringBuilder()
        try {
            withTimeout(60_000L) {
                val flowStream = if (chatHistory.isEmpty()) {
                    model.generateContentStream(prompt)
                } else {
                    val formattedHistory = chatHistory.takeLast(8).map { turn ->
                        content(if (turn.isUser) "user" else "model") {
                            text(turn.text)
                        }
                    }
                    model.startChat(history = formattedHistory).sendMessageStream(prompt)
                }
                flowStream.collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        accumulated.append(piece)
                        emit(AiStreamEvent.Streaming(accumulated.toString()))
                    }
                }
            }
            if (accumulated.isNotBlank()) {
                emit(AiStreamEvent.Success(accumulated.toString()))
            } else {
                emit(AiStreamEvent.Error("لم يتم استلام إجابة من المساعد الذكي. يرجى المحاولة مرة أخرى."))
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI request timed out", e)
            emit(AiStreamEvent.Error("انتهت مهلة الاتصال بالمساعد الذكي (60 ثانية). يرجى المحاولة لاحقاً."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI Logic inference failed", e)
            val detail = e.localizedMessage?.takeIf { it.isNotBlank() }
            emit(
                AiStreamEvent.Error(
                    if (detail != null) "تعذر الاتصال بخدمة الذكاء الاصطناعي: $detail"
                    else "تعذر الاتصال بالمساعد المالي الذكي. تأكد من الاتصال بالإنترنت."
                )
            )
        }
    }

    private companion object {
        const val TAG = "AiFinancialRepo"
    }
}

sealed interface AiStreamEvent {
    data object Loading : AiStreamEvent
    data class Streaming(val partialText: String) : AiStreamEvent
    data class Success(val fullText: String) : AiStreamEvent
    data class Error(val messageAr: String) : AiStreamEvent
}
