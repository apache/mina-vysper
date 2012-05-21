package org.apache.vysper.xmpp.modules.extension.xep0045_muc;

/**
 */
public class MUCFeatures {

    protected boolean rewriteDuplicateNick = true;

    protected int maxRoomHisotryItems = 20;

    public boolean isRewriteDuplicateNick() {
        return rewriteDuplicateNick;
    }

    public void setRewriteDuplicateNick(boolean rewriteDuplicateNick) {
        this.rewriteDuplicateNick = rewriteDuplicateNick;
    }

    public int getMaxRoomHistoryItems() {
        return maxRoomHisotryItems;
    }

    public void setMaxRoomHistoryItems(int maxRoomHisotryItems) {
        this.maxRoomHisotryItems = maxRoomHisotryItems;
    }
}
