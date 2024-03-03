package com.du6ak.notificationbot.service;

import com.du6ak.notificationbot.bot.Bot;
import com.du6ak.notificationbot.entity.Action;
import com.du6ak.notificationbot.entity.User;
import com.du6ak.notificationbot.repository.UserRepository;
import com.du6ak.notificationbot.service.handler.CallbackQueryHandler;
import com.du6ak.notificationbot.service.handler.CommandHandler;
import com.du6ak.notificationbot.service.handler.MessageHandler;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UpdateDispatcher {

    MessageHandler messageHandler;
    CommandHandler commandHandler;
    CallbackQueryHandler queryHandler;
    UserRepository userRepository;

    public BotApiMethod<?> distribute(Update update, Bot bot) throws TelegramApiException {
        if (update.hasCallbackQuery()) {
            checkUser(update.getCallbackQuery().getMessage().getChatId());
            return queryHandler.answer(update.getCallbackQuery(), bot);
        }
        if (update.hasMessage()) {
            var message = update.getMessage();
            checkUser(message.getChatId());
            if (message.hasText()) {
                if (message.getText().charAt(0) == '/') {
                    return commandHandler.answer(message, bot);
                }
                return messageHandler.answer(message, bot);
            }
        }
        log.warn("Unsupported update message: " + update);
        return null;
    }

    private void checkUser(Long chatId){
        if (userRepository.existsByChatId(chatId)){
            return;
        }
        userRepository.save(
                User.builder()
                        .action(Action.FREE)
                        .registeredAt(LocalDateTime.now())
                        .chatId(chatId)
                        .firstName("zxc")
                        .build()
        );
    }

}
