# Sleep Tracker #
This is a sleep quality tracker app, that stores the sleep data over time is a database. It stores start time, end time and quality of sleep. The app architecture is based on MVVM architecture and uses ROOM database.

This app demonstrates the following views and techniques:
* Room database
* DAO
* Coroutines

It also uses:
* Transformation map
* Data Binding in XML files
* ViewModel Factory
* Using Backing Properties to protect MutableLiveData
* Observable state LiveData variables to trigger navigation

## ROOM ##
Room is a database library that is part of Android Jetpack. It is database layer built on top of SQLite database. Below are some terms related to databases and Room.

**Entity** - Object or concept to store in database. Entity class represented by a data class defines a table and each object instance of it is stored as a row in the table. And, each property of the class defines a column of the table.
For example, in this app Sleep data of one night acts as an entity.

**Query**
Query is a request for data from a database table(s), or a request to perform actions on the data.
For example, in this app we can add or delete sleep data in our database. 

Using Room we define each entity as a data class and all queries as an interface. We then use annotations to add metadata to both. Room uses the annotated classes to create tables and perform queries on the database.

**DAO** - Data Access Object or DAO is an annotated class, that contains interfaces to perform queries on the database. We use Kotlin functions for that map to SQL queries.

## Room Annotations ##
1. `@Entity(tableName = "name_of_table")` - It annotates a data class as an Entity representing a table in the database. 

2. `@PrimaryKey` - It is used against a property that will act as primary key of the table.

3. `@ColumnInfo` - It annotates the properties of a data class as columns of the table.

4. `@Insert` - Annotates method signature in interface, that is used to insert an item in database table.

5. `@Delete` - To annotate a method signature for deleting a record in table.

6. `@Update` - To annotate a method signature for updating a record in table.

7. `@Query` - For writing   any queries that are supported by SQLite. We provide SQL query as an argument to the annotation.

8. `@Dao` - It is used to annotate the interface class that defines how to access data in Room database.

9. `@Database` - It is used to annotate the databse class that extends `RoomDatabase`. It creates a database instance.

## ROOM and LiveData ##
Room automatically integrates with LiveData to help us observe changes in the database. To use this feature we set the return type of our method signature in DAO class as LiveData.

## Room Database ##
We use data class as Entity and Interface class as Dao. To create the database we create an abstract database holder class annotated with `@Database` annotation. This class extends Room Database class and it follows Singleton Design pattern, since we need only one instance of the same database for the whole app. We need to add all entities/tables as a parameter to `@Database` annotation inside this class. We have to also define all DAOs associated with the entities here, so that the database can interact with it. Various components of the database class are discussed below : 

1. We use a companion object in our database class to access the database. It allows clients to access the methods for creating or getting the database without instantiating the class.

2. We use `@Volatile` annotation for our database instance inside the companion object. We keep an instance, so that we don't repeatedly open connections to the database, as it is expensive. The `Volatile` annotation ensures the value of database instance is always up to date, and is same for all execution threads. The value of volatile variable is never cached and all reads and writes are done to and from the main memory. So, changes made by a thread are visible to all other threads immediately.

3. We create the database instance inside a synchronized block. This helps in preventing creation of multiple instances of the database, when more than one thread tries to create the database in first place. With a synchronized block, only one thread can enter the block at a time, this makes sure that database gets initialized only once.

4. We provide migration strategy when we create the database. Migration Strategy defines, how should the existing tables and data is converted, when we change the schema like changing the number or type of columns. It defines how we take all rows from old schema and convert them to rows in new schema. It helps in preventing the existing data in the app, when a user updates the app to a version that has a newer schema. 

Below is sample code of a database class

```
@Database(entities = [SleepNight::class], version = 1, exportSchema = false)
abstract class SleepDatabase : RoomDatabase() {

    abstract val sleepDatabaseDao: SleepDatabaseDao

    companion object {
        @Volatile
        private var INSTANCE: SleepDatabase? = null
        fun getInstance(context: Context): SleepDatabase {
            synchronized(this) {
                var instance = INSTANCE

                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        SleepDatabase::class.java,
                        "sleep_history_database"
                    )
                        .fallbackToDestructiveMigration()
                        .build()
                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}
```

## Multi-threading and Coroutines ##

Database operations can take a long time, therefore such operations should run on a separate thread.

An application has a main thread that runs in foreground. It can dispatch other threads that may run into background. In Android, the main thread handlers all updates to the UI. It is responsible for click handlers and lifecycle callbacks. Hence, it is also called UI thread.

The UI thread is the default, therefore all code unless specified otherwise, runs on the UI thread. However that UI thread has to run smoothly for a great user experience. So, we shoould never block the UI thread with long running operations.

One option to do work away from main thread is to use callbacks. We can start long running tasks in background thread. When the task completes the callback which was supplied as an argument is called to inform the result on the main thread. Callbacks has few drawbacks :

1. Callbacks code looks sequential but it will run at some asynchronous time in future.

2. Callbacks does not support direct exception handling. They require additional parameter like result, that determines whether the operation was successful or failure.

Coroutines are efficient way to handle long-running tasks. It helps to convert callback-based code to sequential code and support exception handling. Coroutines have following features:

1. **Asynchronous** - The coroutine runs independently from the main execution of program.

2. **Non-Blocking** - It doesn't block the main or UI thread.

3. **Sequential Code** - Callbacks are not needed, which make code sequential.

Coroutines have following three components:

1. **Job** - A background job is something that can be cancelled. We use it cancel the coroutine. So, when the fragment/viewModel that started the coroutine is destroyed, all coroutines are cancelled.

2. **Dispatcher** - It sends off coroutines to run on various threads.

3. **Scope** - It combines information, including job and dispatcher and defines the context in which coroutine runs.

4. **Supspended Functions** - The keyword suspend is Kotlin's way of marking a function, or function type, available to coroutines. When a coroutine calls a function marked suspend, instead of blocking until that function returns like a normal function call, it suspends execution until the result is ready then it resumes where it left off with the result. While it's suspended waiting for a result, it unblocks the thread that it's running on so other functions or coroutines can run.

### Quick Tips ###
1. Make sure to cancel all coroutines started by the viewModel in `onCleared()`, so that we don't end up with coroutines that have nowhere to return, when the viewModel is destroyed.

