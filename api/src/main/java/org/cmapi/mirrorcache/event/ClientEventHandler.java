package org.cmapi.mirrorcache.event;

public interface ClientEventHandler extends EventHandler {
    
    void onMessage(ClientMessageEvent event);
    void onConnect(ClientConnectEvent event);
    void onDisconnect(ClientDisconnectEvent event);

}
