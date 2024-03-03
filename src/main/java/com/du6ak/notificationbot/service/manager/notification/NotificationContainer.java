package com.du6ak.notificationbot.service.manager.notification;

import com.du6ak.notificationbot.bot.Bot;
import com.du6ak.notificationbot.entity.Notification;
import com.du6ak.notificationbot.entity.Status;
import com.du6ak.notificationbot.repository.NotificationRepository;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
public class NotificationContainer implements Runnable {

    Bot bot;
    Long chatId;
    Notification notification;
    NotificationRepository notificationRepository;

    public NotificationContainer(
            Bot bot,
            Long chatId,
            Notification notification,
            NotificationRepository notificationRepository
    ) {
        this.bot = bot;
        this.chatId = chatId;
        this.notification = notification;
        this.notificationRepository = notificationRepository;
    }

    @Override
    public void run() {
        try {
            Thread.sleep(notification.getSeconds() * 1000);
        } catch (InterruptedException e) {
            log.error("Thread interrupted: " + e.getMessage());
        }
        try {
            bot.execute(
                    sendNotification()
            );
        } catch (TelegramApiException e) {
            log.error("Send notification error: " + e.getMessage());
        }
        notification.setStatus(Status.FINISHED);
        notificationRepository.save(notification);
    }

    private final BotApiMethod<?> sendNotification() {
        return SendMessage.builder()
                .chatId(chatId)
                .text("❗❗❗\n" + notification.getTitle() + "\n" + notification.getDescription())
                .build();
    }
}
