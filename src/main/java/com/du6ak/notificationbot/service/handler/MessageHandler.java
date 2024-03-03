package com.du6ak.notificationbot.service.handler;

import com.du6ak.notificationbot.bot.Bot;
import com.du6ak.notificationbot.repository.UserRepository;
import com.du6ak.notificationbot.service.contract.AbstractHandler;
import com.du6ak.notificationbot.service.manager.NotificationManager;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.interfaces.BotApiObject;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import static com.du6ak.notificationbot.entity.Action.FREE;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MessageHandler extends AbstractHandler {

    UserRepository userRepository;
    NotificationManager notificationManager;

    @Override
    public BotApiMethod<?> answer(BotApiObject object, Bot bot) throws TelegramApiException {
        var message = (Message) object;
        var user = userRepository.findByChatId(message.getChatId());
        switch (user.getAction()) {
            case FREE -> {
                return null;
            }
            case SENDING_TITLE, SENDING_DESC, SENDING_TIME -> {
                return notificationManager.answerMessage(message, bot);
            }
        }
        throw new UnsupportedOperationException();
    }
}
