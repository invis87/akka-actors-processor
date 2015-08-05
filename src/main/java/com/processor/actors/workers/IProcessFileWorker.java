package com.processor.actors.workers;

import com.processor.messages.IMessage;

import java.io.Serializable;

public interface IProcessFileWorker {

    //    ### MESSAGES ###

    public static class SuccessMessage implements IMessage, Serializable {}

    public static class FailureMessage implements IMessage, Serializable {
        private String reason;
        private Throwable failure;

        public FailureMessage(String reason, Throwable failure) {
            this.reason = reason;
            this.failure = failure;
        }

        public String getReason() {
            return reason;
        }

        public Throwable getFailure() {
            return failure;
        }
    }

    public static class WrongFormatMessage implements IMessage, Serializable {
        private final String fileName;

        public WrongFormatMessage(String fileName) {
            this.fileName = fileName;
        }

        public String getFileName() {
            return fileName;
        }
    }

    public static class ProcessFileMessage implements IMessage, Serializable {
        private final String fileAbsolutePath;

        public ProcessFileMessage(String fileAbsolutePath) {
            this.fileAbsolutePath = fileAbsolutePath;
        }

        public String getFileAbsolutePath() {
            return fileAbsolutePath;
        }
    }

    public static class MoveFileMessage implements IMessage, Serializable {
        private final String filePath;

        public MoveFileMessage(String filePath) {
            this.filePath = filePath;
        }

        public String getFilePath() {
            return filePath;
        }
    }
}
