package com.du6ak.notificationbot.bot;

import com.du6ak.notificationbot.configuration.TgProperties;
import com.du6ak.notificationbot.service.UpdateDispatcher;
import lombok.AccessLevel;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramWebhookBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class Bot extends TelegramWebhookBot {

    TgProperties tgProperties;
    UpdateDispatcher dispatcher;

    public Bot(TgProperties tgProperties, UpdateDispatcher dispatcher) {
        super(tgProperties.getToken());
        this.tgProperties = tgProperties;
        this.dispatcher = dispatcher;
    }

    @SneakyThrows
    @Override
    public BotApiMethod<?> onWebhookUpdateReceived(Update update) {
        return dispatcher.distribute(update, this);
    }

    @Override
    public String getBotPath() {
        return tgProperties.getUrl();
    }

    @Override
    public String getBotUsername() {
        return tgProperties.getName();
    }
}
