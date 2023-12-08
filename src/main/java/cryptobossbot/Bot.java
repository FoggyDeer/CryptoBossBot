package cryptobossbot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.*;
import java.io.File;
import java.util.*;

import static cryptobossbot.Locale.*;
import static cryptobossbot.Menu.MenuType.*;

public class Bot extends TelegramLongPollingBot {
    private static final String PASSWORD = "CryptoBossInsurance";
    private Map<Long, CryptoBossUser> users = new HashMap<>();
    Bot(String api_key, BotOnCreate botOnCreate) throws IOException {
        super(api_key);
        this.users = readUsersTo(users);

        boolean registered = false;
        while (!registered){
            try {
                TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
                botsApi.registerBot(this);
                registered = true;
            } catch (TelegramApiException ignored){}
        }
        botOnCreate.run(this);
    }

    @Override
    public String getBotUsername() {
        return "CryptoBossBot";
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            Message msg = update.getMessage();
            CallbackQuery callbackQuery= update.getCallbackQuery();
            User user;
            if(msg != null) user = msg.getFrom();
            else user = callbackQuery.getFrom();

            if(user != null){
                CryptoBossUser cb_user = users.get(user.getId());


                // Check if user is banned
                if(cb_user != null && cb_user.isBanned()){
                    System.out.println("\u001B[31mBanned (ID: "+cb_user.getId()+") \u001B[34m"+cb_user.getFirstName()+": \u001B[33m" + (msg != null ? msg.getText()+"\u001B[0m" : ""));
                    sendMessage(user.getId(), CryptoBossLocales.getText(cb_user.getLocale(),"__@Msg_0100__"));
                    return;
                }

                // Execute menu action
                if(callbackQuery != null && cb_user != null && callbackQuery.getMessage().getMessageId().equals(cb_user.getLastMenuId())){
                    Menu.menuCommand(callbackQuery.getData(), cb_user, this, cb_user.isAuthorized(), callbackQuery.getMessage().getMessageId());
                }


                if(cb_user == null || !cb_user.isAuthorized()){
                    // Login verification
                    checkLoginStatus(user, msg, callbackQuery);
                } else if(msg != null && msg.isCommand()){
                    switch (msg.getText()){
                        case "/menu" -> {
                            Menu.sendMenu(_MENU, cb_user, this);
                        }
                        case "/reset" -> {
                            resetUser(String.valueOf(user.getId()));
                        }
                    }
                }
            }
        } catch (IOException ignored) {}
    }

    /**
     *  Welcome messages:
     *  <p>
     *  from <b>__@Msg_0000__</b> to <b>__@Msg_00ff__</b>
     *  </p>
     *  <p>
     *  System messages:
     *  </p>
     *  <p>
     *  from <b>__@Msg_0100__</b> to <b>__@Msg_01ff__</b>
     *  </p>
     *  <p>
     *  Content messages:
     *  </p>
     *  <p>
     *  from <b>__@Msg_0200__</b> to <b>__@Msg_02ff__</b>
     *  </p>
     *  <p>
     *  Menu buttons:
     *  </p>
     *  <p>
     *  from <b>__@Msg_0300__</b> to <b>__@Msg_03ff__</b>
     *  </p>
     * **/

    public void checkLoginStatus(User user, Message msg, CallbackQuery callbackQuery){
        try {
            CryptoBossUser cb_user = users.get(user.getId());

            if (cb_user == null) {
                cb_user = new CryptoBossUser(user);     // Creating CryptoBossUser instance from telegram User
                users.put(user.getId(), cb_user);       // Adding user to users list

                Menu.sendMenu(_LOCALE, cb_user, this, false);

            } else if (callbackQuery != null && Menu.getMenuType(callbackQuery) == _LOCALE) {
                // Deleting "selecting locale menu" message
                deleteMessage(callbackQuery.getMessage().getChatId(), callbackQuery.getMessage().getMessageId());

                // Sending greeting message and question about username
                sendMessage(cb_user.getId(), CryptoBossLocales.getText(cb_user.getLocale(), "__@Msg_0002__"));
                sendMessage(cb_user.getId(), CryptoBossLocales.getText(cb_user.getLocale(), "__@Msg_0003__"));
            } else if (callbackQuery == null && !cb_user.isLoggedIn() && cb_user.getLocale() != null) {
                if (cb_user.getCBUserName() == null && msg.getText() != null) {
                    // Setting username
                    cb_user.setUsername(msg.getText());

                    // Asking for a password
                    sendMessage(cb_user.getId(), CryptoBossLocales.getText(cb_user.getLocale(), "__@Msg_0004__"));
                } else if (!cb_user.isLoggedIn()) {
                    if (msg.getText().trim().equals(PASSWORD)) {
                        // Deleting entering password messages
                        deleteMessagesTo(msg.getChatId(), msg.getMessageId(), msg.getMessageId());

                        // Resting tries to log in to 3 and set logged in
                        cb_user.resetTries();
                        cb_user.setLoggedIn();

                        sendMessage(cb_user.getId(), CryptoBossLocales.getText(cb_user.getLocale(), "__@Msg_0007__"));
                        Menu.sendMenu(_DATA_SOURCE, cb_user, this, false);
                    } else {
                        // Decrease log in tries by 1
                        cb_user.decreaseTries();

                        // Checking log in tries count
                        if (cb_user.getTriesToLogin() > 0) {
                            sendMessage(cb_user.getId(), CryptoBossLocales.getText(cb_user.getLocale(), "__@Msg_0005__") +
                                    '\n' +
                                    CryptoBossLocales.getText(cb_user.getLocale(), "__@Msg_0006__") +
                                    " - " +
                                    cb_user.getTriesToLogin() + ':');
                        } else {
                            // Ban user
                            cb_user.ban();

                            // Sending message about banned status
                            sendMessage(cb_user.getId(), CryptoBossLocales.getText(cb_user.getLocale(), "__@Msg_0005__") +
                                    '\n' +
                                    CryptoBossLocales.getText(cb_user.getLocale(), "__@Msg_0006__") +
                                    " - " +
                                    cb_user.getTriesToLogin());

                            sendMessage(cb_user.getId(), CryptoBossLocales.getText(cb_user.getLocale(), "__@Msg_0100__"));
                        }
                    }
                }
            } else if (callbackQuery != null) {
                if (Menu.getMenuType(callbackQuery) == _DATA_SOURCE && cb_user.getDataSource() != null) {
                    Menu.sendMenu(_PLATFORM, cb_user, this, false, cb_user.getLastMenuId(), null);
                } else if (Menu.getMenuType(callbackQuery) == _PLATFORM && cb_user.getPlatform() != null) {
                    Menu.sendMenu(_ORDERS_TYPE, cb_user, this, false, cb_user.getLastMenuId(), null);
                } else if (Menu.getMenuType(callbackQuery) == _ORDERS_TYPE && cb_user.getOrdersType() != null) {
                    Menu.sendMenu(_FIAT_CURRENCY, cb_user, this, false, cb_user.getLastMenuId(), 0);
                } else if(Menu.getMenuType(callbackQuery) == _CRYPTOCURRENCY && !Menu.isNavigationAction(callbackQuery) && cb_user.getCryptoCurrency() != null){
                    Menu.sendMenu(_MENU, cb_user, this, false, callbackQuery.getMessage().getMessageId(), 0);
                    sendMessage(cb_user.getId(), CryptoBossLocales.getText(cb_user.getLocale(), "__@Msg_000e__").replace("@Platform", String.valueOf(cb_user.getPlatform())));
                    cb_user.authorize();
                    saveUser(cb_user);
                }
            }
        }catch (IOException ignored){}
        saveUser(users.get(user.getId()));
    }

    public void sendMessage(Long userId, String text) {
        sendMessage(userId, text, false);
    }

    public void sendMessage(Long userId, String text, boolean force) {
        CryptoBossUser cb_user = users.get(userId);

        if(cb_user == null && !force){
            System.out.println("\u001B[31mCan't send message. No users was founded with provided ID.\u001B[0m");
        } else {
            SendMessage sm = SendMessage.builder()
                    .chatId(userId.toString()).text(text).parseMode("HTML")
                    .build();
            try {
                executeMethod(sm);
                System.out.println("\u001B[35mText was sent to:\u001B[0m   " + (force ? userId : cb_user));
            }catch (MethodNotExecutedException ignored){}
        }
    }

    public void deleteMessage(Long chatId, int messageId){
        DeleteMessage dm = new DeleteMessage();
        dm.setChatId(chatId);
        dm.setMessageId(messageId);
        try {
            executeMethod(dm);
        } catch (MethodNotExecutedException ignored){}
    }

    public void deleteMessage(Long chatId, int messageId, DeleteMessage dm) throws MethodNotExecutedException{
        dm.setChatId(chatId);
        dm.setMessageId(messageId);
        executeMethod(dm);
    }

    public void deleteMessagesTo(Long chatId, int messageIdFrom, int count){
        DeleteMessage dm = new DeleteMessage();
        try {
            for (int i = count; i > 0; i--, messageIdFrom--) {
                deleteMessage(chatId, messageIdFrom, dm);
            }
        }
        catch (MethodNotExecutedException ignored){}
    }

    public <T extends Serializable> T executeMethod(BotApiMethod<T> method) throws MethodNotExecutedException{
        boolean executed = false;
        T result = null;
        int tries = 0;
        try {
            while (!executed) {
                tries++;
                try {
                    result = execute(method);
                    executed = true;
                } catch (TelegramApiException error) {
                    if(tries > 3)
                        throw new MethodNotExecutedException(0);

                    if(error instanceof TelegramApiRequestException && ((TelegramApiRequestException)error).getErrorCode() == 400) {
                        throw new MethodNotExecutedException(((TelegramApiRequestException) error).getErrorCode());
                    }

                    if(method instanceof DeleteMessage){
                        System.out.println("Message not deleted");
                    } else if(method instanceof SendMessage){
                        System.out.println("Message not sent");
                    }else if(method instanceof EditMessageText){
                        System.out.println("Message not edited");
                    }else if(method instanceof EditMessageReplyMarkup){
                        System.out.println("Markup not edited");
                    }

                    Thread.sleep(2000);
                }
            }
        } catch (InterruptedException ignored){}
        return result;
    }

    static class MethodNotExecutedException extends RuntimeException{
        private final int errorCode;
        MethodNotExecutedException(int errorCode){
            super("Method not executed");
            this.errorCode = errorCode;
        }

        public int getErrorCode(){
            return errorCode;
        }
    }

    public void notifyAll(String text) {
        if(text == null){
            Console.error("No text provided.");
        }
        else if (users.size() > 0) {
            for (CryptoBossUser cb_user : users.values()) {
                if(cb_user.isAuthorized() && !cb_user.isBanned())
                    sendMessage(cb_user.getId(), text
                            .replace("__@Msg_0200__", CryptoBossLocales.getText(cb_user.getLocale(), "__@Msg_0200__"))
                            .replace("__@Msg_0201__", CryptoBossLocales.getText(cb_user.getLocale(), "__@Msg_0201__"))
                            .replace("__@Msg_0202__", CryptoBossLocales.getText(cb_user.getLocale(), "__@Msg_0202__"))
                            .replace("__@Msg_0203__", CryptoBossLocales.getText(cb_user.getLocale(), "__@Msg_0203__"))
                            .replace("__@Msg_0204__", CryptoBossLocales.getText(cb_user.getLocale(), "__@Msg_0204__")));
            }
        } else {
            Console.error("No users!");
        }
    }

    public void sendMessageToUsers(String text, Long... id){
        for(Long n : id){
            sendMessage(n, text);
        }
    }

    public void saveUser(CryptoBossUser cb_user){
        try {
            FileOutputStream fos = new FileOutputStream("Users\\"+cb_user.getFirstName() + '_' + cb_user.getId() + ".bin");
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(cb_user);
            oos.close();
            fos.close();
        }catch (IOException ignored){}
    }

    private Map<Long, CryptoBossUser> readUsersTo(Map<Long, CryptoBossUser> arg) throws IOException {
        Map<Long, CryptoBossUser> map = new HashMap<>();
        ArrayList<String> filenames = new ArrayList<>();

        File folder = new File("./Users");
        if (!folder.exists()){
            folder.mkdirs();
            return map;
        }

        File[] listOfFiles = folder.listFiles();

        if(listOfFiles != null)
            for (File file : listOfFiles) {
                if (file.isFile()) {
                    filenames.add(file.getName());
                    System.out.println(file.getName());
                }
            }


        for(String filename : filenames){
            try{
                FileInputStream fis = new FileInputStream(folder.getName()+'/'+filename);
                ObjectInputStream ois = new ObjectInputStream(fis);
                CryptoBossUser cb_user = (CryptoBossUser) ois.readObject();
                map.put(cb_user.getId(), cb_user);
                ois.close();
                fis.close();
            } catch (ClassNotFoundException | ClassCastException e){
                FileOutputStream fos = new FileOutputStream(filename);
                ObjectOutputStream oos = new ObjectOutputStream(fos);
                oos.writeObject(arg);
                oos.close();
                fos.close();
            }
        }
        return map;

    }

    public void deleteUser(Long userId){
        File file = new File("Users\\"+users.get(userId).getFirstName()+"_"+userId+".bin");
        file.delete();
    }

    public void banUser(String id){
        CryptoBossUser cb_user = users.get(Long.parseLong(id));

        if(cb_user != null){
            if(cb_user.isBanned()){
                Console.error("User is already banned.");
                return;
            }

            cb_user.ban();
            users.put(cb_user.getId(), cb_user);
            saveUser(cb_user);
            sendMessage(cb_user.getId(), CryptoBossLocales.getText(cb_user.getLocale(),"__@Msg_0006__"));
            Console.success(cb_user + " was banned successfully.");
        } else {
            Console.error("No such user was found.");
        }
    }

    public void unbanUser(String id){
        CryptoBossUser cb_user = users.get(Long.parseLong(id));

        if(cb_user != null){
            if(!cb_user.isBanned()){
                Console.error("User is not banned.");
                return;
            }

            cb_user.unban();
            users.put(cb_user.getId(), cb_user);
            saveUser(cb_user);
            sendMessage(cb_user.getId(), CryptoBossLocales.getText(cb_user.getLocale(),"__@Msg_000e__"));
            Console.success(cb_user + " was banned successfully.");
        } else {
            Console.error("No such user was found.");
        }
    }

    public void resetUser(String id) throws IOException {
        CryptoBossUser cb_user = users.get(Long.parseLong(id));

        if(cb_user != null){
            sendMessage(cb_user.getId(), CryptoBossLocales.getText((cb_user.getLocale() != null ? cb_user.getLocale() : EN), "__@Msg_0102__"), true);
            deleteUser(cb_user.getId());
            users.remove(cb_user.getId());
            Console.success(cb_user + " data was reset successfully.");
        }
    }

    public Map<Long, CryptoBossUser> getUsers() {
        return users;
    }

    public void printAllUsers(){
        System.out.println("-----------------------------------------------------------------------------------");
        System.out.println("\u001B[36mUsers list:\u001B[0m");
        for(CryptoBossUser cb_user : users.values()){
            System.out.println(cb_user);
        }
        System.out.println("-----------------------------------------------------------------------------------");
    }

    public void printAllAuthorizedUsers(){
        System.out.println("-----------------------------------------------------------------------------------");
        System.out.println("\u001B[36mUsers list:\u001B[0m");
        for(CryptoBossUser cb_user : users.values()){
            if(cb_user.isAuthorized())
                System.out.println(cb_user);
        }
        System.out.println("-----------------------------------------------------------------------------------");
    }

    public void printAllLoggingUsers(){
        System.out.println("-----------------------------------------------------------------------------------");
        System.out.println("\u001B[36mUsers list:\u001B[0m");
        for(CryptoBossUser cb_user : users.values()){
            if(!cb_user.isLoggedIn())
                System.out.println(cb_user);
        }
        System.out.println("-----------------------------------------------------------------------------------");
    }

    public void printAllBannedUsers(){
        System.out.println("-----------------------------------------------------------------------------------");
        System.out.println("\u001B[36mUsers list:\u001B[0m");
        for(CryptoBossUser cb_user : users.values()){
            if(cb_user.isBanned())
                System.out.println(cb_user);
        }
        System.out.println("-----------------------------------------------------------------------------------");
    }
}