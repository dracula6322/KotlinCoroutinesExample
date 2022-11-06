import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

//         Hot                              Cold
//   Collections (List, Set)           Sequence, Stream
//   Channel                           Flow, RxJava streams

// terminal operation: collect, launch

class Flow {
}


fun main() = runBlocking {
//    fun1()
//    fun2()
//    fun3()
    fun4(this)
}


suspend fun fun1() {
    val flow = flow {
        emit(1)
        emit(2)
        throw IndexOutOfBoundsException()
        emit(3)
    }
    try {
        flow.onEach { println("Got $it") }
            .catch {
                println("Flow catch $it")
                emit(11)
                emit(12)
            }
            .onEach {
                if (it == 12) {
                    throw RuntimeException()
                }
            }.onCompletion { println("onCompletion") }
            .collect { println("Collected $it") }
    } catch (exception: Exception) {
        println("Global catch $exception")
    }

//    Got 1
//    Collected 1
//    Got 2
//    Collected 2
//    Flow catch java.lang.IndexOutOfBoundsException
//    Collected 11
//    onCompletion
//    Global catch java.lang.RuntimeException
}

suspend fun fun2() {
    val flow = flow {
        emit("Message1")
        emit("Message2")
    }
    flow.onStart { println("Before") }
        .onEach { throw IllegalAccessError() }
        .catch { println("Caught $it") }
        .collect()

//    Before
//    Caught java.lang.IllegalAccessError
}

suspend fun fun3() {

    suspend fun present(place: String, message: String) {
        val ctx = kotlin.coroutines.coroutineContext
        val name = ctx[CoroutineName]?.name
        println("[$name] $message on $place")
    }

    val users = flow {
        present("flow builder", "Message")
        emit("Message")
    }

    withContext(CoroutineName("Name1")) {
        users
            .flowOn(CoroutineName("Name3"))
            .onEach { present("onEach", it) }
            .flowOn(CoroutineName("Name2"))
            .collect { present("collect", it) }
    }
//    [Name3] Message on flow builder
//    [Name2] Message on onEach
//    [Name1] Message on collect
}

suspend fun fun4(coroutineScope: CoroutineScope) {
    flowOf("User1", "User2")
        .onStart { println("Users:") }
        .onEach { println(it) }
        .launchIn(coroutineScope)

//    Users:
//    User1
//    User2
}