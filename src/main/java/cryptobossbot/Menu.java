package cryptobossbot;

import com.vdurmont.emoji.Emoji;
import com.vdurmont.emoji.EmojiManager;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.io.IOException;
import java.util.*;

import static cryptobossbot.ExchangeDataMonitor.DataSource.*;
import static cryptobossbot.ExchangeDataMonitor.OrderType.*;
import static cryptobossbot.ExchangeDataMonitor.Platform.*;
import static cryptobossbot.Locale.EN;
import static cryptobossbot.Locale.RU;
import static cryptobossbot.Menu.MenuType.*;

class MenuMarkup {
    private final String text;
    private final InlineKeyboardMarkup inlineKeyboardMarkup;

    public MenuMarkup(String text, InlineKeyboardMarkup inlineKeyboardMarkup) {
        this.text = text;
        this.inlineKeyboardMarkup = inlineKeyboardMarkup;
    }

    public String getText() {
        return text;
    }

    public InlineKeyboardMarkup getInlineKeyboardMarkup() {
        return inlineKeyboardMarkup;
    }
}

public class Menu {
    private static final Map<String, String> flags = new HashMap<>();
    static {
        List<String> list;
        for(Emoji emoji : EmojiManager.getForTag("flag")){
            flags.put(emoji.getAliases().get(0).replace("_flag", ""), emoji.getUnicode());
        }
    }
    public enum MenuType {
        _MENU("_MENU"),
        _LOCALE("_LOCALE"),
        _DATA_SOURCE("_DATA_SOURCE"),
        _PLATFORM("_PLATFORM"),
        _ORDERS_TYPE("_ORDERS_TYPE"),
        _FIAT_CURRENCY("_FIAT_CURRENCY"),
        _CRYPTOCURRENCY("_CRYPTOCURRENCY");
        private final String value;
        MenuType(String value) {
            this.value = value;
        }
        @Override
        public String toString() {
            return value;
        }
    }

    public static void sendMenu(MenuType menuType, CryptoBossUser cb_user, Bot bot) throws IOException {
        sendMenu(menuType, cb_user, bot, false);
    }

    public static void sendMenu(MenuType menuType, CryptoBossUser cb_user, Bot bot, boolean fullMenu) throws IOException {
        sendMenu(menuType, cb_user, bot, fullMenu, null, null);
    }

    public static void sendMenu(MenuType menuType, CryptoBossUser cb_user, Bot bot, boolean fullMenu, Integer menuId, Integer page) throws IOException {
        MenuMarkup markup = switch (menuType) {
            case _MENU -> getMainMenu(cb_user);
            case _LOCALE -> getLanguageMenu(fullMenu, cb_user);
            case _DATA_SOURCE -> getDataSourceMenu(fullMenu, cb_user);
            case _PLATFORM -> getPlatformMenu(fullMenu, cb_user);
            case _ORDERS_TYPE -> getOrderTypeMenu(fullMenu, cb_user);
            case _FIAT_CURRENCY -> getFiatCurrencyMenu(page, fullMenu, cb_user);
            case _CRYPTOCURRENCY -> getCryptocurrencyMenu(page, fullMenu, cb_user);
        };
        if(markup != null) {
            if (menuId == null) {
                try {
                    Message message = sendMenu(cb_user.getId(), markup.getText(), markup.getInlineKeyboardMarkup(), bot);
                    cb_user.setLastMenuId(message.getMessageId());
                    bot.saveUser(cb_user);
                } catch (Bot.MethodNotExecutedException ignored) {
                }
            } else
                editMenu(cb_user.getId(), menuId, markup.getText(), markup.getInlineKeyboardMarkup(), bot);
        }
    }

