package com.example;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ReplyRepository extends JpaRepository<Reply, Long> {

    @Query("select r from Reply r where r.message.messageId = :messageId")
    List<Reply> findByMessageId(Long messageId);

    @Modifying
    @Query("delete from Reply r where r.message.messageId = :messageId and r.ai = true")
    void deleteAiReplies(Long messageId);
}
