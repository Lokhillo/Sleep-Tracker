# Sleep Tracker #
This is a sleep quality tracker app, that stores the sleep data over time in a database. It stores start time, end time and quality of sleep. The app architecture is based on MVVM architecture and uses ROOM database.

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

### App Preview ###
<img alt="Sleep Tracker Preview 1" src="https://github.com/pawanharariya/Sleep-Tracker/assets/43620548/ecabf3c2-dd8f-47f2-922e-beeb0bc42465" width="220" >
<img alt="Sleep Tracker Preview 2" src="https://github.com/pawanharariya/Sleep-Tracker/assets/43620548/dd82c022-846d-4610-be9a-7f4faa64f08f" width="220" >
<img alt="Sleep Tracker Preview 3" src="https://github.com/pawanharariya/Sleep-Tracker/assets/43620548/059020c2-df13-479b-8020-d1447b0ab119" width="220" >

### App Architecture ###
<img alt="Sleep Tracker Architecture" src="https://github.com/pawanharariya/Sleep-Tracker/assets/43620548/e275bcea-52a3-4ff9-8d16-16d054a24576" width="400" >

## ROOM ##
Room is a database library that is part of Android Jetpack. It is database layer built on top of SQLite database. Below are some terms related to databases and Room.

**Entity** - Object or concept to store in database. Entity class represented by a data class defines a table and each object instance of it is stored as a row in the table. And, each property of the class defines a column of the table.
For example, in this app Sleep data of one night acts as an entity.

**Query**
Query is a request for data from a database table(s), or a request to perform actions on the data.
For example, in this app we can add or delete sleep data in our database. 

Using Room, we define each entity as a data class and all queries as interfaces. We then use annotations to add metadata to both. Room uses the annotated classes to create tables and perform queries on the database.

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

## Room and LiveData ##
Room automatically integrates with LiveData to help us observe changes in the database. To use this feature we set the return type of our method signature in DAO class as LiveData.

## Room Database ##
We use data class as Entity and Interface class as Dao. To create the database we create an abstract database holder class annotated with `@Database` annotation. This class extends Room Database class and it follows Singleton Design Pattern, since we need only one instance of the same database for the whole app. We need to add all entities/tables as a parameter to `@Database` annotation inside this class. We have to also define all DAOs associated with the entities here, so that the database can interact with it. Various components of the database class are discussed below : 

1. We use a companion object in our database class to access the database. It allows clients to access the methods for creating or getting the database without instantiating the class.

2. We use `@Volatile` annotation for our database instance inside the companion object. We keep an instance, so that we don't repeatedly open connections to the database, as it is expensive. The `Volatile` annotation ensures the value of database instance is always up to date, and is same for all execution threads. The value of volatile variable is never cached and all reads and writes are done to and from the main memory. So, changes made by a thread are visible to all other threads immediately.

3. We create the database instance inside a synchronized block. This helps in preventing creation of multiple instances of the database, when more than one thread tries to create the database in first place. With a synchronized block, only one thread can enter the block at a time, this makes sure that database gets initialized only once.

4. We provide migration strategy when we create the database. Migration Strategy defines, how should the existing tables and data is converted, when we change the schema like changing the number or type of columns. It defines how we take all rows from old schema and convert them to rows in new schema. It helps in preserving the existing data in the app, when a user updates the app to a version that has a newer schema. 

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

An application has a main thread that runs in foreground. It can dispatch other threads that may run into background. In Android, the main thread handles all updates to the UI. It is responsible for click handlers and lifecycle callbacks. Hence, it is also called UI thread.

The UI thread is the default, therefore all code unless specified otherwise, runs on the UI thread. However that UI thread has to run smoothly for a great user experience. So, we should never block the UI thread with long running operations. Database operations can take a long time, therefore such operations should run on a separate thread. If we block the main thread for too long, the app may even crash and present an **Application Not Responding** dialog.

One option to do work away from main thread is to use callbacks. We can start long running tasks in background thread. When the task completes the callback which was supplied as an argument is called to inform the result on the main thread. Callbacks has few drawbacks :

1. Callbacks code looks sequential but it will run at some asynchronous time in future.

2. Callbacks does not support direct exception handling. They require additional parameter like result, that determines whether the operation was successful or failure.

Coroutines are efficient way to handle long-running tasks. It helps to convert callback-based code to sequential code and support direct exception handling. Coroutines have following features:

1. **Asynchronous** - The coroutine runs independently from the main execution of program.

2. **Non-Blocking** - It doesn't block the main or UI thread.

3. **Sequential Code** - Callbacks are not needed, which make code sequential.

Coroutines have following four components:

1. **Job** - A background job is something that can be cancelled. We use it cancel the coroutine. So, when the fragment/viewModel that started the coroutine is destroyed, all coroutines are cancelled.

