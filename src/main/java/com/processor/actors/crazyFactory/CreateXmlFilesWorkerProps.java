package com.processor.actors.crazyFactory;

import akka.actor.ActorRef;
import akka.actor.Props;
import com.processor.actors.workers.xmlfile.XmlFileCreatorWorker;

public class CreateXmlFilesWorkerProps extends AbstractFileWorkerProps {

    @Override public Props props(ActorRef filesWorkerActor) {
        return XmlFileCreatorWorker.props();
    }
}