    public static MenuMarkup getMainMenu(CryptoBossUser cb_user){
        InlineKeyboardButton locale = InlineKeyboardButton.builder()
                .text(CryptoBossLocales.getText(cb_user.getLocale(), "__@Msg_0301__"))
                .callbackData("_MENU " + _LOCALE)
                .build();

        InlineKeyboardButton dataSource = InlineKeyboardButton.builder()
                .text(CryptoBossLocales.getText(cb_user.getLocale(), "__@Msg_0302__"))
                .callbackData("_MENU " + _DATA_SOURCE)
                .build();

        InlineKeyboardButton platform = InlineKeyboardButton.builder()
                .text(CryptoBossLocales.getText(cb_user.getLocale(), "__@Msg_0303__"))
                .callbackData("_MENU " + _PLATFORM)
                .build();

        InlineKeyboardButton orderType = InlineKeyboardButton.builder()
                .text(CryptoBossLocales.getText(cb_user.getLocale(), "__@Msg_0304__"))
                .callbackData("_MENU " + _ORDERS_TYPE)
                .build();

        InlineKeyboardButton fiat = InlineKeyboardButton.builder()
                .text(CryptoBossLocales.getText(cb_user.getLocale(), "__@Msg_0305__"))
                .callbackData("_MENU " + _FIAT_CURRENCY + " 0")
                .build();

        InlineKeyboardButton crypto = InlineKeyboardButton.builder()
                .text(CryptoBossLocales.getText(cb_user.getLocale(), "__@Msg_0306__"))
                .callbackData("_MENU " + _CRYPTOCURRENCY + " 0")
                .build();


        InlineKeyboardMarkup.InlineKeyboardMarkupBuilder keyboard = InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(locale, dataSource))
                .keyboardRow(List.of(platform, orderType))
                .keyboardRow(List.of(fiat, crypto));