2. **Dispatcher** - It sends off coroutines to run on various threads.

3. **Scope** - It combines information, including job and dispatcher and defines the context in which coroutine runs.

4. **Supspended Functions** - The suspend keyword marks a function to be available to coroutines. When a coroutine calls a function marked suspend, instead of blocking until that function returns like a normal function call, it suspends execution until the result is ready then it resumes where it left off with the result. While it's suspended waiting for a result, it unblocks the thread that it's running on so other functions or coroutines can run.

### Coroutines with Room ###
We latest library, we can directly call suspended DAO methods from our viewModel scope. With the use of suspended functions, our coroutines become main-safe, i.e. we can directly cann them from our main thread.
```
// use suspend keyword for Dao methods
@Insert
suspend fun insert(night: SleepNight)
```
```
fun onStartTracking() {
        // use view model scope to launch the coroutine from main thread
        viewModelScope.launch {
            val newNight = SleepNight()
            insert(newNight)
            tonight.value = getTonightFromDatabase()
        }
}
private suspend fun insert(night: SleepNight) {
        database.insert(night)
}
```


## RecyclerView ##
It is used to display data in form of list. It uses adpater pattern and does processing only for items visible on the screen, until user scrolls. When the user scrolls, it reuses existing scrolled off views, at new positions with new data. Following are features of RecyclerView.

1. Efficient : It is designed to be efficient for displaying extremely large lists.

2. Display Complex Views : It can handle complex collection of views easily as a item of list.

3. Customizable : It can display different views in the same list. It can support list or grid layout and horizontal or vertical scrolling.

4. Recycling : When the items are scrolled off the visible screen. It uses them to populate with new data. And, when an item changes, instead of re-drawing complete list, it just updates the changed items.

## Adapter ##
The adapter takes the data (from list, room database, etc) and converts them, so that it can be handled by RecyclerView. It is based on Adapter Design Pattern, which converts one interface to work with another. An adapter for RecyclerView should have following methods:

1. `getItemCount()` - The recycler view should know how many items are available, to decide how far to scroll, or deciding the size of scrollbar.

2. `onBindViewHolder()` - It tells RecyclerView how to add the data to the views.

3. `onCreateViewHolder()` - It tells RecyclerView how to create a new viewHolder, when required.

RecyclerView doesn't directly interact with views but ViewHolders, provided by the adapter. ViewHolders just hold the views of the item. RecyclerView reuses the ViewHolders that are scrolled off the screen create items with new data. It is used by recyclerView to draw, animate and scroll the list.

### DiffUtil ###
`notifyDataSetChanged()` tells the RecyclerView that entire list needs to be re-drawn. Hence, RecyclerView re-draws everything, which can be expensive, in-cases when only a single list item is changed.

DiffUtil is helper class for RecyclerView adapters that calculates changes in the list and minimizes modifications. This helps RecyclerView to re-draw only the items inserted, deleted or updated, instead of entire list. It also provides default animations.

### Helpful Tips ###
1. Since we know, recyclerView reuses the viewHolders, we must reset the state of the views. For example, suppose we set the text color based on some condition, when this viewHolder is reused it will have the same text color, so we must reset it, so that next time the viewHolder's state is correct, if item at that position don't match the condition.
    ```
    if (someCondition) {
           holder.textView.setTextColor(Color.RED) 
    } else {
           holder.textView.setTextColor(Color.BLACK) 
    }
    ```

2. Instead of using `notifyDataSetChanged()` when single list item is changed, we can use other APIs like `notifyOnItemInserted()` or use the `DiffUtil` helper class.

3. Instead of using `RecylerView.Adapter`, we can use `ListAdapter` for cases when our RecyclerView is backed by a list. It keeps track of list, notifies adapter when list is updated, so works well with DiffUtil.

4. We can use DataBinding for items in our RecyclerView using BindingAdapters and extension functions.
    ```
    @BindingAdapter("sleepDuration")
    fun TextView.setSleepDuration(item: SleepNight?) {
        item?.let {
            text = item.sleepDuration
        }
    }
    ```
    ```
    \\ and use it as an attribute in xml
    <TextView
        android:id="@+id/sleep_duration"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        app:sleepDurationFormatted="@{sleep}" />
    ```
4. In case, we want different layouts for items in one RecylcerView, we create different ViewHolders, and to tell RecyclerView which ViewHolder for which position, we override the `getItemViewType(position: Int)` method.

5. To use grids instead of a linear list. We can use GridLayoutManager instead of LinearLayoutManager. Also, we can control the Span using span size lookup configuration object.
    ```
    manager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int) =  when (position) {
                0 -> 3     // first item spans three columns
                else -> 1  // all other items span one column
            }
    }
    ```