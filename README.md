# akka-actors-processor

This is a experimental java application that uses akka.actors.
There 2 main classes here:

#### 1. com.processor.CreateXmlFiles
  Create xml files in chosen directory. 
  Number of files is second parameter.
  Start file index is the third parameter.
  Output files looks like this: good(fileIndex + startFileIndex).xml

#### 2. com.processor.ProcessXmlFiles
  Processed files in chosen directory. That's mean - read file, save it somewhere and move it.
  Files will be saved in PostgreDB, `sql` directory have scripts to create DB and Table.
  


# Build

Clone project, install JDK 1.7+. In Windows run `gradlew.bat build` in Unix run `./gradlew build`.

# Run

First of all create some test files to process. Change `build.gradle` `mainClassName` to `'com.processor.CreateXmlFiles'`
and run program with gradlew: `./gradlew run -Pargs="<absolutePathToDirWhereFilesWillBeCreated> 200 0"`

Now you have 200 xml files. To process them change `build.gradle` `mainClassName` to `'com.processor.ProcessXmlFiles'`
and run: `./gradlew run -Pargs="<absolutePathToFilesDirectory> <absolutePathToDirectoryToStoreProcessedFiles>"`
