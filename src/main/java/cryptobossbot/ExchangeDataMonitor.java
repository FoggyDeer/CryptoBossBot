package cryptobossbot;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class ExchangeDataMonitor {
    public enum DataSource {
        Binance("Binance"),
        ByBit("ByBit");

        private final String value;
        DataSource(String value) {this.value = value;}

        @Override
        public String toString() {
            return value;
        }
    }
    public enum Platform{
        Spot("Spot"),
        P2P("P2P");

        private final String value;
        Platform(String value) {this.value = value;}

        @Override
        public String toString() {
            return value;
        }
    }
    public enum OrderType{
        BUY("BUY"),
        SELL("SELL"),
        BOTH("BOTH");

        private final String value;
        OrderType(String value) {this.value = value;}

        @Override
        public String toString() {
            return value;
        }
    }
    private final static String lastBuyOrdersFilename = "LastBUYOrders.bin";
    private final static String lastSellOrdersFilename = "LastSELLOrders.bin";
    private Thread p2pMonitor;
    private Thread fiatCurrencyMonitor;
    private static final ArrayList<String> preferredCurrencies= new ArrayList<>(List.of("EUR", "USD", "JPY", "PLN", "UAH", "CZK", "GBP", "CNY", "CAD"));
    private static Map<DataSource, Map<Platform, ArrayList<AbstractMap.SimpleEntry<String, String>>>> availableFiatCurrency = new HashMap<>();
    private static final Set<String> acceptableBankList = new HashSet<>(List.of("Monobank", "Privat Bank", "A-Bank", "PUMB", "Raiffeisen Bank Aval"));
    public static class BinanceOrder implements Serializable{
        private String tradeType;
        private String nickname;
        private String price;
        private String currency;
        private String cryptoCurrency;
        private String minVal;
        private String maxVal;
        private String fiatSymbol;
        private final Set<String> bankList = new HashSet<>();

        public String getTradeType() {
            return tradeType;
        }

        public String getNickname() {
            return nickname;
        }

        public String getPrice() {
            return price;
        }

        public String getCurrency() {
            return currency;
        }

        public String getCryptoCurrency() {
            return cryptoCurrency;
        }

        public String getMinVal() {
            return minVal;
        }

        public String getMaxVal() {
            return maxVal;
        }

        public String getFiatSymbol() {
            return fiatSymbol;
        }

        public Set<String> getBankList() {
            return bankList;
        }

        public void setTradeType(String tradeType) {
            this.tradeType = tradeType;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }

        public void setPrice(String price) {
            this.price = price;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }

        public void setCryptoCurrency(String cryptoCurrency){
            this.cryptoCurrency = cryptoCurrency;
        }

        public void setMinVal(String minVal) {
            this.minVal = minVal;
        }

        public void setMaxVal(String maxVal) {
            this.maxVal = maxVal;
        }

        public void setFiatSymbol(String fiatSymbol) {
            this.fiatSymbol = fiatSymbol;
        }

        public void addBankName(String bankName) {
            this.bankList.add(bankName);
        }

        public static String getFormattedNumber(String num){
            String[] separated = num.split("\\.");
            StringBuilder temp = new StringBuilder();
            if(separated[0].charAt(0) == '+' || separated[0].charAt(0) == '-'){
                temp.append(separated[0].charAt(0));
                separated[0] = separated[0].substring(1);
            }
            for(int i = 0, j = separated[0].length(); i < j; i++){
                if(i > 0 && (j - i) % 3 == 0){
                    temp.append(',').append(separated[0].charAt(i));
                } else {
                    temp.append(separated[0].charAt(i));
                }
            }
            temp.append('.');
            if(separated[1].length() == 1 && separated[1].charAt(0) != '0'){
                temp.append(separated[1].charAt(0)).append('0');
            }
            else {
                if (separated[1].length() > 2) {
                    if (separated[1].charAt(2) >= '5') {
                        temp.append(separated[1].charAt(0)).append((char) (separated[1].charAt(1) + 1));
                    } else {
                        temp.append(separated[1], 0, 2);
                    }
                } else {
                    temp.append(separated[1]);
                }
                if (temp.substring(temp.indexOf(".") + 1, temp.length()).equals("00"))
                    temp.deleteCharAt(temp.length() - 1);
            }
            return temp.toString();
        }

        @Override
        public String toString(){
            return "<b>"+this.nickname +
                    "</b>\n-__@Msg_0202__: <b>" + getFormattedNumber(this.price) + "</b> <b>" + this.currency + "</b> __@Msg_0203__ 1 <b>" + this.cryptoCurrency +
                    "</b>\n-__@Msg_0204__: " + "<b>" + getFormattedNumber(this.minVal) + "</b> - <b>" + getFormattedNumber(this.maxVal) + "</b><b>" + this.fiatSymbol +
                    "</b>\n" + this.bankList.stream().filter(bank -> (acceptableBankList.stream().anyMatch(bank::contains))).map(bank -> "● "+bank).collect(Collectors.joining(", "));
        }
    }

    private void initCurrenciesMap(){
        for(DataSource source : DataSource.values()){
            availableFiatCurrency.put(source, new HashMap<>());
            for(Platform platform : Platform.values()){
                availableFiatCurrency.get(source).put(platform, new ArrayList<>());
            }
        }
    }

    ExchangeDataMonitor(int delay, UpdateMonitor updateMonitor){
        initCurrenciesMap();

       /*p2pMonitor = new Thread(() -> {
            while (true) {
                try {
                    updateMonitor.onTick();
                    validateUpdate( updateMonitor,
                            ExchangeDataMonitor.readLastOrders(lastBuyOrdersFilename),
                            ExchangeDataMonitor.parseOrdersData("BUY", 1,
                                    acceptableBankList.toArray(new String[]{})));

                    validateUpdate( updateMonitor,
                            ExchangeDataMonitor.readLastOrders(lastSellOrdersFilename),
                            ExchangeDataMonitor.parseOrdersData("SELL", 1,
                                    acceptableBankList.toArray(new String[]{})));

                    Thread.sleep(delay);
                } catch (InterruptedException | IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });*/

        fiatCurrencyMonitor = new Thread(() -> {
            while (true) {
                try {
                    updateAvailableFiatList(DataSource.Binance, Platform.P2P);
                    Thread.sleep(30_000);
                } catch (InterruptedException | IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private boolean validateUpdate(UpdateMonitor updateMonitor, ArrayList<ExchangeDataMonitor.BinanceOrder> lastOrders,
                                   ArrayList<ExchangeDataMonitor.BinanceOrder> newOrders) throws IOException {
        String type;
        if(lastOrders == null || lastOrders.isEmpty())
            lastOrders = newOrders;

        if(newOrders == null || newOrders.isEmpty())
            return false;

        type = lastOrders.get(0).tradeType;
        if(!type.equals(newOrders.get(0).tradeType) && !type.equals("BUY") && !type.equals("SELL")) {
            Console.error("Wrong trade type provided.");
            return false;
        }
        String message = null;
        ExchangeDataMonitor.saveLastOrders(newOrders, "Last"+type+"Orders.bin");

        float lPrice = Float.parseFloat(lastOrders.get(0).getPrice());
        float nPrice = Float.parseFloat(newOrders.get(0).getPrice());

        if (lPrice != nPrice) {
            message = ExchangeDataMonitor.binanceDataToString(newOrders,
                    nPrice - lPrice);
            lastOrders = newOrders;
            ExchangeDataMonitor.saveLastOrders(lastOrders, "Last"+type+"Orders.bin");
        }
        if(message != null && !message.equals("")) {
            updateMonitor.onP2PUpdate(message);
            return true;
        }
        return false;
    }

    public void start(){
        //p2pMonitor.start();
        fiatCurrencyMonitor.start();
    }

    public void interrupt(){
        p2pMonitor.interrupt();
    }

    public static String binanceDataToString(ArrayList<BinanceOrder> orders, Float difference){
        String diff = BinanceOrder.getFormattedNumber(difference.toString());
        if(orders.size() > 0){
            StringBuilder sb = new StringBuilder();
            if(orders.get(0).getTradeType().equals("BUY"))
                sb.append("\uD83D\uDFE2__@Msg_0200__\uD83D\uDFE2");
            else
                sb.append("\uD83D\uDD34__@Msg_0201__\uD83D\uDD34");
            sb.append("    <b>").append(difference > 0 ? "+" + diff : diff).append("</b> <b>").append(orders.get(0).currency).append(difference < 0 ? "⬇️" : "⬆️").append("</b>\n");

            for(BinanceOrder order : orders){
                sb.append('\n').append(order).append('\n');
            }
            sb.append('ㅤ');
            return sb.toString();
        }
        return null;
    }

    static void setRequestHeaders(HttpURLConnection con) {
        con.setRequestProperty("accept", "*/*");
        con.setRequestProperty("accept-language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7");
        con.setRequestProperty("bnc-uuid", "95096b4b-a35e-4e7f-af5d-5b45e399ed3e");
        con.setRequestProperty("c2ctype", "c2c_merchant");
        con.setRequestProperty("cache-control", "no-cache");
        con.setRequestProperty("clienttype", "web");
        con.setRequestProperty("content-type", "application/json");
        con.setRequestProperty("csrftoken", "00aecc9216aa7eadc1cdc8954b5ded15");
        con.setRequestProperty("device-info", "eyJzY3JlZW5fcmVzb2x1dGlvbiI6Ijg2NCwxNTM2IiwiYXZhaWxhYmxlX3NjcmVlbl9yZXNvbHV0aW9uIjoiODY0LDE1MzYiLCJzeXN0ZW1fdmVyc2lvbiI6IldpbmRvd3MgMTAiLCJicmFuZF9tb2RlbCI6InVua25vd24iLCJzeXN0ZW1fbGFuZyI6InJ1LVJVIiwidGltZXpvbmUiOiJHTVQrMDI6MDAiLCJ0aW1lem9uZU9mZnNldCI6LTEyMCwidXNlcl9hZ2VudCI6Ik1vemlsbGEvNS4wIChXaW5kb3dzIE5UIDEwLjA7IFdpbjY0OyB4NjQpIEFwcGxlV2ViS2l0LzUzNy4zNiAoS0hUTUwsIGxpa2UgR2Vja28pIENocm9tZS8xMTYuMC4wLjAgU2FmYXJpLzUzNy4zNiBPUFIvMTAyLjAuMC4wIiwibGlzdF9wbHVnaW4iOiJQREYgVmlld2VyLENocm9tZSBQREYgVmlld2VyLENocm9taXVtIFBERiBWaWV3ZXIsTWljcm9zb2Z0IEVkZ2UgUERGIFZpZXdlcixXZWJLaXQgYnVpbHQtaW4gUERGIiwiY2FudmFzX2NvZGUiOiJmM2YzYjk4ZCIsIndlYmdsX3ZlbmRvciI6Ikdvb2dsZSBJbmMuIChJbnRlbCkiLCJ3ZWJnbF9yZW5kZXJlciI6IkFOR0xFIChJbnRlbCwgSW50ZWwoUikgSEQgR3JhcGhpY3MgNjIwIERpcmVjdDNEMTEgdnNfNV8wIHBzXzVfMCwgRDNEMTEpIiwiYXVkaW8iOiIxMjQuMDQzNDc1Mjc1MTYwNzQiLCJwbGF0Zm9ybSI6IldpbjMyIiwid2ViX3RpbWV6b25lIjoiRXRjL0dNVC0yIiwiZGV2aWNlX25hbWUiOiJPcGVyYSBWMTAyLjAuMC4wIChXaW5kb3dzKSIsImZpbmdlcnByaW50IjoiZjc3YjkwNjQwOTAyZTMyZTg0MTkwMTZkMzEwMDFkYTUiLCJkZXZpY2VfaWQiOiIiLCJyZWxhdGVkX2RldmljZV9pZHMiOiIxNjk3MjQxMDU5MzE1SWpDQ1V6R3dkUFNIRWRFZTZrWiJ9");
        con.setRequestProperty("fvideo-id", "339559001d08a1d1a8e967f35c7650ba55181cfa");
        con.setRequestProperty("fvideo-token", "ADcOsN8oYNEfTm9FLirE7OJLD0LBjBqRQKqRiykM2cX8xCTgMlGBnABdGdUpOSLzOe5dLgPGV5jGPe07iM60Q7diZTyZitENKrI+5tH5buFgYde6lmb5bHT5l9os4gtq0MVF0lLlgvYtoH+QMWELITcPNoUy7STXj2qzPR92K8B4A5rPPLqIQP5hOMO7TUKlQ=06");
        con.setRequestProperty("lang", "en");
        con.setRequestProperty("pragma", "no-cache");
        con.setRequestProperty("sec-ch-ua", "\\\"Chromium\\\";v=\\\"116\\\", \\\"Not)A;Brand\\\";v=\\\"24\\\", \\\"Opera GX\\\";v=\\\"102\\\"");
        con.setRequestProperty("sec-ch-ua-mobile", "?0");
        con.setRequestProperty("sec-ch-ua-platform", "\\\"Windows\\\"");
        con.setRequestProperty("sec-fetch-dest", "empty");
        con.setRequestProperty("sec-fetch-mode", "cors");
        con.setRequestProperty("sec-fetch-site", "same-origin");
        con.setRequestProperty("x-passthrough-token", "");
        con.setRequestProperty("x-trace-id", "b6f99f87-8bbe-4c0e-8f06-3289352d81e0");
        con.setRequestProperty("x-ui-request-trace", "b6f99f87-8bbe-4c0e-8f06-3289352d81e0");
        con.setDoOutput(true);
    }

    public static String sendRequest(String url, String method, String body) throws IOException {
        URL Url = new URL(url);
        HttpURLConnection con = (HttpURLConnection) Url.openConnection();
        con.setRequestMethod(method);

        //Initialising HTTP headers
        setRequestHeaders(con);

        try (OutputStream os = con.getOutputStream()) {
            byte[] input = body.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        InputStreamReader isr = new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader(isr);
        StringBuilder response = new StringBuilder();
        String responseLine;
        while ((responseLine = br.readLine()) != null) {
            response.append(responseLine.trim());
        }
        return response.toString();
    }

    public static String sendRequest(String url, String method) throws IOException {
        return sendRequest(url, method, "{}");
    }

    public static JSONArray getOrdersData(String type, int page, String... bankNames) throws IOException, WrongTradeTypeException {
        if(!Objects.equals(type, "SELL") && !type.equals("BUY"))
            throw new WrongTradeTypeException();

        String temp = Arrays.toString(Arrays.stream(bankNames).map(s -> '"'+s+'"').toArray(String[]::new));
        String jsonInputString = "{\"fiat\":\"UAH\",\"page\":"+page+",\"rows\":20,\"tradeType\":\""+type+"\",\"asset\":\"BTC\",\"countries\":[],\"proMerchantAds\":false,\"shieldMerchantAds\":false,\"publisherType\":null,\"payTypes\":"+ temp +",\"classifies\":[\"mass\",\"profession\"]}";

        String response = sendRequest("https://p2p.binance.com/bapi/c2c/v2/friendly/c2c/adv/search", "POST", jsonInputString);

        if(response.length() > 0)
            return new JSONObject(response).getJSONArray("data");
        else
            return new JSONArray();
    }


    private static void updateAvailableFiatList(DataSource source, Platform platform) throws IOException {
        ArrayList<AbstractMap.SimpleEntry<String, String>> favoriteCurrencies = new ArrayList<>();
        ArrayList<AbstractMap.SimpleEntry<String, String>> currencies = new ArrayList<>();

        String response;
        if(source == DataSource.Binance && platform == Platform.P2P)
            response = sendRequest("https://p2p.binance.com/bapi/c2c/v1/friendly/c2c/trade-rule/fiat-list", "POST");
        else
            return;

        if(response.length() > 0) {
            JSONArray jsonArray = new JSONObject(response).getJSONArray("data");
            for(String currCode : preferredCurrencies){
                for(int i = 0; i < jsonArray.length(); i++) {
                    if (jsonArray.getJSONObject(i).getString("currencyCode").equals(currCode)) {
                        favoriteCurrencies.add(new AbstractMap.SimpleEntry<>(jsonArray.getJSONObject(i).getString("currencyCode"), jsonArray.getJSONObject(i).getString("countryCode")));
                        jsonArray.remove(i);
                        break;
                    }
                }
            }
            for(int i = 0; i < jsonArray.length(); i++){
                currencies.add(new AbstractMap.SimpleEntry<>(jsonArray.getJSONObject(i).getString("currencyCode"), jsonArray.getJSONObject(i).getString("countryCode")));
            }
            currencies.sort(Comparator.comparingInt(o -> o.getKey().charAt(0)));
            favoriteCurrencies.addAll(currencies);
        }
        availableFiatCurrency.get(source).put(Platform.P2P, favoriteCurrencies);
    }

    public static ArrayList<AbstractMap.SimpleEntry<String, String>> getAvailableFiatList(DataSource source, Platform platform){
        ArrayList<AbstractMap.SimpleEntry<String, String>> list = null;
        try {
            while (availableFiatCurrency == null ||
                    availableFiatCurrency.get(source) == null ||
                    (list = availableFiatCurrency.get(source).get(platform)) == null) {
                Thread.sleep(1000);
            }
        }catch (InterruptedException ignored){}
        return list;
    }

    public static ArrayList<String> getAvailableCryptoList(DataSource source, Platform platform, String fiat, OrderType orderType) throws IOException {
        ArrayList<String> crypto = new ArrayList<>();
        String response;
        if(source == DataSource.Binance && platform == Platform.P2P)
            response = sendRequest("https://p2p.binance.com/bapi/c2c/v2/friendly/c2c/portal/config", "POST", "{fiat: \""+fiat+"\"}");
        else
            return crypto;

        if(orderType == OrderType.BOTH)
            orderType = OrderType.BUY;

        JSONArray jsonArray = new JSONObject(response).getJSONObject("data").getJSONArray("areas");
        jsonArray = getJSONArrayElemHaving(jsonArray, "area", String.valueOf(platform)).getJSONArray("tradeSides");
        jsonArray = getJSONArrayElemHaving(jsonArray, "side", String.valueOf(orderType)).getJSONArray("assets");
        for(int i = 0; i < jsonArray.length(); i++){
            crypto.add(jsonArray.getJSONObject(i).getString("asset"));
        }
        return crypto;
    }

    private static JSONObject getJSONArrayElemHaving(JSONArray jsonArray, String key, String value){
        JSONObject obj = null;
        for(int i = 0; i < jsonArray.length(); i++){
            obj = jsonArray.getJSONObject(i);
            if(obj.getString(key).equals(String.valueOf(value)))
                break;
        }
        return obj;
    }

    public static ArrayList<BinanceOrder> parseOrdersData(String type, int page, String... bankNames){
        ArrayList<BinanceOrder> result = new ArrayList<>();
        try {
            JSONArray arr = getOrdersData(type, page, bankNames);

            BinanceOrder binanceOrder;

            for (int i = 0; i < 3; i++) {

                JSONObject adv = arr.getJSONObject(i).getJSONObject("adv");
                JSONObject advertiser = arr.getJSONObject(i).getJSONObject("advertiser");

                binanceOrder = new BinanceOrder();

                String userNickname = advertiser.getString("nickName");
                binanceOrder.setNickname(userNickname);

                binanceOrder.setTradeType(adv.getString("tradeType").equals("BUY") ? "SELL" : "BUY");
                binanceOrder.setPrice(adv.getString("price"));
                binanceOrder.setCurrency(adv.getString("fiatUnit"));
                binanceOrder.setCryptoCurrency(adv.getString("asset"));
                binanceOrder.setMinVal(adv.getString("minSingleTransAmount"));
                binanceOrder.setMaxVal(adv.getString("dynamicMaxSingleTransAmount"));
                binanceOrder.setFiatSymbol(adv.getString("fiatSymbol"));
                JSONArray temp = adv.getJSONArray("tradeMethods");
                try {
                    for (int j = 0; j < temp.length(); j++) {
                        binanceOrder.addBankName(temp.getJSONObject(j).getString("tradeMethodName"));
                    }
                }catch (org.json.JSONException ignored){}

                result.add(binanceOrder);
            }
            return result;
        }catch (IOException | WrongTradeTypeException e){
            return null;
        }
    }

    public static ArrayList<BinanceOrder> readLastOrders(String filename) throws IOException{
        ArrayList<BinanceOrder> lastOrders = new ArrayList<>();
        try {
            FileInputStream fis = new FileInputStream(filename);
            ObjectInputStream ois = new ObjectInputStream(fis);
            lastOrders = (ArrayList<BinanceOrder>) ois.readObject();
            ois.close();
            fis.close();
        }catch (FileNotFoundException | ClassNotFoundException | ClassCastException e){
            FileOutputStream fos = new FileOutputStream(filename);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(lastOrders);
            oos.close();
            fos.close();
        }
        return lastOrders;
    }

    public static void saveLastOrders(ArrayList<BinanceOrder> lastOrders, String filename) throws IOException {
        FileOutputStream fos = new FileOutputStream(filename);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(lastOrders);
        oos.close();
        fos.close();
    }

    public static class WrongTradeTypeException extends Exception{}
}
