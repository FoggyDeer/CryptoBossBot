package cryptobossbot;

import com.vdurmont.emoji.Emoji;
import com.vdurmont.emoji.EmojiManager;

import java.io.*;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class Main {
    public static void main(String[] args) throws IOException, ExchangeDataMonitor.WrongTradeTypeException {

        new Bot("6470608883:AAEwA8bs2fjvlWz4DRDvp7HDcilHPCsNm_g", (bot) -> {
            Console.success("Bot successfully created.");
            bot.printAllUsers();

            ExchangeDataMonitor exchangeDataMonitor = new ExchangeDataMonitor(10_000, new UpdateMonitor() {
                @Override
                public void onP2PUpdate(String message) {
                    bot.notifyAll(message);
                }

                @Override
                public void onTick() {

                }
            });
            exchangeDataMonitor.start();
            Console console = new Console(bot);
        });
    }
}