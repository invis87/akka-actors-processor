package com.processor.actors.crazyFactory;

import akka.actor.ActorRef;
import akka.actor.Props;

public abstract class AbstractFileWorkerProps {
    public abstract Props props(ActorRef filesWorkerActor);
}
