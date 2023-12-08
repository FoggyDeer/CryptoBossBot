package cryptobossbot;

import org.telegram.telegrambots.meta.api.objects.User;

import java.util.HashSet;
import java.util.Set;

public class CryptoBossUser extends User{
    public static final int MAX_TRIES_TO_LOGIN = 3;
    private String username;
    private Locale locale;
    private int triesToLogin = MAX_TRIES_TO_LOGIN;
    private ExchangeDataMonitor.DataSource dataSource;
    private ExchangeDataMonitor.Platform platform;
    private ExchangeDataMonitor.OrderType ordersType;
    private String fiatCurrency;
    private String cryptoCurrency;
    private final Set<String> priorityBanks = new HashSet<>();
    private boolean isBanned = false;
    private boolean isLoggedIn = false;
    private boolean isAuthorized = false;

    private Integer lastMenuId;

    public CryptoBossUser(User user) {
        super(user.getId(), user.getFirstName(), user.getIsBot(), user.getLastName(), user.getUserName(), user.getLanguageCode(),user.getCanJoinGroups(), user.getCanReadAllGroupMessages(), user.getSupportInlineQueries(), user.getIsPremium(), user.getAddedToAttachmentMenu());
    }

    public Locale getLocale() {
        return locale;
    }

    public String getCBUserName() {
        return username;
    }

    public int getTriesToLogin() {
        return triesToLogin;
    }

    public ExchangeDataMonitor.DataSource getDataSource() {
        return dataSource;
    }

    public ExchangeDataMonitor.Platform getPlatform() {
        return platform;
    }

    public ExchangeDataMonitor.OrderType getOrdersType() {
        return ordersType;
    }

    public String getFiatCurrency() {
        return fiatCurrency;
    }

    public String getCryptoCurrency() {
        return cryptoCurrency;
    }

    public Set<String> getPriorityBanks() {
        return priorityBanks;
    }

    public boolean isBanned(){
        return isBanned;
    }

    public boolean isLoggedIn() {
        return isLoggedIn;
    }

    public boolean isAuthorized() {
        return isAuthorized;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void decreaseTries(){
        if(triesToLogin > 0) triesToLogin--;
    }

    public void resetTries(){
        triesToLogin = MAX_TRIES_TO_LOGIN;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = ExchangeDataMonitor.DataSource.valueOf(dataSource);
    }

    public void setPlatform(String platform) {
        this.platform = ExchangeDataMonitor.Platform.valueOf(platform);
    }

    public void setOrdersType(String ordersType) {
        this.ordersType = ExchangeDataMonitor.OrderType.valueOf(ordersType);
    }

    public void setFiatCurrency(String fiatCurrency) {
        this.fiatCurrency = fiatCurrency;
    }

    public void setCryptoCurrency(String cryptoCurrency) {
        this.cryptoCurrency = cryptoCurrency;
    }

    public void addPriorityBank(String bank){
        this.priorityBanks.add(bank);
    }

    public void removePriorityBank(String bank){
        this.priorityBanks.remove(bank);
    }

    public void clearPriorityBanks(){
        this.priorityBanks.clear();
    }

    public void setLoggedIn(){
        this.isLoggedIn = true;
    }

    public void authorize(){
        this.isLoggedIn = true;
        this.isAuthorized = true;
    }

    public void ban(){
        this.isLoggedIn = false;
        this.isBanned = true;
    }

    public void unban(){
        this.isLoggedIn = false;
        this.isBanned = false;
        this.triesToLogin = MAX_TRIES_TO_LOGIN;
    }

    public void reset(){
        username = null;
        locale = null;
        triesToLogin = MAX_TRIES_TO_LOGIN;
        priorityBanks.clear();
        isLoggedIn = false;
        isAuthorized = false;
        dataSource = null;
        platform = null;
        ordersType = null;
        fiatCurrency = null;
        cryptoCurrency = null;
    }

    public Integer getLastMenuId() {
        return lastMenuId;
    }

    public void setLastMenuId(Integer lastMenuId) {
        this.lastMenuId = lastMenuId;
    }

    public boolean isAvailable(){
        return locale != null && dataSource != null && platform != null && ordersType != null && fiatCurrency != null && cryptoCurrency != null;
    }

    @Override
    public String toString() {
        return getFirstName()
                + " (ID: " + getId() + (getUserName() != null && getUserName().length() > 0 ? ", " + getUserName() : "")
                + (username != null && username.length() > 0 ? ", username: " + username : "")
                + ")";
    }
}
