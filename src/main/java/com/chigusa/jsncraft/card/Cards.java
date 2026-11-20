package com.chigusa.jsncraft.card;

import java.util.HashMap;
import java.util.Map;

public final class Cards {
    private static Map<String, CardList.Card> BY_ID;

    public static CardList.Card get(String id) {
        if (BY_ID == null) {
            BY_ID = new HashMap<>();
            for (CardList.Card c : CardList.ALL) BY_ID.put(c.id(), c);
        }
        return BY_ID.get(id);
    }

    private Cards() {}
}
