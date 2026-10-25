package com.financemanager.config;

import com.financemanager.entity.User;
import com.financemanager.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FamilyLinkRepository familyLinkRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private SavingsGoalRepository savingsGoalRepository;

    @Autowired
    private InvestmentRepository investmentRepository;

    @Autowired
    private SipRecordRepository sipRecordRepository;

    @Autowired
    private AiConversationMessageRepository aiConversationMessageRepository;

    @Autowired
    private InvestorProfileRepository investorProfileRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Override
    public void run(String... args) {
        cleanupLegacyDemoUser("student@university.edu");
        cleanupLegacyDemoUser("parent@family.com");
        log.info("Finance System Manager initialized. Demo accounts removed. Production authentication active.");
    }

    private void cleanupLegacyDemoUser(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            log.info("Purging legacy demo user and associated demo records: {}", email);
            try {
                aiConversationMessageRepository.deleteByUser(user);
                investorProfileRepository.findByStudent(user).ifPresent(investorProfileRepository::delete);
                auditEventRepository.deleteAll(auditEventRepository.findByActorOrderByTimestampDesc(user));
                sipRecordRepository.deleteAll(sipRecordRepository.findByStudentOrderByCreatedAtDesc(user));
                investmentRepository.deleteAll(investmentRepository.findByStudentOrderByCreatedAtDesc(user));
                savingsGoalRepository.deleteAll(savingsGoalRepository.findByStudentOrderByCreatedAtDesc(user));
                budgetRepository.deleteAll(budgetRepository.findByStudentOrderByPeriodMonthDesc(user));
                transactionRepository.deleteAll(transactionRepository.findByOwnerOrderByTransactionDateDescCreatedAtDesc(user));
                familyLinkRepository.deleteAll(familyLinkRepository.findByStudentOrderByCreatedAtDesc(user));
                familyLinkRepository.deleteAll(familyLinkRepository.findByParentOrderByCreatedAtDesc(user));
                userRepository.delete(user);
                log.info("Successfully purged legacy demo user: {}", email);
            } catch (Exception e) {
                log.warn("Could not completely purge legacy demo account {}: {}", email, e.getMessage());
            }
        }
    }
}
