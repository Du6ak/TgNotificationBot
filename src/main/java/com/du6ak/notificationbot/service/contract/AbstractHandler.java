package com.du6ak.notificationbot.service.contract;

import com.du6ak.notificationbot.bot.Bot;
import org.telegram.telegrambots.meta.api.interfaces.BotApiObject;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public abstract class AbstractHandler {
    public abstract BotApiMethod<?> answer(BotApiObject object, Bot bot) throws TelegramApiException;
}
