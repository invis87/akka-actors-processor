package com.processor.actors.workers;

import com.processor.entity.IEntity;
import com.processor.messages.IMessage;

import java.io.Serializable;

public interface ISaveEntityWorker {

    public static class SaveEntityMessage implements IMessage, Serializable {
        private final IEntity entity;

        public SaveEntityMessage(IEntity entity) {
            this.entity = entity;
        }

        public IEntity getEntity() {
            return entity;
        }
    }

    public static class EntityProcessedMessage implements IMessage, Serializable {
        private final boolean isSaved;
        private final Throwable failure;
        private final String entitySourcePath;

        public EntityProcessedMessage(String entitySourcePath) {
            this.entitySourcePath = entitySourcePath;
            this.isSaved = true;
            this.failure = null;
        }

        public EntityProcessedMessage(Throwable failure) {
            this.isSaved = false;
            this.failure = failure;
            this.entitySourcePath = "";
        }

        public boolean isSaved() {
            return isSaved;
        }

        public Throwable getFailure() {
            return failure;
        }

        public String getEntitySourcePath() {
            return entitySourcePath;
        }
    }
}
