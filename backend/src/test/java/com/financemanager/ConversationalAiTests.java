package com.financemanager;

import com.financemanager.dto.AiQuestionResponseDto;
import com.financemanager.dto.ChatMessageDto;
import com.financemanager.entity.*;
import com.financemanager.exception.ForbiddenException;
import com.financemanager.repository.*;
import com.financemanager.service.GeminiClientService;
import com.financemanager.service.IntelligenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class ConversationalAiTests {

    @Autowired
    private IntelligenceService intelligenceService;

    @Autowired
    private GeminiClientService geminiClientService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FamilyLinkRepository familyLinkRepository;

    private User student;
    private User parent;

    @BeforeEach
    void setUp() {
        student = userRepository.findByEmail("student@university.edu").orElseThrow();
        parent = userRepository.findByEmail("parent@family.com").orElseThrow();
        intelligenceService.clearConversationHistory(student);
        intelligenceService.clearConversationHistory(parent);
    }

    @Test
    @DisplayName("1. User asks 'Hello, who are you?' -> Returns assistant identity")
    void testHelloWhoAreYou() {
        AiQuestionResponseDto res = intelligenceService.askMoneyQuestion(student, "Hello, who are you?");
        assertNotNull(res);
        assertNotNull(res.getAnswer());
        assertFalse(res.getAnswer().isBlank());
        assertTrue(res.getAnswer().toLowerCase().contains("finance") ||
                   res.getAnswer().toLowerCase().contains("assistant") ||
                   res.getAnswer().toLowerCase().contains("shyam") ||
                   res.getAnswer().toLowerCase().contains("ai"));
    }

    @Test
    @DisplayName("2. User asks 'Explain compound interest.' -> Returns compound interest explanation")
    void testExplainCompoundInterest() {
        AiQuestionResponseDto res = intelligenceService.askMoneyQuestion(student, "Explain compound interest with a simple example.");
        assertNotNull(res);
        assertNotNull(res.getAnswer());
        assertTrue(res.getAnswer().toLowerCase().contains("interest") || res.getAnswer().toLowerCase().contains("compound"));
    }

    @Test
    @DisplayName("3. User asks 'What is the difference between a stock and a mutual fund?' -> Returns stock vs mutual fund comparison")
    void testStockVsMutualFund() {
        AiQuestionResponseDto res = intelligenceService.askMoneyQuestion(student, "What is the difference between a stock and a mutual fund?");
        assertNotNull(res);
        assertNotNull(res.getAnswer());
        // Must be generated dynamically by real LLM, not the old static SIP commitments table
        assertTrue(res.isRealLlm(), "Must be processed by real LLM");
        assertFalse(res.getAnswer().startsWith("### 📈 SIP Commitments"), "Must not return irrelevant hardcoded SIP record table");
        assertTrue(res.getAnswer().toLowerCase().contains("stock") && res.getAnswer().toLowerCase().contains("mutual fund"));
    }

    @Test
    @DisplayName("4. User asks follow-up referring to previous answer -> Uses conversation history")
    void testFollowUpToPreviousAnswer() {
        // Step 1: Ask about compound interest
        intelligenceService.askMoneyQuestion(student, "Explain compound interest with a simple example.");

        // Step 2: Follow up
        AiQuestionResponseDto followUp = intelligenceService.askMoneyQuestion(student, "What was my previous question?");
        assertNotNull(followUp);
        assertNotNull(followUp.getAnswer());
        assertTrue(followUp.getAnswer().toLowerCase().contains("compound interest") ||
                   followUp.getAnswer().toLowerCase().contains("previous question") ||
                   followUp.getAnswer().toLowerCase().contains("you asked"));
    }

    @Test
    @DisplayName("5. Verify all responses are distinct and relevant (no identical copies)")
    void testAllResponsesAreDistinctAndRelevant() {
        AiQuestionResponseDto r1 = intelligenceService.askMoneyQuestion(student, "Hello, introduce yourself.");
        AiQuestionResponseDto r2 = intelligenceService.askMoneyQuestion(student, "Explain compound interest with a simple example.");
        AiQuestionResponseDto r3 = intelligenceService.askMoneyQuestion(student, "What is the difference between a stock and a mutual fund?");

        assertNotEquals(r1.getAnswer(), r2.getAnswer(), "Questions must have different answers");
        assertNotEquals(r2.getAnswer(), r3.getAnswer(), "Questions must have different answers");
        assertNotEquals(r1.getAnswer(), r3.getAnswer(), "Questions must have different answers");
    }

    @Test
    @DisplayName("6. Missing API credentials handled truthfully with status indication")
    void testMissingCredentialsTruthfullyHandled() {
        geminiClientService.setApiKey("none"); // explicitly disable key
        AiQuestionResponseDto res = intelligenceService.askMoneyQuestion(student, "What is inflation?");
        assertNotNull(res);
        assertFalse(res.isRealLlm(), "Must not claim real LLM is active when no key is configured");
        assertTrue(res.getModelUsed().contains("Setup Required") || res.getModelUsed().contains("No Provider"));
        assertTrue(res.getAnswer().contains("Setup Required"));
    }

    @Test
    @DisplayName("7. Provider error (e.g. invalid key) is truthfully returned rather than silent fallback")
    void testProviderErrorHandledTruthfully() {
        // Configure an invalid key
        geminiClientService.setApiKey("INVALID_TEST_KEY_FOR_TESTING");

        AiQuestionResponseDto res = intelligenceService.askMoneyQuestion(student, "Tell me about mutual funds.");
        assertNotNull(res);
        assertFalse(res.isRealLlm(), "Failed API call must not claim successful real LLM completion");
        // Must clearly indicate provider error
        assertTrue(res.getAnswer().contains("AI Provider Error") || res.getAnswer().contains("HTTP"));
        assertTrue(res.getModelUsed().contains("Error"));
    }

    @Test
    @DisplayName("8. Parent authorized read-only Q&A with active link")
    void testParentAuthorizedAccess() {
        AiQuestionResponseDto res = intelligenceService.askMoneyQuestion(parent, "Where is most of the student's money going?");
        assertNotNull(res);
        assertNotNull(res.getAnswer());
        assertTrue(res.getAnswer().toLowerCase().contains("expense") ||
                   res.getAnswer().toLowerCase().contains("spending") ||
                   res.getAnswer().toLowerCase().contains("hostel") ||
                   res.getAnswer().toLowerCase().contains("food") ||
                   res.getAnswer().toLowerCase().contains("money"));
    }

    @Test
    @DisplayName("9. Parent access blocked immediately if link is revoked")
    void testParentAccessBlockedWhenRevoked() {
        FamilyLink link = familyLinkRepository.findByParentAndStudentAndStatus(parent, student, LinkStatus.ACTIVE).orElseThrow();
        link.setStatus(LinkStatus.REVOKED);
        familyLinkRepository.save(link);

        assertThrows(ForbiddenException.class, () -> {
            intelligenceService.askMoneyQuestion(parent, "What are the latest expenses?");
        });
    }

    @Test
    @DisplayName("10. Cross-user conversation history isolation")
    void testCrossUserConversationIsolation() {
        intelligenceService.askMoneyQuestion(student, "How much did I spend on books?");
        intelligenceService.askMoneyQuestion(parent, "What is the total expenditure?");

        List<ChatMessageDto> studentHistory = intelligenceService.getConversationHistory(student);
        List<ChatMessageDto> parentHistory = intelligenceService.getConversationHistory(parent);

        assertEquals(2, studentHistory.size());
        assertEquals(2, parentHistory.size());

        assertTrue(studentHistory.get(0).getContent().contains("books"));
        assertTrue(parentHistory.get(0).getContent().contains("total expenditure"));
    }
}
