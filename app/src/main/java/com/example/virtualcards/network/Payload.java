package com.example.virtualcards.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Payload {

    /**
     * Operation this payload is supposed to be used for.
     */
    public final NetworkData.Operation operation;
    /**
     * Read only list of data contained in this payload.
     */
    public final List<Object> data;

    Payload(NetworkData.Operation operation, Object... data){
        if(operation == null)throw new IllegalArgumentException("Operation of payload is not allowed to be null.");

        List<Object> tempList = new ArrayList<>();
        Collections.addAll(tempList, data);
        this.data = Collections.unmodifiableList(tempList);

        this.operation = operation;
    }
}
