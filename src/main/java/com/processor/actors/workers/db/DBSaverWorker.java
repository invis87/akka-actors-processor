package com.processor.actors.workers.db;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.Creator;
import akka.japi.Procedure;
import com.processor.actors.HashMapAbstractActor;
import com.processor.sql.XmlSqlQueries;
import com.processor.entity.IEntity;
import com.processor.entity.XMLEntity;
import com.processor.messages.IMessage;
import org.apache.commons.dbcp2.BasicDataSource;
import scala.Option;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DBSaverWorker extends HashMapAbstractActor {
    public static Props props(final ActorRef poolActor, final BasicDataSource dataSource) {
        return Props.create(new Creator<DBSaverWorker>() {
            @Override
            public DBSaverWorker create() {
                return new DBSaverWorker(poolActor, dataSource);
            }
        });
    }

    private final ActorRef poolActor;
    private final BasicDataSource dataSource;
    private Connection connection;

    public DBSaverWorker(ActorRef poolActor, BasicDataSource dataSource) {
        this.poolActor = poolActor;
        this.dataSource = dataSource;
    }

    @Override public void preStart() throws Exception {
        super.preStart();

        if (connection != null) {
            connection.close();
        }

        connection = dataSource.getConnection();
    }

    @Override public void preRestart(Throwable reason, Option<Object> message) throws Exception {
        super.preRestart(reason, message);

        if (message.nonEmpty()) {
            SaveEntityMessage msg = (SaveEntityMessage) message.get();
            poolActor.tell(new FailToSaveEntityMessage(reason, msg.getRequester()), getSelf());
        }

        if (connection != null) {
            connection.close();
        }
    }

    @Override public void postStop() throws Exception {
        super.postStop();

        if (connection != null) {
            connection.close();
        }
    }

    @Override protected void fillMessageProcessors() {

        messageHandlers.put(SaveEntityMessage.class, SaveEntityMessageHandler());
    }


//    ### HANDLERS ###

    private Procedure<IMessage> SaveEntityMessageHandler() {
        return new Procedure<IMessage>() {
            public void apply(IMessage message) throws SQLException {

                SaveEntityMessage msg = (SaveEntityMessage) message;
                XMLEntity entity = (XMLEntity) msg.getEntity();

                log.info("Start saving entity {} to DB", entity.toString());

                String selectXmlEntity = XmlSqlQueries.SelectXmlEntityQuery(entity);
                Statement connStatement = connection.createStatement();
                ResultSet selectResult = connStatement.executeQuery(selectXmlEntity);

                if (selectResult.next()) {

                    log.info("entity {} already in DB", entity.hashCode());
                    getSender().tell(new EntitySavedMessage(msg.getRequester(), entity.getSourcePath()), getSelf());
                } else {

                    String insertQuery = XmlSqlQueries.InsertXmlEntityQuery(entity);
                    int updateResult = connStatement.executeUpdate(insertQuery);

                    log.info("entity {} saved to DB with result {}", entity.hashCode(), updateResult);
                    getSender().tell(new EntitySavedMessage(msg.getRequester(), entity.getSourcePath()), getSelf());
                }

                connStatement.close();
            }
        };
    }


    //    ### MESSAGES ###

    public static class SaveEntityMessage implements IMessage, Serializable {
        private final IEntity entity;
        private final ActorRef requester;

        public SaveEntityMessage(IEntity entity, ActorRef requester) {
            this.entity = entity;
            this.requester = requester;
        }

        public IEntity getEntity() {
            return entity;
        }

        public ActorRef getRequester() {
            return requester;
        }
    }

    public static class EntitySavedMessage implements IMessage, Serializable {
        private final ActorRef requester;
        private final String sourcePath;

        public EntitySavedMessage(ActorRef requester, String sourcePath) {
            this.requester = requester;
            this.sourcePath = sourcePath;
        }

        public ActorRef getRequester() {
            return requester;
        }

        public String getSourcePath() {
            return sourcePath;
        }
    }

    public static class FailToSaveEntityMessage implements IMessage, Serializable {
        private final Throwable failure;
        private final ActorRef requester;

        public FailToSaveEntityMessage(Throwable failure, ActorRef requester) {
            this.failure = failure;
            this.requester = requester;
        }

        public Throwable getFailure() {
            return failure;
        }

        public ActorRef getRequester() {
            return requester;
        }
    }

}
