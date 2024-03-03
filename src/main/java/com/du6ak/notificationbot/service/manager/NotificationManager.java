package com.du6ak.notificationbot.service.manager;

import com.du6ak.notificationbot.bot.Bot;
import com.du6ak.notificationbot.entity.Action;
import com.du6ak.notificationbot.entity.Notification;
import com.du6ak.notificationbot.entity.Status;
import com.du6ak.notificationbot.entity.User;
import com.du6ak.notificationbot.repository.NotificationRepository;
import com.du6ak.notificationbot.repository.UserRepository;
import com.du6ak.notificationbot.service.contract.AbstractManager;
import com.du6ak.notificationbot.service.contract.CommandListener;
import com.du6ak.notificationbot.service.contract.MessageListener;
import com.du6ak.notificationbot.service.contract.QueryListener;
import com.du6ak.notificationbot.service.factory.KeyboardFactory;
import com.du6ak.notificationbot.service.manager.notification.NotificationContainer;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import static com.du6ak.notificationbot.data.CallbackData.*;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationManager extends AbstractManager implements QueryListener, CommandListener, MessageListener {

    KeyboardFactory keyboardFactory;
    NotificationRepository notificationRepository;
    UserRepository userRepository;

    @Override
    public BotApiMethod<?> mainMenu(Message message, Bot bot) {
        return SendMessage.builder()
                .chatId(message.getChatId())
                .text("Настройте уведомление")
                .replyMarkup(
                        editNotificationReplyMarkup(
                                String.valueOf(
                                        userRepository.findByChatId(message.getChatId())
                                                .getCurrent()
                                )
                        )
                )
                .build();
    }

    @Override
    public BotApiMethod<?> mainMenu(CallbackQuery query, Bot bot) {
        return EditMessageText.builder()
                .chatId(query.getMessage().getChatId())
                .messageId(query.getMessage().getMessageId())
                .text("###")
                .replyMarkup(
                        keyboardFactory.createInlineKeyboard(
                                List.of("Добавить уведомление"),
                                List.of(1),
                                List.of(notification_new.name())
                        )
                )
                .build();
    }

    @Override
    public BotApiMethod<?> answerCommand(Message message, Bot bot) {
        return null;
    }

    @Override
    public BotApiMethod<?> answerMessage(Message message, Bot bot) throws TelegramApiException {
        var user = userRepository.findByChatId(message.getChatId());
        bot.execute(
                DeleteMessage.builder()
                        .chatId(message.getChatId())
                        .messageId(message.getMessageId() - 1)
                        .build()
        );
        switch (user.getAction()) {
            case SENDING_TIME -> {
                return editTime(message, user, bot);
            }
            case SENDING_DESC -> {
                return editDescription(message, user, bot);
            }
            case SENDING_TITLE -> {
                return editTitle(message, user, bot);
            }
        }
        return null;
    }

    private BotApiMethod<?> editTitle(Message message, User user, Bot bot) {
        var notification = notificationRepository.findById(user.getCurrent()).orElseThrow();
        notification.setTitle(message.getText());
        notificationRepository.save(notification);

        user.setAction(Action.FREE);
        userRepository.save(user);
        return mainMenu(message, bot);
    }

    private BotApiMethod<?> editDescription(Message message, User user, Bot bot) {
        var notification = notificationRepository.findById(user.getCurrent()).orElseThrow();
        notification.setDescription(message.getText());
        notificationRepository.save(notification);

        user.setAction(Action.FREE);
        userRepository.save(user);
        return mainMenu(message, bot);
    }

    private BotApiMethod<?> editTime(Message message, User user, Bot bot) {
        var notification = notificationRepository.findById(user.getCurrent()).orElseThrow();

        var messageText = message.getText().strip();
        var pattern = Pattern.compile("^[0-9]{2}:[0-9]{2}:[0-9]{2}$").matcher(messageText);
        if (pattern.matches()) {
            var nums = messageText.split(":");
            int seconds = Integer.parseInt(nums[0]) * 3600 + Integer.parseInt(nums[1]) * 60 + Integer.parseInt(nums[2]);
            notification.setSeconds((long)seconds);
        } else {
            return SendMessage.builder()
                    .text("Некорректное время")
                    .chatId(message.getChatId())
                    .replyMarkup(
                            keyboardFactory.createInlineKeyboard(
                                    List.of("\uD83D\uDD19 Назад"),
                                    List.of(1),
                                    List.of(notification_back_.name() + user.getCurrent())
                            )
                    )
                    .build();
        }
        notificationRepository.save(notification);
        user.setAction(Action.FREE);
        userRepository.save(user);
        return mainMenu(message, bot);
    }

    @Override
    public BotApiMethod<?> answerQuery(CallbackQuery query, String[] words, Bot bot) throws TelegramApiException {
        switch (words.length) {
            case 2 -> {
                switch (words[1]) {
                    case "main" -> {
                        return mainMenu(query, bot);
                    }
                    case "new" -> {
                        return newNotification(query, bot);
                    }
                }
            }
            case 3 -> {
                switch (words[1]) {
                    case "back" -> {
                        return editPage(query, words[2]);
                    }
                    case "done" -> {
                        return sendNotification(query, words[2], bot);
                    }
                }
            }
            case 4 -> {
                switch (words[1]) {
                    case "edit" -> {
                        switch (words[2]) {
                            case "title" -> {
                                return askTitle(query, words[3]);
                            }
                            case "desc" -> {
                                return askDescription(query, words[3]);
                            }
                            case "time" -> {
                                return askSeconds(query, words[3]);
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private BotApiMethod<?> sendNotification(CallbackQuery query, String id, Bot bot) throws TelegramApiException {
        var notification = notificationRepository.findById(UUID.fromString(id)).orElseThrow();
        if (notification.getTitle() == null  || notification.getTitle().isBlank() || notification.getSeconds() == null) {
            return AnswerCallbackQuery.builder()
                    .callbackQueryId(query.getId())
                    .text("Заполните обязательные значения: Заголовок и Время")
                    .build();
        }
        bot.execute(
                AnswerCallbackQuery.builder()
                        .text("Уведомление придет к вам через " + notification.getSeconds() + " секунд \uD83D\uDC40")
                        .callbackQueryId(query.getId())
                        .build()
        );
        notification.setStatus(Status.WAITING);
        notificationRepository.save(notification);
        Thread.startVirtualThread(
                new NotificationContainer(
                        bot,
                        query.getMessage().getChatId(),
                        notification,
                        notificationRepository
                )
        );
        return EditMessageText.builder()
                .text("✅ Успешно")
                .chatId(query.getMessage().getChatId())
                .messageId(query.getMessage().getMessageId())
                .replyMarkup(
                        keyboardFactory.createInlineKeyboard(
                                List.of("На главную"),
                                List.of(1),
                                List.of(main.name())

                        )
                )
                .build();
    }

    private BotApiMethod<?> editPage(CallbackQuery query, String id) {
        return EditMessageText.builder()
                .chatId(query.getMessage().getChatId())
                .messageId(query.getMessage().getMessageId())
                .text("Настройте уведомление")
                .replyMarkup(editNotificationReplyMarkup(id))
                .build();
    }

    private BotApiMethod<?> askSeconds(CallbackQuery query, String id) {
        var user = userRepository.findByChatId(query.getMessage().getChatId());
        user.setAction(Action.SENDING_TIME);
        user.setCurrent(UUID.fromString(id));
        userRepository.save(user);

        Date now = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        String date = sdf.format(now);
        var time = date.split(":");

        return EditMessageText.builder()
//                .text("⚡\uFE0F Введите время, по прошествии которого хотите получить напоминание\nФормат - ЧЧ:ММ:СС\nНапример - (01:30:00) - полтора часа")
                .text("⚡\uFE0F Выберите время, через которое хотите получить уведомление:")
                .messageId(query.getMessage().getMessageId())
                .chatId(query.getMessage().getChatId())
                .replyMarkup(
                        keyboardFactory.createInlineKeyboard(
                                List.of(
                                        "+","+","+",
                                        time[0] + " : " + time[1] + " : " + time[2],
                                        "-","-","-",
                                        "✅","\uD83D\uDD19 Назад"),
                                List.of(9),
                                List.of(notification_back_.name() + id)
                        )
                )
                .build();
    }

    private BotApiMethod<?> askDescription(CallbackQuery query, String id) {
        var user = userRepository.findByChatId(query.getMessage().getChatId());
        user.setAction(Action.SENDING_DESC);
        user.setCurrent(UUID.fromString(id));
        userRepository.save(user);
        return EditMessageText.builder()
                .text("⚡\uFE0F Добавьте или измените описание, просто напишите в чат тест, который бы хотели получить")
                .messageId(query.getMessage().getMessageId())
                .chatId(query.getMessage().getChatId())
                .replyMarkup(
                        keyboardFactory.createInlineKeyboard(
                                List.of("\uD83D\uDD19 Назад"),
                                List.of(1),
                                List.of(notification_back_.name() + id)
                        )
                )
                .build();
    }

    private BotApiMethod<?> askTitle(CallbackQuery query, String id) {
        var user = userRepository.findByChatId(query.getMessage().getChatId());
        user.setAction(Action.SENDING_TITLE);
        user.setCurrent(UUID.fromString(id));
        userRepository.save(user);
        return EditMessageText.builder()
                .text("⚡\uFE0F Опишите краткий заголовок в следующем сообщение, чтобы вам было сразу понятно, что я вам напоминаю")
                .messageId(query.getMessage().getMessageId())
                .chatId(query.getMessage().getChatId())
                .replyMarkup(
                        keyboardFactory.createInlineKeyboard(
                                List.of("\uD83D\uDD19 Назад"),
                                List.of(1),
                                List.of(notification_back_.name() + id)
                        )
                )
                .build();
    }

    private BotApiMethod<?> newNotification(CallbackQuery query, Bot bot) {
        var user = userRepository.findByChatId(query.getMessage().getChatId());
        String id = String.valueOf(notificationRepository.save(
                Notification.builder()
                        .user(user)
                        .status(Status.BUILDING)
                        .build()
        ).getId());
        return EditMessageText.builder()
                .chatId(query.getMessage().getChatId())
                .messageId(query.getMessage().getMessageId())
                .text("Настройте уведомление")
                .replyMarkup(editNotificationReplyMarkup(id))
                .build();

    }

    private InlineKeyboardMarkup editNotificationReplyMarkup(String id) {
        List<String> text = new ArrayList<>();
        var notification = notificationRepository.findById(UUID.fromString(id)).orElseThrow();
        if (notification.getTitle() != null && !notification.getTitle().isBlank()) {
            text.add("✅ Заголовок");
        } else {
            text.add("❌ Заголовок");
        }
        if (notification.getSeconds() != null && notification.getSeconds() != 0) {
            text.add("✅ Время");
        } else {
            text.add("❌ Время");
        }
        if (notification.getDescription() != null && !notification.getDescription().isBlank()) {
            text.add("✅ Описание");
        } else {
            text.add("❌ Описание");
        }
        text.add("\uD83D\uDD19 Главная");
        text.add("\uD83D\uDD50 Готово");
        return keyboardFactory.createInlineKeyboard(
                text,
                List.of(2, 1, 2),
                List.of(
                        notification_edit_title_.name() + id, notification_edit_time_.name() + id,
                        notification_edit_desc_.name() + id,
                        main.name(), notification_done_.name() + id
                )
        );
    }
}
