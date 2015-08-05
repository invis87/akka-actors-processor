package com.processor.messages;

import java.io.Serializable;

public class IntMessage implements IMessage, Serializable {
    private final int i;

    public IntMessage(int i){
        this.i = i;
    }

    public int getI() {
        return i;
    }
}
