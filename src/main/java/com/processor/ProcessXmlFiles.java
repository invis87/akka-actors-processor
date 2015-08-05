package com.processor;

import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.processor.actors.ParseXMLFileToDataBaseActor;

import java.io.File;

public class ProcessXmlFiles {

    public static void main(String[] args) {

        if (args.length != 2) {
            System.out.println("Please specify directory with .xml files and directory to store processed .xml files.");
            System.exit(0);
        }

        String incomingDirPath = args[0];
        String outgoingDirPath = args[1];
        if(!CheckDirs(incomingDirPath, outgoingDirPath)) {
            System.out.println("One of parameters in not exists or not a directory.");
            System.exit(0);
        }

        ActorSystem system = ActorSystem.create("system");
        ActorRef mainActor = system.actorOf(ParseXMLFileToDataBaseActor.props(incomingDirPath, outgoingDirPath).withDispatcher("main-dispatcher"), "mainActor");
        system.actorOf(Props.create(Terminator.class, mainActor), "terminator");

        mainActor.tell(new ParseXMLFileToDataBaseActor.StartMessage(), ActorRef.noSender());
    }


    public static class Terminator extends UntypedActor {

        private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
        private final ActorRef ref;

        public Terminator(ActorRef ref) {
            this.ref = ref;
            getContext().watch(ref);
        }

        @Override
        public void onReceive(Object msg) {
            if (msg instanceof Terminated) {
                log.info("{} has terminated, shutting down system", ref.path());
                getContext().system().shutdown();
            } else {
                unhandled(msg);
            }
        }
    }

    private static boolean CheckDirs(String incoming, String outgoing) {
        File inc = new File(incoming);
        File out = new File(outgoing);
        return inc.exists() && inc.isDirectory() &&
                out.exists() && out.isDirectory();
    }
}
