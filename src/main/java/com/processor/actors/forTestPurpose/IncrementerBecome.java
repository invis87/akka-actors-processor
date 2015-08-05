package com.processor.actors.forTestPurpose;

import akka.actor.UntypedActor;
import akka.japi.Procedure;
import com.processor.messages.IntMessage;

public class IncrementerBecome extends UntypedActor {
    @Override public void onReceive(Object message) throws Exception {
        counter(0);
    }

    private void counter(final int i) {
        getContext().become(new Procedure<Object>(){
            @Override public void apply(Object param){
                if(param == "get"){
                    getSender().tell(new IntMessage(i), getSelf());
                }
                if(param == "inc") {
                    counter(i+1);
                }
            }
        });
    }
}
