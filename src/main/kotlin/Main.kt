@file:Suppress("unused", "OPT_IN_USAGE")

import kotlinx.coroutines.*
import kotlinx.coroutines.NonCancellable.invokeOnCompletion

// Тут показывается что CEH обрабатывается только в launch, в async он бесполезен
// https://kotlinlang.org/docs/exception-handling.html#coroutineexceptionhandler
val handler = CoroutineExceptionHandler { _, exception ->
    println("CoroutineExceptionHandler $exception " + exception.suppressed.contentToString())
}

//fun main() = fun1()
fun fun1() = runBlocking {
    // Простой пример показывающий что в async эксепшон бросается в await если корутина рутовая
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

//fun main() = fun2()
fun fun2() = runBlocking {
    // Тут показывается что CEH обрабатывается только в launch, в async он бесполезен
    // https://kotlinlang.org/docs/exception-handling.html#coroutineexceptionhandler
    val handler = CoroutineExceptionHandler { _, exception ->
        println("CoroutineExceptionHandler got $exception")
    }
    val job = GlobalScope.launch(handler) {
        throw AssertionError()
    }
    println("After launch")
    val deferred = GlobalScope.async(handler) {
        throw ArithmeticException()
    }
    println("After defered")
    joinAll(job, deferred)
    println("After joinAll")
    deferred.join()
    println("After deferred join")
    deferred.await()
    println("After await")
}

//fun main() = fun32()
fun fun32() = runBlocking {
    Thread.currentThread().uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { p0, p1 ->
        println("!!!!!!!! Crash !!!!!!!!")
        println(p1)
    }

    supervisorScope {
//        coroutineScope {
//                val launch = launch(handler) {
        val launch = launch() {
//        val launch = async {
            //        val launch = async(handler) {
            throw RuntimeException()
        }

        println("Before inner join")
        //        joinAll(launch)
        println("Before await join")
//        awaitAll(launch)
        println("After inner join")
    }
    delay(300)
    println("After join")
}


// fun main() = fun3()
fun fun3() = runBlocking {
    // Пример показывающий как отменять корутины
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

//fun main() = fun4()
fun fun4() = runBlocking {
    // Как сделать код который отработает когда отменят корутину
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

//fun main() = fun5()
fun fun5() = runBlocking {
    // Показывается что собираются все exception которые бросались после того как отменилась корутина
    val job = GlobalScope.launch(handler) {
        launch {
            try {
                delay(Long.MAX_VALUE)
            } finally {
                throw ArithmeticException()
            }
        }
        launch {
            try {
                delay(Long.MAX_VALUE)
            } finally {
                throw IndexOutOfBoundsException()
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

//fun main() = fun6()
fun fun6() = runBlocking {
    // Показывает что корутины отменяются рекурсивно и что в catch приходит не тот exception который будет с CEH
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

//fun main() = fun7()
fun fun7() = runBlocking {
    // Показывается что SupervisorJob не отменяет корутину если ее child бросил exception
    // И что все задачи отменяются если отменить job которая передавалась при создании scope
    val supervisor = SupervisorJob()
    with(CoroutineScope(coroutineContext + supervisor)) {
        // launch the first child -- its exception is ignored for this example (don't do this in practice!)
        val firstChild = launch(handler) {
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

//fun main() = fun8()
fun fun8() = runBlocking {
    // Тут показывается что supervisorScope брасает exception если он есть в теле scope, а не в корутине
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

//fun main() = fun9()
fun fun9() = runBlocking {
    // Показано что в SupervisorScope не зависит от exception в child
    supervisorScope {
        val child = launch(handler) {
            println("The child throws an exception")
            throw AssertionError()
        }
        println("The scope is completing")
    }
    println("The scope is completed")
}

// fun main() = fun10()
fun fun10() = runBlocking {
    // Показано что CEH в async безсполезен
    // Что async в supervisorScope не сообщаем ему про exception у себя
    supervisorScope {
        val r1: Deferred<String> = async(CoroutineName("_2") + handler) { error("Unexpected error") }
        val r2: Deferred<String> = async(CoroutineName("_3")) { delay(1000); "success" }
        try {
            r1.await() + r2.await()
        } catch (t: Throwable) {
            println("Catch $t " + t.suppressed.contentToString())
            "Exit"
        }
    }
    println("End")
    Unit
}

//fun main() = fun11()
fun fun11() = runBlocking {
    // Тут показано что coroutineScope бросает исключение которое было в child, но при этом завершает весь код который в scope
    coroutineScope {
        val r1: Deferred<String> = async(CoroutineName("_2") + handler) { error("Unexpected error") }
        val r2: Deferred<String> = async(CoroutineName("_3")) { delay(1000); "success" }
        try {
            r1.await() + r2.await()
        } catch (t: Throwable) {
            println("Catch $t " + t.suppressed.contentToString())
            "Exit"
        }
    }
    println("End")
    Unit
}


//fun main() = fun12()
fun fun12() = runBlocking {
    val scope = CoroutineScope(Job())
    val job = scope.launch(SupervisorJob()) {
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
    joinAll(job)
    println("We are here")
    Unit
}

//fun main() = fun13()
fun fun13() = runBlocking {
    //  Показывается что CEH в async бесполезен
    try {
        supervisorScope {
            val handler = CoroutineExceptionHandler { _, exception ->
                println("CoroutineExceptionHandler got $exception")
            }
            val job = launch(handler) {
                throw AssertionError()
            }
            println("After launch")
            val deferred = async(handler) {
                throw ArithmeticException()
            }
            println("After defered")
            joinAll(job, deferred)
            println("After joinAll")
            deferred.join()
            println("After deferred join")
            deferred.await()
            println("After await")
        }
    } catch (throwable: Throwable) {
        println("Catch throwable $throwable " + throwable.suppressed.contentToString())
    }
}

//fun main() = fun14()
fun fun14() = runBlocking {
    // Если переопределить SupervisorJob(), то скопу не получится убить из его корутин
    val scope = CoroutineScope(Job())
    val handler = CoroutineExceptionHandler { _, exception ->
        println("CoroutineExceptionHandler got $exception")
    }
    val job = scope.launch(SupervisorJob()) {
        // new coroutine -> can suspend
        val launch1 = launch(handler) {
            println("1")
            throw java.lang.RuntimeException()
        }
        val launch = launch {
            yield()
            println("2")
        }
        delay(100)
        println("Hi")
        joinAll(launch, launch1)
    }
    joinAll(job)
    scope.launch {
        println("scope is not over")
    }.join()
    Unit
}


//fun main() = fun17()
fun fun17() = runBlocking {
    // Тут показано что в coroutineScope exception одного из child отменяет все остальные корутины
    val handler = CoroutineExceptionHandler { _, exception ->
        println("CoroutineExceptionHandler got $exception")
    }
    coroutineScope {
        val r1: Deferred<String> = async(CoroutineName("_2") + handler) { error("Unexpected error") }
        val r2: Deferred<String> = async(CoroutineName("_3")) { delay(1000); "success" }
        try {
            r2.await() + r1.await()
        } catch (t: Throwable) {
            println("Catch $t") // Catch kotlinx.coroutines.JobCancellationException: Parent job is Cancelling; job=ScopeCoroutine{Cancelling}@42f30e0a
            "Exit"
        }
    }
    Unit
}

//fun main() = fun18()
fun fun18() = runBlocking {
    // Показывает что если в scope было несколько exception, то они сольются в один.
    // Приоритет будет у того кто первый отменил скоуп
    val handler = CoroutineExceptionHandler { _, exception ->
        println("CoroutineExceptionHandler got $exception")
    }
    coroutineScope {
        val r1: Deferred<String> = async(CoroutineName("_2") + handler) { error("Unexpected error") }
        val r2: Deferred<String> = async(CoroutineName("_3")) { delay(1000); "success" }
        try {
            r1.await() + r2.await()
        } catch (t: Throwable) {
            println("Catch $t " + t.suppressed.contentToString())
            "Exit"
        }
        throw java.lang.RuntimeException() // Exception in thread "main" java.lang.IllegalStateException: Unexpected error
    }
    launch {
        println("scope is not over")
    }.join()
    Unit
}

//fun main() = fun19()
fun fun19() = runBlocking {
    // Пример как остановить проброску exception через SupervisorJob()
    val scope = CoroutineScope(Job())
    try {
        val longDelayLaunch = scope.launch {
            delay(5000)
        }
        longDelayLaunch.invokeOnCompletion {
            println("invokeOnCompletion $it " + it?.suppressed.contentToString()) // invokeOnCompletion null null
        }

        val launch = scope.launch(SupervisorJob()) {
            launch {
                throw Exception("Failed coroutine 1")
            }
            val longDelayLaunch1 = launch {
                delay(5000)
            }
            longDelayLaunch1.invokeOnCompletion {
                // invokeOnCompletion in launch kotlinx.coroutines.JobCancellationException: Parent job is Cancelling; job=StandaloneCoroutine{Cancelling}@1b2e280a []
                println("invokeOnCompletion in launch $it " + it?.suppressed.contentToString())
            }
            println("Before yield")
            yield()
            println("Not show")
        }
        println("Before join")
        joinAll(launch, longDelayLaunch)
    } catch (exception: Throwable) {
        // Not show
    }
    scope.launch {
        println("scope is not over")
    }.join()
    Unit
}

//fun main() = fun20()
fun fun20() = runBlocking {
    // Пример как остановить проброску exception через SupervisorJob()
    val scope = CoroutineScope(Job())
    try {
        val longDelayLaunch = scope.launch {
            delay(5000)
        }
        longDelayLaunch.invokeOnCompletion {
            println("invokeOnCompletion $it " + it?.suppressed.contentToString()) // invokeOnCompletion null null
        }

        val launch = scope.launch(SupervisorJob()) {
            launch {
                throw Exception("Failed coroutine 1")
            }
            val longDelayLaunch1 = launch {
                delay(5000)
            }
            longDelayLaunch1.invokeOnCompletion {
                // invokeOnCompletion in launch kotlinx.coroutines.JobCancellationException: Parent job is Cancelling; job=StandaloneCoroutine{Cancelling}@1b2e280a []
                println("invokeOnCompletion in launch $it " + it?.suppressed.contentToString())
            }
            println("Before yield")
            yield()
            println("Not show")
        }
        println("Before join")
        joinAll(launch, longDelayLaunch)
    } catch (exception: Throwable) {
        // Not show
    }
    scope.launch {
        println("scope is not over")
    }.join()
    Unit
}

//fun main() = fun21()
fun fun21(): Unit = runBlocking {
    // Еще один пример из книги в котором говорится про SupervisorJob в launch

    // Don't do that, SupervisorJob with one children
    // and no parent works similar to just Job
    launch(SupervisorJob()) { // 1
        launch {
            delay(1000)
            throw Error("Some error")
        }
        launch {
            delay(2000)
            println("Will not be printed")
        }
    }
    launch {
        delay(3000)
        println("We are here")
    }
    delay(3000)
}

//fun main() = fun22()
fun fun22(): Unit = runBlocking {
    // Пример как будет проходить отмена корутин если использовать отдельный SupervisorJob()

//    val launch = launch(SupervisorJob()) {
    val launch = launch() {
        launch {
            delay(1000)
            println("First launch")
        }.invokeOnCompletion {
            println("First launch end with $it")
        }
        launch {
            delay(2000)
            println("Second launch")
        }.invokeOnCompletion {
            println("Second launch end with $it")
        }
    }
    launch {
        delay(3000)
        println("Main launch")
    }.invokeOnCompletion {
        println("Main launch end with $it")
    }
    cancel()
}


//fun main() = fun23()
fun fun23() = runBlocking {
    /**
    CEH вызывается когда рутовая корутина скоупа умерла
     */
    val handler = CoroutineExceptionHandler { _, exception ->
        println("CoroutineExceptionHandler $exception " + exception.suppressed.contentToString())
    }
    val scope = CoroutineScope(Job())
//    val scope = CoroutineScope(SupervisorJob())
//    val scope = CoroutineScope(Job() + handler)
//    val scope = CoroutineScope(SupervisorJob() + handler)
    try {
        val longDelayLaunch = scope.launch {
            delay(5000)
        }
        longDelayLaunch.invokeOnCompletion {
            println("invokeOnCompletion $it " + it?.suppressed.contentToString())
        }

        val launch = scope.launch(handler) {
//        val launch = scope.launch(SupervisorJob()) {
//        val launch = scope.launch(handler) {
            val async = launch() {
                throw Exception("Failed coroutine 1")
            }
            val longDelayLaunch1 = launch {
                delay(5000)
            }
            longDelayLaunch1.invokeOnCompletion {
                println("invokeOnCompletion1 in launch $it " + it?.suppressed.contentToString())
            }
            println("Before yield")
            yield()
            println("After yield")
        }
        println("Before join")
        joinAll(launch, longDelayLaunch)
    } catch (exception: Throwable) {
        println("Catch $exception " + exception.suppressed.contentToString())
    }
    scope.launch {
        println("scope is not over")
    }.join()
    println("We are here")
    Unit
}

fun println(value: Any?) {
    kotlin.io.println(Thread.currentThread().toString() + " " + value)
}

//fun main() = runBlocking {
//
//    supervisorScope {
//
//        val launch = launch {
//            try {
//                supervisorScope {
//                    delay(1000)
//                }
//            } catch (e: Exception) {
//                println("adasd" + e)
//            }
//
//        }
//        delay(500)
//        launch.cancel()
//
//    }
//
//    try {
//        coroutineScope {
//            val async = async {
//                throw RuntimeException()
//            }
//            delay(10000)
//        }
//    } catch (e: Exception) {
//        println(e)
//    }
//    delay(400)
//    println("We ")
//}


cdfun main(): Unit = runBlocking {

    Thread.currentThread().uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { p0, p1 ->
        println("!!!!!!!! Crash !!!!!!!!")
        println(p1)
    }

//    val context = Job()
//    val context = SupervisorJob()
//    val coroutineScope = coroutineScope {
    val coroutineScope = supervisorScope {
        val launch = launch {
            launch {
//                launch() {
//                launch(handler) {
                val job = Job()
                launch(handler + job) {
//                supervisorScope {
//                launch(Job()) {
//                launch(context ) {
//                    launch(coroutineExceptionHandler) {
                        async { throw RuntimeException() }
                        launch { delay(10000) }.invokeOnCompletion { println(it) }
//                    }
                }.join()
            }
        }
        invokeOnCompletion { println("asdas $it") }
        launch
    }

    coroutineScope.join()
//    context.join()
    println("we are")
}