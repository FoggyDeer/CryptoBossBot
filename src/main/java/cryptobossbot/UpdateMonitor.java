package cryptobossbot;

public interface UpdateMonitor {
    void onP2PUpdate(String message);

    void onTick();
}
