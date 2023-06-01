package io.proj3ct.SpringLawandaFarmBot.service;

import io.proj3ct.SpringLawandaFarmBot.config.BotConfig;
import io.proj3ct.SpringLawandaFarmBot.model.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    final BotConfig config;

    static final String HELP_TEXT = "This bot is created to pass the Programming Engineering course";

    //Костылики
    private final Program program = new Program();

    public TelegramBot(BotConfig config){
        this.config = config;
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "Главное меню"));
        listOfCommands.add(new BotCommand("/about", "Информация о нашей компании"));
        listOfCommands.add(new BotCommand("/help", "Просто \"help\" для красоты"));
        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Error settings bot's command list: " + e.getMessage());
        }
    }

    @SneakyThrows
    @Override
    public void onUpdateReceived(Update update) {
        System.out.println(program);
        if(update.hasMessage() && update.getMessage().hasText()) {
            long chatId = update.getMessage().getChatId();
            String messageText = update.getMessage().getText();
            String userName = update.getMessage().getChat().getUserName();
            switch (messageText){
                case "/start" -> {
                    BotDBConnection.POST("INSERT INTO Users VALUES ('" + userName
                            + "', '', '', '', " + chatId + ", '" + States.MAIN_MENU + "') "
                            + "ON CONFLICT (\"UserName\") DO " +
                            "UPDATE SET currentstate='" + States.MAIN_MENU + "'");
                    startCommandReceived(chatId);
                }
                case "/about" -> aboutCommandReceived(chatId);
                case "/help" -> sendMessage(chatId, HELP_TEXT);
                default -> {
                    ResultSet resultSet = BotDBConnection.SELECT(
                            "SELECT * FROM users WHERE \"ChatId\"=" + chatId);
                    String currentstate = States.MAIN_MENU;
                    while (resultSet.next()) {
                        currentstate = resultSet.getString("currentstate");
                    }

                    switch (currentstate){
                        case States.MAIN_MENU -> mainMenuSelected(messageText, chatId);
                        case States.MAP -> mapMenuSelected(messageText, chatId);
                        case States.CATALOG -> catalogMenuSelected(messageText, chatId, userName);

                        case States.SIGN_UP -> suMenuSelected(messageText, chatId);
                        case States.SU_DAY -> suDaySelected(messageText, chatId);
                        case States.SU_TIME -> suTimeSelected(messageText, chatId);
                        case States.SU_PAY -> suPaySelected(messageText, chatId);

                        case States.TRASH -> trashMenuSelected(messageText, chatId);
                        case States.TRASH_DELETING -> trashDeletingMenuSelected(messageText, chatId);
                        case States.TRASH_PAYING -> trashPayingMenuSelected(messageText, chatId);

                        case States.LK -> lkMenuSelected(messageText, chatId);

                    }

                }
            }
        } else if (update.hasCallbackQuery()) {
            long messageId = update.getCallbackQuery().getMessage().getMessageId();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            EditMessageText message = new EditMessageText();
            message.setChatId(chatId);
            message.setMessageId((int) messageId);
        }

    }

    @SneakyThrows
    private void aboutCommandReceived(long chatId) {
        SendPhoto photo = new SendPhoto();

        photo.setPhoto(new InputFile(new File("images/uncle_Olya.jpg")));
        photo.setChatId(chatId);
        photo.setCaption("""
                Привет! Меня зовут Ольга, и я расскажу Вам о нашей лавандовой ферме.
                
                Наша лавандовая ферма - это 12 гектаров Прованса в 40 км. от Нижнего Новгорода. Фотосессии, прогулки в лавандовое поле, свежесрезанная лаванда саженцы морозостойкой лаванды и сухоцветы.

                У нас вы сможете как посетить лавандовое поле в составе группы, так и полностью арендовать поле на пол часа.""");

        execute(photo);
    }

    @SneakyThrows
    private void suPaySelected(String messageText, long chatId) {
        String pattern = "[0-9]-[0-9]-[0-9]{3}-[0-9]{3}-[0-9]{3}";

        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(messageText);

        if (messageText.equals(Constants.back)){
            setCurrentState(States.SU_TIME, chatId);
            suDaySelected(program.getDate().toString(), chatId);
        } else if (m.matches()){
            if (program.getProgram().equals(Constants.su_vip_program)){
                changeDays();
                BotDBConnection.POST("INSERT INTO times VALUES ('" + program.getTime() +
                        "', '" + program.getDate() + "', '" + program.getProgram() + "', 5)");
            } else {
                ResultSet resultSet = BotDBConnection.SELECT(
                        "SELECT * FROM days WHERE day='" + program.getDate() + "'"
                );
                if (!resultSet.next()) {
                    BotDBConnection.POST("INSERT INTO " +
                            "days VALUES ('" + program.getDate() + "', 0)");
                }
                resultSet = BotDBConnection.SELECT(
                        "SELECT * FROM times WHERE moment='" + program.getTime() + "' " +
                                "AND day='" + program.getDate() + "'"
                );
                if (resultSet.next()){
                    int people = resultSet.getInt("people");
                    BotDBConnection.POST(
                            "UPDATE times SET people=" + (people++)
                    );
                    if (people == 5){
                        changeDays();
                    }
                } else {
                    BotDBConnection.POST(
                            "INSERT INTO times VALUES (" +
                                    "'" + program.getTime() + "', '" + program.getDate() + "', "
                                    + "'" + program.getProgram() + "', 1)"
                    );
                }
            }
            BotDBConnection.POST(
                    "INSERT INTO visits VALUES (" +
                            getUserName(chatId) +
                            ", '" + program.getDate() + "', '" + program.getTime() + "', '" + messageText + "', '" + program.getProgram() + "')"
            );

            sendMessage(chatId, "Вы успешно записались на посещение фермы. " +
                    "Пожалуйста, сохраните квитанцию об оплате.");

            setCurrentState(States.MAIN_MENU, chatId);
            startCommandReceived(chatId);
        } else {
            sendMessage(chatId,"Пожалуйста, введите корректное значение номера квитанции.");
        }
    }

    @SneakyThrows
    private void changeDays(){
        ResultSet resultSet = BotDBConnection.SELECT(
                "SELECT busy_times FROM days WHERE day='" + program.getDate() + "'"
        );
        if (resultSet.next()){
            int busy_times = resultSet.getInt("busy_times");
            BotDBConnection.POST(
                    "UPDATE days SET busy_times=" + (busy_times+1) + " WHERE day='" + program.getDate() + "'"
            );
        } else {
            BotDBConnection.POST(
                    "INSERT INTO days VALUES (" +
                            "'" + program.getDate() + "', 1)"
            );
        }
    }

    @SneakyThrows
    private void suTimeSelected(String messageText, long chatId) {
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        DateValidatorUsingDateTimeFormatter validator = new DateValidatorUsingDateTimeFormatter(timeFormatter);
        if (messageText.equals(Constants.back)){
            setCurrentState(States.SU_DAY, chatId);
            selectSUDay(program.getProgram(), chatId);
        } else if (validator.isValid(messageText)) {
            LocalTime moment = LocalTime.parse(messageText);
            if (moment.getHour() < 5 || moment.getHour() > 22 ||
                    (moment.getMinute() != 0 && moment.getMinute() != 30)){
                sendMessage(chatId, "Пожалуйста, введите выберете дату из представленных ниже.");
            } else {
                setCurrentState(States.SU_PAY, chatId);
                program.setTime(Time.valueOf(moment));
                SendPhoto photo = new SendPhoto();
                photo.setPhoto(new InputFile(new File("images/qr.jpg")));

                int price = 300;
                if (program.getProgram().equals(Constants.su_vip_program)) price = 1500;
                String caption = "Вы выбрали запись по программе \"" + program.getProgram() +
                        "\", на " + program.getDate() + " в " + program.getTime() + ", стоимость " + price + " р. " +
                        "\n\nДля оплаты посещения переведите указанную сумму с помощью qr-кода " +
                        "или по номеру +79026682015.\n\nДля подтверждения оплаты пришлите номер квитанции" +
                        "в формате (1-2-345-678-910).";
                photo.setCaption(caption);

                ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
                keyboardMarkup.setResizeKeyboard(true);
                List<KeyboardRow> rows = new ArrayList<>();
                KeyboardRow row = new KeyboardRow();
                row.add(Constants.back); rows.add(row);
                keyboardMarkup.setKeyboard(rows); photo.setReplyMarkup(keyboardMarkup);

                photo.setChatId(chatId);
                execute(photo);
            }
        } else {
            sendMessage(chatId, "Пожалуйста, введите время в представленном формате.");
        }
    }

    @SneakyThrows
    private void suDaySelected(String messageText, long chatId) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateValidatorUsingDateTimeFormatter validator = new DateValidatorUsingDateTimeFormatter(dateFormatter);
        if (messageText.equals(Constants.back)){
            setCurrentState(States.SIGN_UP, chatId);
            signUpSelected(chatId);
        } else if (validator.isValid(messageText)){
            LocalDate date = LocalDate.parse(messageText, dateFormatter);
            if (date.isBefore(LocalDate.now().minusDays(1)) ||
                    date.getMonthValue() > 9 || date.getMonthValue() < 5){
                sendMessage(chatId, "Пожалуйста, введите дату между сегодняшним днём и 30 сентября текущего года.");
            } else {
                ResultSet next_day = BotDBConnection.SELECT("SELECT busy_times " +
                        "FROM days WHERE day='" + date + "'");
                int busy_times = 0;
                if (next_day.next()){
                    busy_times = next_day.getInt("busy_times");
                }
                if (busy_times == 32) {
                    sendMessage(chatId, "В данный день нет свободного времени для посещения.");
                } else {
                    program.setDate(Date.valueOf(date));
                    setCurrentState(States.SU_TIME, chatId);

                    SendMessage message = new SendMessage();
                    message.setChatId(chatId);
                    message.setText("Выберете время: ");

                    ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
                    keyboardMarkup.setResizeKeyboard(true);
                    List<KeyboardRow> rows = new ArrayList<>();
                    KeyboardRow row1 = new KeyboardRow();
                    row1.add(Constants.back); rows.add(row1);

                    LocalTime next_time = LocalTime.of(5,0,0,0);
                    while (next_time.isBefore(LocalTime.of(23,0))){
                        KeyboardRow row = new KeyboardRow();
                        ResultSet resultSet = BotDBConnection.SELECT(
                                "SELECT * FROM times " +
                                        "WHERE moment='" + next_time + "' AND " +
                                        "day='" + program.getDate() + "'"
                        );
                        if (program.getProgram().equals(Constants.su_vip_program)){
                            if (!resultSet.next()){
                                row.add(next_time.toString());
                            }
                        } else if (program.getProgram().equals(Constants.su_common_program)){
                            int people = 0;
                            if (resultSet.next()) {
                                people = resultSet.getInt("people");
                            }
                            if (people<5) {
                                row.add(next_time.toString());
                            }
                        }
                        next_time = next_time.plusMinutes(30);
                        rows.add(row);
                    }

                    keyboardMarkup.setKeyboard(rows);
                    message.setReplyMarkup(keyboardMarkup);
                    execute(message);
                }
            }
        } else {
            sendMessage(chatId, "Пожалуйста, введите дату в указанном формате.");
        }
    }

    private void suMenuSelected(String messageText, long chatId) {
        if (messageText.equals(Constants.back)){
            setCurrentState(States.MAIN_MENU, chatId);
            startCommandReceived(chatId);
        } else if (messageText.equals(Constants.su_common_program)){
            setCurrentState(States.SU_DAY, chatId);
            selectSUDay(Constants.su_common_program, chatId);
        } else if (messageText.equals(Constants.su_vip_program)){
            setCurrentState(States.SU_DAY, chatId);
            selectSUDay(Constants.su_vip_program, chatId);
        }
    }

    @SneakyThrows
    private void selectSUDay(String next_program, long chatId) {
        program.setProgram(next_program);
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Вы выбрали программу \"" + next_program + "\".\n" +
                "Введите день, в который бы вы хотели посетить нашу ферму (в формате 2022-06-22):");

        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setResizeKeyboard(true);
        List<KeyboardRow> rows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow(); row.add(Constants.back);
        rows.add(row); replyKeyboardMarkup.setKeyboard(rows);
        message.setReplyMarkup(replyKeyboardMarkup);

        execute(message);
    }

    @SneakyThrows
    private void trashPayingMenuSelected(String messageText, long chatId) {
        String pattern = "[0-9]-[0-9]-[0-9]{3}-[0-9]{3}-[0-9]{3}";

        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(messageText);
        if (messageText.equals(Constants.back)){
            setCurrentState(States.TRASH, chatId);
            trashSelected(chatId);
        } else if (m.matches()){
            String user_name = null;
            ResultSet resultSet = BotDBConnection.SELECT(
                    "SELECT \"UserName\" FROM users WHERE \"ChatId\"=" + chatId);
            while (resultSet.next()) user_name = resultSet.getString("UserName");

            ResultSet trash = BotDBConnection.SELECT(
                    "SELECT * FROM trash WHERE user_name='" + user_name + "'");

            float summa = 0;
            resultSet = BotDBConnection.SELECT(
                    "SELECT SUM(price*count) AS full_sum FROM trash WHERE user_name='" + user_name + "'"
            );
            while (resultSet.next()) summa = resultSet.getFloat("full_sum");

            BotDBConnection.POST("INSERT INTO orders " +
                    "VALUES ('" + user_name + "', " + summa + ", '" + messageText + "')");

            while (trash.next()){
                BotDBConnection.POST("INSERT INTO order_products " +
                        "VALUES ('" + trash.getString("product_name") + "', " +
                        "'" + messageText + "', " + trash.getString("count") + ")");
            }
            BotDBConnection.POST("DELETE FROM trash WHERE user_name='" + user_name + "'");

            sendMessage(chatId, "Ваш заказ успешно оплачен. " +
                    "Пожалуйста, сохраните квитанцию об оплате. " +
                    "В этом выпуске доставка пока не реализована, " +
                    "однако вы можете забрать свой заказ, " +
                    "когда посетите нашу ферму в следующий раз.");
            setCurrentState(States.TRASH, chatId);
            trashSelected(chatId);
        } else {
            sendMessage(chatId, "Пожалуйста, введите корректное значение номера квитанции.");
        }
    }

    @SneakyThrows
    private void trashDeletingMenuSelected(String messageText, long chatId) {
        if (messageText.equals(Constants.back)){
            setCurrentState(States.TRASH, chatId);
            trashSelected(chatId);
        } else {
            String where = " WHERE user_name=(" +
                    "SELECT \"UserName\" FROM users WHERE \"ChatId\"=" + chatId + ") AND " +
                    "product_name='" + messageText + "'";
            ResultSet resultSet = BotDBConnection.SELECT("SELECT * FROM trash " +
                    where);
            while (resultSet.next()){
                int count = resultSet.getInt("count");
                if (count>1){
                    BotDBConnection.POST("UPDATE trash SET count=" + (count-1) +
                            where);
                } else {
                    BotDBConnection.POST("DELETE FROM trash " +
                            where);
                }
            }
            sendMessage(chatId, "Удаление прошло успешно!");
            setCurrentState(States.TRASH, chatId);
            trashSelected(chatId);
        }
    }

    private void trashMenuSelected(String messageText, long chatId) {
        if (messageText.equals(Constants.back)){
            setCurrentState(States.MAIN_MENU, chatId);
            startCommandReceived(chatId);
        } else if (messageText.equals(Constants.trash_deleting)){
            setCurrentState(States.TRASH_DELETING, chatId);
            deletingProduct(chatId);
        } else if (messageText.equals(Constants.trash_paying)){
            setCurrentState(States.TRASH_PAYING, chatId);
            trashPaying(chatId);
        } else if (messageText.equals(Constants.trash_all_deleting)){
            BotDBConnection.POST("DELETE FROM trash WHERE user_name=(" +
                    "SELECT \"UserName\" FROM users WHERE \"ChatId\"=" + chatId + ")");
            sendMessage(chatId, "Удаление прошло успешно!");
            trashSelected(chatId);
        }
    }

    @SneakyThrows
    private void trashPaying(long chatId) {
        ResultSet resultSet = BotDBConnection.SELECT(
                "SELECT SUM(count * price) AS full_sum FROM trash \n" +
                        "WHERE user_name=(SELECT \"UserName\" FROM users WHERE \"ChatId\"=" + chatId + ")");
        SendPhoto photo = new SendPhoto();
        StringBuilder caption = new StringBuilder("Общая сумма вашего заказа\n\n");
        while (resultSet.next()){
            caption.append(resultSet.getString("full_sum")).append(" р.\n\n");
        }
        caption.append("Для успешной оплаты переведите нам указанную сумму по этому " +
                "qr-коду или по номеру +79026682015, и пришлите нам номер квитанции " +
                "(в формате 1-2-345-678-910).");
        photo.setPhoto(new InputFile(new File("images/qr.jpg")));
        photo.setCaption(String.valueOf(caption));

        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setResizeKeyboard(true);
        List<KeyboardRow> rows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow(); row.add(Constants.back);
        rows.add(row); replyKeyboardMarkup.setKeyboard(rows);
        photo.setReplyMarkup(replyKeyboardMarkup);
        photo.setChatId(chatId);

        try {
            execute(photo);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @SneakyThrows
    private void deletingProduct(long chatId) {
        ResultSet resultSet = BotDBConnection.SELECT("SELECT * FROM trash WHERE user_name=" +
                "(SELECT \"UserName\" FROM users WHERE \"ChatId\"=" + chatId + ")");
        SendMessage message = new SendMessage();
        message.setText("Выберете товар, который хотите удалить: ");
        message.setChatId(chatId);
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        List<KeyboardRow> rows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();

        while (resultSet.next()){
            row.add(resultSet.getString("product_name"));
        }

        KeyboardRow row2 = new KeyboardRow();
        row2.add(Constants.back);
        rows.add(row); rows.add(row2);

        keyboardMarkup.setKeyboard(rows);
        message.setReplyMarkup(keyboardMarkup);

        execute(message);
    }

    @SneakyThrows
    private void mapMenuSelected(String messageText, long chatId) {
        if (messageText.equals(Constants.back)){
            setCurrentState(States.MAIN_MENU, chatId);
            startCommandReceived(chatId);
        } else {
            sendMessage(chatId, "За более подробной информацией обращайтесь по телефону +79026682015.");
        }
    }

    @SneakyThrows
    private void lkMenuSelected(String messageText, long chatId) {
        if (messageText.equals(Constants.back)){
            setCurrentState(States.MAIN_MENU, chatId);
            startCommandReceived(chatId);
        } else if (messageText.equals(Constants.my_orders)){
            ResultSet resultSet = BotDBConnection.SELECT(
                    "SELECT * FROM orders WHERE user_name=" + getUserName(chatId)
            );
            StringBuilder text = new StringBuilder("На данный момент вы совершили такие заказы: \n\n");
            int i = 1;
            while (resultSet.next()){
                text.append(i++).append(". Номер: ").append(resultSet.getString("number"))
                        .append("\n").append("Сумма заказа: ")
                        .append(resultSet.getFloat("summa"))
                        .append("\n\n");
            }
            sendMessage(chatId, text.toString());
        } else if (messageText.equals(Constants.my_signs)){
            ResultSet resultSet = BotDBConnection.SELECT(
                    "SELECT * FROM visits WHERE user_name=" + getUserName(chatId) +
                            " ORDER BY day, moment"
            );
            StringBuilder text = new StringBuilder("На данный момент вы записаны на следующие посещения: \n\n");
            int i = 1;
            while (resultSet.next()){
                text.append(i++).append(". Номер: ").append(resultSet.getString("number"))
                        .append("\n").append("Программа: ")
                        .append(resultSet.getString("program"))
                        .append("\n").append("Дата: ")
                        .append(resultSet.getDate("day"))
                        .append("\n").append("Время: ")
                        .append(resultSet.getTime("moment"))
                        .append("\n\n");
            }
            sendMessage(chatId, text.toString());
        }
    }

    private String getUserName(long chatId){
        return "(SELECT \"UserName\" FROM users WHERE \"ChatId\"=" + chatId + ")";
    }

    @SneakyThrows
    private void catalogMenuSelected(String messageText, long chatId, String userName) {
        if (messageText.equals(Constants.back)){
            setCurrentState(States.MAIN_MENU, chatId);
            startCommandReceived(chatId);
        } else {
            ResultSet resultSet = BotDBConnection.SELECT("SELECT * FROM products WHERE \"Name\"='" + messageText + "'");
            if (resultSet.next()){
                float price = resultSet.getFloat("price");
                String name = resultSet.getString("Name");

                ResultSet checking = BotDBConnection.SELECT("SELECT * FROM trash WHERE user_name='"
                        + userName +"' AND product_name='" + name + "'");

                boolean flag = true;
                while (checking.next()) {
                    int count = checking.getInt("count");
                    BotDBConnection.POST("UPDATE trash SET count=" + (count+1) + " WHERE user_name='"
                            + userName +"' AND product_name='" + name + "'");
                    flag = false;
                }
                if (flag) {
                    BotDBConnection.POST("INSERT INTO trash VALUES ('" + userName
                            + "', '" + name + "', " + price + ", " + 1 + ")");
                }

                sendMessage(chatId,"Товар \"" + name + "\" успешно добавлен в корзину.");
            } else {
                sendMessage(chatId, "Пожалуйста, выберете товар из корзины или вернитесь в главное меню.");
            }
        }
    }

    private void mainMenuSelected(String messageText, long chatId) {
        if (messageText.equals(Constants.menu_map)){
            setCurrentState(States.MAP, chatId);
            showMap(chatId);
        } else if (messageText.equals(Constants.menu_catalog)){
            setCurrentState(States.CATALOG, chatId);
            catalogSelected(chatId);
        } else if (messageText.equals(Constants.menu_sign_up)) {
            setCurrentState(States.SIGN_UP, chatId);
            signUpSelected(chatId);
        } else if (messageText.equals(Constants.menu_trash)) {
            setCurrentState(States.TRASH, chatId);
            trashSelected(chatId);
        } else if (messageText.equals(Constants.menu_lk)){
            setCurrentState(States.LK, chatId);
            lkSelected(chatId);
        }
    }

    @SneakyThrows
    private void signUpSelected(long chatId) {
        SendPhoto message = new SendPhoto();
        message.setPhoto(new InputFile(new File("images/girl.jpg")));

        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setResizeKeyboard(true);
        List<KeyboardRow> rows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add(Constants.su_common_program);
        row.add(Constants.su_vip_program);
        rows.add(row);
        KeyboardRow row1 = new KeyboardRow();
        row1.add(Constants.back);
        rows.add(row1);
        replyKeyboardMarkup.setKeyboard(rows);
        message.setReplyMarkup(replyKeyboardMarkup);

        message.setCaption("Выберете программу посещения:");
        message.setChatId(chatId);
        execute(message);
    }

    @SneakyThrows
    private void trashSelected(long chatId) {
        ResultSet resultSet = BotDBConnection.SELECT(
                "SELECT * FROM trash WHERE user_name=(SELECT \"UserName\" FROM users WHERE \"ChatId\"=" + chatId + ")" );
        SendMessage message = new SendMessage();
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        List<KeyboardRow> rows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add(Constants.trash_deleting); row.add(Constants.trash_all_deleting); row.add(Constants.trash_paying);
        KeyboardRow row1 = new KeyboardRow();
        row1.add(Constants.back);
        rows.add(row); rows.add(row1); keyboardMarkup.setKeyboard(rows);
        message.setReplyMarkup(keyboardMarkup);
        message.setChatId(chatId);

        StringBuilder text = new StringBuilder("""
                В Вашей корзине на данный момент находятся следующие товары:
                                
                """);
        while (resultSet.next()){
            text.append(resultSet.getString("product_name")).append(" ")
                    .append(resultSet.getFloat("price")).append(" - р. ")
                    .append(resultSet.getInt("count")).append(" шт.\n");
        }
        message.setText(text.toString());

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @SneakyThrows
    private void showMap(long chatId) {
        SendPhoto photo = new SendPhoto();
        photo.setChatId(chatId);
        photo.setCaption("""
                Наш адрес: Нижегородская область, Дальнеконстантиновский муниципальный округ.

                Ориентир - деревня Зубаниха дальнеконстантиновского район Нижегородской области. Для тех кого интересует возможность добраться на общественном транспорте - автобусы 224, 224а . Отправление с автовокзала Щербинки.
                Просите остановить у лавандового поля- это между деревнями Зубаниха и Кужутки.""");
        photo.setPhoto(new InputFile(new File("images/map.jpg")));

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        List<KeyboardRow> rows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add(Constants.back);
        rows.add(row);
        keyboardMarkup.setKeyboard(rows);
        photo.setReplyMarkup(keyboardMarkup);

        execute(photo);
    }

    @SneakyThrows
    private void lkSelected(long chatId) {
        SendMessage message = new SendMessage();
        StringBuilder text = new StringBuilder("""
                Добро пожаловать в Ваш личный кабинет. Здесь Вы можете просматривать ваши контактные данные, а также смотреть списки ваших заказов и записей на посещение.

                """);

        ResultSet resultSet = BotDBConnection.SELECT("Select * FROM users WHERE \"ChatId\"=" + chatId);
        while (resultSet.next()){
            text.append("Имя пользователя: ").append(resultSet.getString("UserName")).append("\nПолное имя: ").append(resultSet.getString("FullName")).append("\nНомер телефона: ").append(resultSet.getString("PhoneNumber")).append("\nАдрес доставки: ").append(resultSet.getString("Address"));
        }

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        List<KeyboardRow> rows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add(Constants.my_orders);
        row.add(Constants.my_signs);
        rows.add(row);

        KeyboardRow row2 = new KeyboardRow();
        row2.add(Constants.back);
        rows.add(row2);

        keyboardMarkup.setKeyboard(rows);

        message.setReplyMarkup(keyboardMarkup);
        message.setText(text.toString());
        message.setChatId(chatId);
        execute(message);
    }

    @SneakyThrows
    private void catalogSelected(long chatId){
        SendMessage message = new SendMessage();

        SendMediaGroup group = new SendMediaGroup();
        List<InputMedia> list = new ArrayList<>();

        ResultSet resultSet = BotDBConnection.SELECT("SELECT * FROM products");
        StringBuilder text = new StringBuilder("На данный момент в нашем каталоге представлены следующие товары. Выберете, что бы вы хотели добавить в корзину:\n\n");

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();

        int i = 1;
        while (resultSet.next()){
            SendPhoto photo = new SendPhoto();
            photo.setChatId(chatId);

            String name = resultSet.getString("Name");
            row.add(name);
            photo.setPhoto(new InputFile(new File("images/" + resultSet.getString("Photo"))));
            execute(photo);

            text.append(i++).append(". ").append(name).append("\n")
                    .append(resultSet.getString("Description"))
                    .append("\n").append("Стоимость: ")
                    .append(resultSet.getString("Price")).append(" рублей\n\n");
        }
        keyboardRows.add(row);
        keyboardMarkup.setKeyboard(keyboardRows);
        KeyboardRow row2 = new KeyboardRow();
        row2.add(Constants.back);
        keyboardRows.add(row2);
        keyboardMarkup.setKeyboard(keyboardRows);

        group.setMedias(list);
        group.setChatId(chatId);

        message.setChatId(chatId);
        message.setText(text.toString());
        message.setReplyMarkup(keyboardMarkup);

        execute(message);
    }

    private static void setCurrentState(String currentState, long chatId){
        BotDBConnection.POST("UPDATE users SET currentstate='" + currentState + "' WHERE \"ChatId\"="+chatId);
    }

    private void startCommandReceived(long chatId){
        SendPhoto message = new SendPhoto();
        message.setPhoto(new InputFile(new File("images/about.jpg")));
        message.setChatId(chatId);

        String answer = "Добро пожаловать в главное меню нашего телеграмм-бота, что вы ищите на этот раз?";
        message.setCaption(answer);
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(Constants.menu_map);
        row1.add(Constants.menu_catalog);
        row1.add(Constants.menu_sign_up);

        KeyboardRow row2 = new KeyboardRow();
        row2.add(Constants.menu_trash);
        row2.add(Constants.menu_lk);

        keyboardRows.add(row1); keyboardRows.add(row2);
        keyboardMarkup.setKeyboard(keyboardRows);
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
        log.info("Replied to user");
    }

    private void sendMessage(long chatId, String textToSend){
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(textToSend);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }
}
