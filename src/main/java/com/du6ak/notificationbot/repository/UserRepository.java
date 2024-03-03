package com.du6ak.notificationbot.repository;

import com.du6ak.notificationbot.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    User findByChatId(Long chatId);
    boolean existsByChatId(Long chatId);
}
