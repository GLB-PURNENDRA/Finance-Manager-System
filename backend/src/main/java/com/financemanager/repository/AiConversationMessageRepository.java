package com.financemanager.repository;

import com.financemanager.entity.AiConversationMessage;
import com.financemanager.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface AiConversationMessageRepository extends JpaRepository<AiConversationMessage, Long> {

    List<AiConversationMessage> findTop20ByUserOrderByCreatedAtDesc(User user);

    List<AiConversationMessage> findByUserOrderByCreatedAtAsc(User user);

    @Transactional
    @Modifying
    @Query("DELETE FROM AiConversationMessage m WHERE m.user = :user")
    void deleteByUser(User user);
}
