package cryptobossbot;

public enum Locale {
    EN("EN"),
    RU("RU"),
    UA("UA");
    private final String value;

    Locale(String value) {
        this.value = value;
    }

    public static String getLocaleFlag(Locale locale){
        return switch (locale) {
            case EN -> "\uD83C\uDDEC\uD83C\uDDE7";
            case UA -> "\uD83C\uDDFA\uD83C\uDDE6";
            case RU -> "\uD83C\uDDF7\uD83C\uDDFA";
        };
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
