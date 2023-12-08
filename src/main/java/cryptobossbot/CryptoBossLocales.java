package cryptobossbot;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.util.Scanner;

public class CryptoBossLocales {
    public static String getText(Locale locale, String command){
        try {
            JSONObject obj = readLocale(locale);
            return obj.getString(command);
        } catch (NullPointerException e){
            Console.error("No such file found.");
            return "";
        } catch (JSONException e){
            Console.error("Wrong locale file format.");
            return "";
        }
    };
    public static JSONObject readLocale(Locale locale) throws JSONException {
        try {
            File file = new File("locales\\"+locale + ".json");

            if(file.exists() && !file.isDirectory()) {
                Scanner scanner = new Scanner(file);
                StringBuilder sb = new StringBuilder();
                while (scanner.hasNext()) {
                    sb.append(scanner.nextLine());
                }
                return new JSONObject(sb.toString());
            }

            file.createNewFile();
        } catch (IOException ignored){}

        throw new NullPointerException();
    }
}
