@file:Suppress("unused", "OPT_IN_USAGE")

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import java.io.IOException

fun main() {
//    fun1()
//    fun2()
//    fun3()
//    fun4()
//    fun5()
//    fun6()
//    fun7()
//    fun8()
//    fun9()
    fun10()
//    fun11()
}


// https://pl.kotl.in/unOSf06ol
fun fun1() = runBlocking {
    val job = GlobalScope.launch {
        println("Throwing exception from launch")
        throw IndexOutOfBoundsException()
    }
    job.join()
    println("Joined failed job")
    val deferred = GlobalScope.async {
        println("Throwing exception from async")
        throw ArithmeticException()
    }
    try {
        deferred.await()
        println("Done")
    } catch (e: ArithmeticException) {
        println("Caught ArithmeticException")
    }
}

// https://pl.kotl.in/OheqZHmnm
fun fun2() = runBlocking {
    val handler = CoroutineExceptionHandler { _, exception ->
        println("CoroutineExceptionHandler got $exception")
    }
    val job = GlobalScope.launch(handler) {
        throw AssertionError()
    }
    val deferred = GlobalScope.async(handler) {
        throw ArithmeticException()
    }
    joinAll(job, deferred)
}

// https://pl.kotl.in/bUBPGTg9q
fun fun3() = runBlocking {
    val job = launch {
        val child = launch {
            try {
                delay(Long.MAX_VALUE)
            } finally {
                println("Child is cancelled")
            }
        }
        yield()
        println("Cancelling child")
        child.cancel()
        child.join()
        yield()
        println("Parent is not cancelled")
    }
    job.join()
}

// https://pl.kotl.in/vDWtNLwlE
fun fun4() = runBlocking {
    val handler = CoroutineExceptionHandler { _, exception ->
        println("CoroutineExceptionHandler got $exception")
    }
    val job = GlobalScope.launch(handler) {
        launch {
            try {
                delay(Long.MAX_VALUE)
            } finally {
                withContext(NonCancellable) {
                    println("Children are cancelled, but exception is not handled until all children terminate")
                    delay(100)
                    println("The first child finished its non cancellable block")
                }
            }
        }
        launch {
            delay(10)
            println("Second child throws an exception")
            throw ArithmeticException()
        }
    }
    job.join()
}

// https://pl.kotl.in/5LRdsJ32J
fun fun5() = runBlocking {
    val handler = CoroutineExceptionHandler { _, exception ->
        println("CoroutineExceptionHandler got $exception with suppressed ${exception.suppressed.contentToString()}")
    }
    val job = GlobalScope.launch(handler) {
        launch {
            try {
                delay(Long.MAX_VALUE)
            } finally {
                throw ArithmeticException()
            }
        }
        launch {
            delay(100)
            throw IllegalAccessError()
        }
        delay(Long.MAX_VALUE)
    }
    job.join()
}

// https://pl.kotl.in/hOz4e53Dt
fun fun6() = runBlocking {
    val handler = CoroutineExceptionHandler { _, exception ->
        println("CoroutineExceptionHandler got $exception")
    }
    val job = GlobalScope.launch(handler) {
        val inner = launch { // all this stack of coroutines will get cancelled
            launch {
                launch {
                    throw ArithmeticException() // the original exception
                }
            }
        }
        try {
            inner.join()
        } catch (e: CancellationException) { // JobCancellationException
            println("Rethrowing CancellationException with original cause")
            throw e // cancellation exception is rethrown, yet the original ArithmeticException gets to the handler
        }
    }
    job.join()
}

// https://pl.kotl.in/EB6nXLIsC
fun fun7() = runBlocking {
    val supervisor = SupervisorJob()
    with(CoroutineScope(coroutineContext + supervisor)) {
        // launch the first child -- its exception is ignored for this example (don't do this in practice!)
        val firstChild = launch(CoroutineExceptionHandler { _, throwable ->
            println(throwable)
        }) {
            println("The first child is failing")
            throw AssertionError("The first child is cancelled")
        }
        // launch the second child
        val secondChild = launch {
            firstChild.join()
            // Cancellation of the first child is not propagated to the second child
            println("The first child is cancelled: ${firstChild.isCancelled}, but the second one is still active")
            try {
                delay(Long.MAX_VALUE)
            } finally {
                // But cancellation of the supervisor is propagated
                println("The second child is cancelled because the supervisor was cancelled")
            }
        }
        // wait until the first child fails & completes
        firstChild.join()
        println("Cancelling the supervisor")
        supervisor.cancel()
        secondChild.join()
    }
}

// https://pl.kotl.in/Tl3yMe6fC
fun fun8() = runBlocking {
    try {
        supervisorScope {
            launch {
                try {
                    println("The child is sleeping")
                    delay(Long.MAX_VALUE)
                } finally {
                    println("The child is cancelled")
                }
            }
            // Give our child a chance to execute and print using yield
            yield()
            println("Throwing an exception from the scope")
            throw AssertionError()
        }
    } catch (e: AssertionError) {
        println("Caught an assertion error")
    }
}

// https://pl.kotl.in/r6gqrzia_
fun fun9() = runBlocking {
    val handler = CoroutineExceptionHandler { _, exception ->
        println("CoroutineExceptionHandler got $exception")
    }
    supervisorScope {
        val child = launch(handler) {
            println("The child throws an exception")
            throw AssertionError()
        }
        println("The scope is completing")
    }
    println("The scope is completed")
}

// https://pl.kotl.in/6VY7SfotN
fun fun10() = runBlocking {
    val handler = CoroutineExceptionHandler { _, exception ->
        println("CoroutineExceptionHandler got $exception")
    }
    supervisorScope {
        val r1: Deferred<String> = async(CoroutineName("_2") + handler) { error("Unexpected error") }
        val r2: Deferred<String> = async(CoroutineName("_3")) { delay(1000); "success" }
        try {
            r1.await() + r2.await()
        } catch (t: Throwable) {
            println("Catch $t")
            "Exit"
        }
    }
}

// https://pl.kotl.in/OPVgiLXyN
fun fun11() = runBlocking {
    val handler = CoroutineExceptionHandler { _, exception ->
        println("CoroutineExceptionHandler got $exception")
    }
    coroutineScope {
        val r1: Deferred<String> = async(CoroutineName("_2") + handler) { error("Unexpected error") }
        val r2: Deferred<String> = async(CoroutineName("_3")) { delay(1000); "success" }
        try {
            r1.await() + r2.await()
        } catch (t: Throwable) {
            println("Catch $t")
            "Exit"
        }
    }
}

// https://pl.kotl.in/CIkmGKBDV
fun fun12() = runBlocking {
    val scope = CoroutineScope(Job())
    scope.launch(SupervisorJob()) {
        // new coroutine -> can suspend
        val launch1 = launch {
            println("1")
            throw java.lang.RuntimeException()
        }
        val launch = launch {
            println("2")
        }
        delay(100)
        println("Hi")
        joinAll(launch, launch1)
    }
}

fun println(value: Any?) {
    kotlin.io.println(Thread.currentThread().toString() + " " + value)
}