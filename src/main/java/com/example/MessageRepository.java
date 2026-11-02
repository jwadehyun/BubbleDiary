package com.example;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Long> {

    // Summaries leave out the photo bytes and count the replies instead of loading them.
    String SUMMARY = """
            select new com.example.MessageSummary(m.messageId, m.text, m.imageType, m.createdAt, m.status, m.error,
                (select count(r) from Reply r where r.message = m))
            from Message m
            """;

    @Query(SUMMARY + "where m.user = :user order by m.createdAt")
    List<MessageSummary> findSummaries(User user);

    @Query(SUMMARY + "where m.messageId = :id and m.user = :user")
    Optional<MessageSummary> findSummary(Long id, User user);

    Optional<Message> findByMessageIdAndUser(Long messageId, User user);

    long countByUserAndCreatedAtAfter(User user, Instant since);

    @Query("select m.text from Message m where m.user = :user and m.createdAt < :before order by m.createdAt desc limit 3")
    List<String> findRecentTexts(User user, Instant before);

    @Modifying
    @Query("delete from Message m where m.messageId = :id and m.user = :user")
    int deleteOwned(Long id, User user);

    @Modifying
    @Query("delete from Message m where m.user = :user")
    void deleteAllOwned(User user);

    // Updates nothing if the user deleted the message while its replies were being written.
    @Modifying
    @Query("update Message m set m.status = :status, m.error = :error where m.messageId = :id")
    int updateStatus(Long id, MessageStatus status, String error);

    @Modifying
    @Query("update Message m set m.status = :to, m.error = :error where m.status = :from")
    int updateAllStatus(MessageStatus from, MessageStatus to, String error);
}