        return new MenuMarkup(CryptoBossLocales.getText(cb_user.getLocale(), "__@Msg_000f__"), keyboard.build());
    }

    public static MenuMarkup getLanguageMenu(boolean fullMenu, CryptoBossUser cb_user) {
        // Creating Menu Buttons (EN, RU)
        InlineKeyboardButton en = InlineKeyboardButton.builder()
                .text(isSelectedButton(cb_user.getLocale(), EN) +
                        CryptoBossLocales.getText(EN, "__@Msg_0001__"))
                .callbackData("_LOCALE " + EN)
                .build();

        InlineKeyboardButton ru = InlineKeyboardButton.builder()
                .text(isSelectedButton(cb_user.getLocale(), RU) +
                        CryptoBossLocales.getText(RU, "__@Msg_0001__"))
                .callbackData("_LOCALE " + RU)
                .build();

        // Creating menu keyboard
        InlineKeyboardMarkup.InlineKeyboardMarkupBuilder keyboard = InlineKeyboardMarkup.builder().keyboardRow(List.of(en, ru));

        if (fullMenu) {
            keyboard.keyboardRow(List.of(backButton(cb_user.getLocale())));
        }

        return new MenuMarkup(CryptoBossLocales.getText(EN, "__@Msg_0000__")
                + '\n'
                + CryptoBossLocales.getText(RU, "__@Msg_0000__"), keyboard.build());
    }

    public static MenuMarkup getDataSourceMenu(boolean fullMenu, CryptoBossUser cb_user) {
        // Creating Menu Buttons (Binance, ByBit)
        InlineKeyboardButton binanceBtn = InlineKeyboardButton.builder()
                .text(isSelectedButton(cb_user.getDataSource(), Binance) + "Binance").callbackData("_DATA_SOURCE " + Binance)
                .build();

        InlineKeyboardButton bybitBtn = InlineKeyboardButton.builder()
                .text(isSelectedButton(cb_user.getDataSource(), ByBit) + "-ByBit-")
                .callbackData("_DATA_SOURCE " + ByBit)
                .build();

        // Creating menu keyboard
        InlineKeyboardMarkup.InlineKeyboardMarkupBuilder keyboard = InlineKeyboardMarkup.builder().keyboardRow(List.of(binanceBtn, bybitBtn));

        if (fullMenu) {
            keyboard.keyboardRow(List.of(backButton(cb_user.getLocale())));
        }

        return new MenuMarkup(CryptoBossLocales.getText(cb_user.getLocale(), "__@Msg_0008__"), keyboard.build());
    }

    public static MenuMarkup getPlatformMenu(boolean fullMenu, CryptoBossUser cb_user) {
        // Creating Menu Buttons (Spot, P2P)
        InlineKeyboardButton spot = InlineKeyboardButton.builder()
                .text(isSelectedButton(cb_user.getPlatform(), Spot) + "-Spot-").callbackData("_PLATFORM " + Spot)
                .build();

        InlineKeyboardButton p2p = InlineKeyboardButton.builder()
                .text(isSelectedButton(cb_user.getPlatform(), P2P) + "P2P").callbackData("_PLATFORM " + P2P)
                .build();

        // Creating menu keyboard
        InlineKeyboardMarkup.InlineKeyboardMarkupBuilder keyboard = InlineKeyboardMarkup.builder().keyboardRow(List.of(spot, p2p));

        if (fullMenu) {
            keyboard.keyboardRow(List.of(backButton(cb_user.getLocale())));
        }

        return new MenuMarkup(CryptoBossLocales.getText(cb_user.getLocale(), "__@Msg_0009__"), keyboard.build());
    }

    public static MenuMarkup getOrderTypeMenu(boolean fullMenu, CryptoBossUser cb_user) {
        // Creating Menu Buttons (Buy, Sell, Both)
        InlineKeyboardButton buy = InlineKeyboardButton.builder()
                .text(isSelectedButton(cb_user.getOrdersType(), BUY) + CryptoBossLocales.getText(cb_user.getLocale(), "__@Msg_0200__")).callbackData("_ORDERS_TYPE " + BUY)
                .build();

        InlineKeyboardButton sell = InlineKeyboardButton.builder()
                .text(isSelectedButton(cb_user.getOrdersType(), SELL) + CryptoBossLocales.getText(cb_user.getLocale(), "__@Msg_0201__")).callbackData("_ORDERS_TYPE " + SELL)
                .build();

        InlineKeyboardButton both = InlineKeyboardButton.builder()
                .text(isSelectedButton(cb_user.getOrdersType(), BOTH) + CryptoBossLocales.getText(cb_user.getLocale(), "__@Msg_000b__")).callbackData("_ORDERS_TYPE " + BOTH)
                .build();


        // Creating menu keyboard
        InlineKeyboardMarkup.InlineKeyboardMarkupBuilder keyboard = InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(buy, sell))
                .keyboardRow(List.of(both));

        if (fullMenu) {
            keyboard.keyboardRow(List.of(backButton(cb_user.getLocale())));
        }

        return new MenuMarkup(CryptoBossLocales.getText(cb_user.getLocale(), "__@Msg_000a__"), keyboard.build());
    }

    public static MenuMarkup getFiatCurrencyMenu(Integer page, boolean fullMenu, CryptoBossUser cb_user) throws IOException {

        ArrayList<AbstractMap.SimpleEntry<String, String>> currencies = ExchangeDataMonitor.getAvailableFiatList(cb_user.getDataSource(), cb_user.getPlatform());
        if(currencies.size() == 0)
            return null;

        InlineKeyboardButton.InlineKeyboardButtonBuilder buttonBuilder = InlineKeyboardButton.builder();
        InlineKeyboardMarkup.InlineKeyboardMarkupBuilder keyboard = InlineKeyboardMarkup.builder();

        int pagesAmount = Math.ceilDiv(currencies.size(), 9);
        page = Math.floorMod(page, pagesAmount);

        ArrayList<InlineKeyboardButton> buttonsRow = new ArrayList<>();
        int temp = 0;
        for (int i = 0; i < 9 &&
                i + (page * 9) < currencies.size(); i++, temp++) {
            buttonsRow.add(getFiatCurrencyButton(currencies.get(i + (page * 9)), page, cb_user, buttonBuilder));
            if ((i + 1) % 3 == 0) {
                keyboard.keyboardRow(buttonsRow);
                buttonsRow = new ArrayList<>();
                temp = 0;
            }
        }

        if(temp > 0)
            keyboard.keyboardRow(buttonsRow);

        if(currencies.size() > 9)
            keyboard.keyboardRow(getMenuNavigationRow(_FIAT_CURRENCY, page));

        if (fullMenu) {
            keyboard.keyboardRow(List.of(backButton(cb_user.getLocale())));
        }

        return new MenuMarkup(CryptoBossLocales.getText(cb_user.getLocale(), "__@Msg_000c__"), keyboard.build());
    }

    public static String getFlagByCountryCode(String name){
        name = validateCountryCode(name);
        if((name = flags.get(name)) == null)
            return "";

        return name;
    }

    public static String validateCountryCode(String countryCode){
        if(countryCode.equalsIgnoreCase("xo"))
            return "ng";
        else if(countryCode.equalsIgnoreCase("xa"))
            return "cf";
        return countryCode.toLowerCase();
    }

    private static InlineKeyboardButton getFiatCurrencyButton(AbstractMap.SimpleEntry<String, String> entry, int page,  CryptoBossUser cb_user, InlineKeyboardButton.InlineKeyboardButtonBuilder buttonBuilder) {
        return buttonBuilder.text(isSelectedButton(cb_user.getFiatCurrency(), entry.getKey(), getFlagByCountryCode(entry.getValue())) + entry.getKey())
                .callbackData("_FIAT_CURRENCY " + entry.getKey() + " " + page).build();
    }

    public static MenuMarkup getCryptocurrencyMenu(Integer page, boolean fullMenu, CryptoBossUser cb_user) throws IOException {
        ArrayList<String> crypto = ExchangeDataMonitor.getAvailableCryptoList(cb_user.getDataSource(), cb_user.getPlatform(), cb_user.getFiatCurrency(), cb_user.getOrdersType());
        if(crypto.size() == 0)
            return null;

        InlineKeyboardButton.InlineKeyboardButtonBuilder buttonBuilder = InlineKeyboardButton.builder();
        InlineKeyboardMarkup.InlineKeyboardMarkupBuilder keyboard = InlineKeyboardMarkup.builder();

        int pagesAmount = Math.ceilDiv(crypto.size(), 9);
        page = Math.floorMod(page, pagesAmount);

        ArrayList<InlineKeyboardButton> buttonsRow = new ArrayList<>();
        int temp = 0;
        for (int i = 0; i < 9 &&
                i + (page * 9) < crypto.size(); i++, temp++) {
            buttonsRow.add(getCryptocurrencyButton(crypto.get(i + (page * 9)), page, cb_user, buttonBuilder));
            if ((i + 1) % 3 == 0) {
                keyboard.keyboardRow(buttonsRow);
                buttonsRow = new ArrayList<>();
                temp = 0;
            }
        }

        if(temp > 0)
            keyboard.keyboardRow(buttonsRow);

        if(crypto.size() > 9)
            keyboard.keyboardRow(getMenuNavigationRow(_CRYPTOCURRENCY, page));

        if (fullMenu && crypto.contains(cb_user.getCryptoCurrency())) {
            keyboard.keyboardRow(List.of(backButton(cb_user.getLocale())));
        }

        return new MenuMarkup(CryptoBossLocales.getText(cb_user.getLocale(), "__@Msg_000d__"), keyboard.build());
    }
    private static InlineKeyboardButton getCryptocurrencyButton(String cryptoCode, int page, CryptoBossUser cb_user, InlineKeyboardButton.InlineKeyboardButtonBuilder buttonBuilder) {
        return buttonBuilder.text(isSelectedButton(cb_user.getCryptoCurrency(),cryptoCode) + cryptoCode)
                .callbackData("_CRYPTOCURRENCY " + cryptoCode + " " + page).build();
    }
    public static void menuCommand(String text, CryptoBossUser cb_user, Bot bot, boolean fullMenu, Integer messageId) throws IOException {
        String[] data = text.split("\\s+");
        switch (MenuType.valueOf(data[0])) {
            case _MENU -> {
                if(data[1].equals("_BACK"))
                    sendMenu(_MENU, cb_user, bot, false, messageId, 0);
                else
                    sendMenu(MenuType.valueOf(data[1]), cb_user, bot, true, messageId, 0);
            }
            case _LOCALE -> {
                if(cb_user.getLocale() != Locale.valueOf(data[1])) {
                    cb_user.setLocale(Locale.valueOf(data[1]));
                    sendMenu(_LOCALE, cb_user, bot, fullMenu, messageId, 0);
                }
            }
            case _DATA_SOURCE -> {
                if(data[1].equals(String.valueOf(Binance)) && cb_user.getDataSource() != ExchangeDataMonitor.DataSource.valueOf(data[1])) {
                    cb_user.setDataSource(data[1]);
                    sendMenu(_DATA_SOURCE, cb_user, bot, fullMenu, messageId, 0);
                }
            }
            case _PLATFORM -> {
                if(data[1].equals(String.valueOf(P2P)) && cb_user.getPlatform() != ExchangeDataMonitor.Platform.valueOf(data[1])) {
                    cb_user.setPlatform(data[1]);
                    sendMenu(_PLATFORM, cb_user, bot, fullMenu, messageId, 0);
                }
            }
            case _ORDERS_TYPE -> {
                if(cb_user.getOrdersType() != ExchangeDataMonitor.OrderType.valueOf(data[1])) {
                    cb_user.setOrdersType(data[1]);
                    sendMenu(_ORDERS_TYPE, cb_user, bot, fullMenu, messageId, 0);
                }
            }
            case _FIAT_CURRENCY -> {
                if (data[1].equals("_PREV")) {
                    sendMenu(_FIAT_CURRENCY, cb_user, bot, fullMenu, messageId, Integer.parseInt(data[2]));
                } else if(data[1].equals("_NEXT")){
                    sendMenu(_FIAT_CURRENCY, cb_user, bot, fullMenu, messageId, Integer.parseInt(data[2]));
                } else {
                    if(cb_user.getFiatCurrency() == null || !cb_user.getFiatCurrency().equals(data[1])) {
                        cb_user.setFiatCurrency(data[1]);
                        if (!ExchangeDataMonitor.getAvailableCryptoList(cb_user.getDataSource(), cb_user.getPlatform(), cb_user.getFiatCurrency(), cb_user.getOrdersType()).contains(cb_user.getCryptoCurrency())) {
                            cb_user.setCryptoCurrency(null);
                            sendMenu(_CRYPTOCURRENCY, cb_user, bot, false, messageId, 0);
                        } else
                            sendMenu(_FIAT_CURRENCY, cb_user, bot, fullMenu, messageId, Integer.parseInt(data[2]));
                    }
                }
            }
            case _CRYPTOCURRENCY -> {
                if (data[1].equals("_PREV")) {
                    sendMenu(_CRYPTOCURRENCY, cb_user, bot, fullMenu, messageId, Integer.parseInt(data[2]));
                } else if(data[1].equals("_NEXT")){
                    sendMenu(_CRYPTOCURRENCY, cb_user, bot, fullMenu, messageId, Integer.parseInt(data[2]));
                } else {
                    if(cb_user.getCryptoCurrency() == null || !cb_user.getCryptoCurrency().equals(data[1])) {
                        cb_user.setCryptoCurrency(data[1]);
                        sendMenu(_CRYPTOCURRENCY, cb_user, bot, fullMenu, messageId, Integer.parseInt(data[2]));
                    }
                }
            }
        }
        if (!data[1].equals("_PREV") && !data[1].equals("_NEXT") && !data[1].equals("_BACK"))
            bot.saveUser(cb_user);
    }

    private static List<InlineKeyboardButton> getMenuNavigationRow(MenuType menuType, int currentPage) {
        InlineKeyboardButton prev = InlineKeyboardButton.builder()
                .text("<<").callbackData(menuType + " _PREV " + (currentPage - 1)).build();

        InlineKeyboardButton next = InlineKeyboardButton.builder()
                .text(">>").callbackData(menuType + " _NEXT " + (currentPage + 1)).build();
        return new ArrayList<>(List.of(prev, next));
    }

    public static boolean isNavigationAction(CallbackQuery callbackQuery){
        String command = callbackQuery.getData().split("\\s+")[1];
        if(command.startsWith("_"))
            return command.equals("_PREV") || command.equals("_NEXT");
        return false;
    }

    private static InlineKeyboardButton backButton(Locale locale) {
        return InlineKeyboardButton.builder()
                .text(CryptoBossLocales.getText(locale, "__@Msg_0300__")).callbackData("_MENU _BACK")
                .build();
    }
    private static Message sendMenu(Long userId, String text, InlineKeyboardMarkup ikm, Bot bot){
        SendMessage sm = SendMessage.builder().chatId(userId.toString())
                .parseMode("HTML").text(text)
                .replyMarkup(ikm).build();

        return bot.executeMethod(sm);
    }

    private static void editMenu(Long chatId, int messageId, String text, InlineKeyboardMarkup ikm, Bot bot){
        EditMessageText emt = new EditMessageText(String.valueOf(chatId), messageId, null, text, null, null, ikm, null);
        bot.executeMethod(emt);
    }

    public static MenuType getMenuType(CallbackQuery callbackQuery){
        return Menu.MenuType.valueOf(callbackQuery.getData().split("\\s+")[0]);
    }

    public static String isSelectedButton(Enum firstValue, Enum secondValue){
        return isSelectedButton(String.valueOf(firstValue), String.valueOf(secondValue), "");
    }

    public static String isSelectedButton(String firstValue, String secondValue){
        return isSelectedButton(firstValue, secondValue, "");
    }

    public static String isSelectedButton(String firstValue, String secondValue, String elseValue){
        return  firstValue != null && firstValue.equals(secondValue) ? "⭐️" : elseValue;
    }
}
