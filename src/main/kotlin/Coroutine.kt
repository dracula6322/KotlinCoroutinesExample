import kotlinx.coroutines.*
import rx.Observable
import rx.Subscriber
import rx.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

//fun main() = Main().fun1()
//fun main() = Main().fun2()
//fun main() = Main().fun3()
//fun main() = Main().fun4()
//fun main() = Main().fun5()
//fun main() = Main().fun6()
//fun main() = Main().fun7()
//fun main() = Main().fun8()
//fun main() = Main().fun9()
//fun main() = Main().fun10()
//fun main() = Main().fun11()
//fun main() = Main().fun12()
//fun main() = Main().fun13()
//fun main() = Main().fun14()
//fun main() = Main().fun15()
//fun main() = Main().fun16()
fun main() = Main().fun17()
//fun main() = Main().fun18()
//fun main() = Main().fun19()
//fun main() = Main().fun20()
//fun main() = Main().fun21()
//fun main() = Main().fun22()
//fun main() = Main().fun24()
//fun main() = Main().fun25()

private class Main {

    // CEH обрабатывается только в launch, в async он бесполезен
    // https://kotlinlang.org/docs/exception-handling.html#coroutineexceptionhandler
    private val handler = CoroutineExceptionHandler { _, exception ->
        println("CoroutineExceptionHandler $exception " + exception.suppressed.contentToString())
    }

    private fun println(value: Any?) = kotlin.io.println(Thread.currentThread().toString() + " " + value)

    init {
        Thread.currentThread().uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { _, exception ->
            println("!!!!!!!! Android Crash !!!!!!!!")
            println(exception.toString() + " " + exception.suppressed.contentToString())
        }
    }

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

    fun fun2() = runBlocking {
        val job = GlobalScope.launch(handler) { throw AssertionError() }
        println("After launch")
        val deferred = GlobalScope.async(handler) { throw ArithmeticException() }
        println("After defered")
        joinAll(job, deferred)
        println("After joinAll")
        deferred.join()
        println("After deferred join")
        deferred.await()
        println("After await")
    }

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
            println("Join child")
            child.join()
            println("Before yield after cancel")
            yield()
            println("Parent is not cancelled")
        }
        job.join()
    }

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

    fun fun6() = runBlocking {
        // Показывает что корутины отменяются рекурсивно и что в catch приходит не тот exception который будет с CEH
        val job = GlobalScope.launch(handler) {
            val inner = launch {
                launch {
                    launch {
                        throw ArithmeticException()
                    }
                }
            }
            try {
                inner.join()
            } catch (exception: CancellationException) { // JobCancellationException
                println(exception.toString() + " " + exception.suppressed.contentToString())
                throw exception
            }
        }
        job.join()
    }

    fun fun7() = runBlocking {
        // Показывается что SupervisorJob не отменяет корутину если ее child бросил exception
        // И что все задачи отменяются если отменить job которая передавалась при создании scope
        val supervisor = SupervisorJob()
        with(CoroutineScope(supervisor)) {
            val firstChild = launch(handler) {
                println("The first child is failing")
                throw AssertionError("The first child is cancelled")
            }
            firstChild.join()
            val secondChild = launch {
                firstChild.join()
                println("The first child is cancelled: ${firstChild.isCancelled}, but the second one is still active")
                try {
                    delay(Long.MAX_VALUE)
                } finally {
                    // But cancellation of the supervisor is propagated
                    println("The second child is cancelled because the supervisor was cancelled")
                }
            }
            yield()
            println("Cancelling the supervisor")
            supervisor.cancelAndJoin()
            secondChild.join()
        }
    }

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

    fun fun9() = runBlocking {
        // Показано что в SupervisorScope не зависит от exception в child
        supervisorScope {
            launch(handler) { // Этот handle будет использоваться
                println("The child throws an exception")
                throw AssertionError()
            }
            println("The scope is completing")
        }
        println("The scope is completed")
    }

    fun fun10() = runBlocking {
        supervisorScope {
            val r1: Deferred<String> =
                async(handler) { error("Unexpected error") } // Этот handle не будет использоваться
            val r2: Deferred<String> = async { delay(1000); "success" }
            try {
                r1.await() + r2.await()
            } catch (t: Throwable) {
                println(t)
            }
        }
        launch { println("Will printed") }.join()
        println("End runBlocking")
    }

    fun fun11() = runBlocking {
        // Тут показано что coroutineScope отменяет scope и бросает исключение если до него дошел exception
        coroutineScope {
            val r1: Deferred<String> =
                async(handler) { error("Unexpected error") } // // Этот handle не будет использоваться
            val r2: Deferred<String> = async { delay(1000); "success" }
            try {
                r1.await() + r2.await()
            } catch (exception: Exception) {
                println(exception)
            }
        }
        launch { println("Will not printed") }.join()
        println("End runBlocking") // Will not printed
    }

    fun fun12() = runBlocking {
        //  Показывается что CEH в async бесполезен
        try {
            supervisorScope {
                val job = launch(handler) {
                    throw AssertionError()
                }
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
        launch { println("Will be printed") }.join()
        println("End runBlocking")
    }

    fun fun13() = runBlocking {
        // Если переопределить SupervisorJob(), то скопу не получится убить из его корутин
        val scope = CoroutineScope(Job())
        val job = scope.launch(SupervisorJob()) { // Если не будет передана Job, то скоуп умрет
            val first = launch(handler) { // Этот handle не будет использоваться, нужно передать выше
                println("1")
                throw java.lang.RuntimeException()
            }
            val second = launch {
                delay(50)
                println("2")
            }
            second.invokeOnCompletion { println("Second $it") }
            delay(100)
            println("Hi")
            joinAll(second, first)
        }
        joinAll(job)
        scope.launch { println("Will be printed") }.join()
        println("End runBlocking")
    }

    fun fun14() = runBlocking {
        // Показывает что если в scope было несколько exception, то они сольются в один.
        // Приоритет будет у того кто первый отменил скоуп
        coroutineScope {
            val r1: Deferred<String> = async(handler) { error("Unexpected error") }
            val r2: Deferred<String> = async { delay(1000); "success" }
            try {
                r1.await() + r2.await()
            } catch (exception: Exception) {
                println("Catch $exception")
            }
            throw java.lang.RuntimeException()
        }
        println("End runBlocking")

    }

    fun fun15() = runBlocking {
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
                launch { throw Exception("Failed coroutine 1") }
                val longDelayLaunch1 = launch { delay(5000) }
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
        println("End runBlocking")
    }

    fun fun16(): Unit = runBlocking {
        // Еще один пример из книги в котором говорится про SupervisorJob в launch, стр. 119
        // Для того что бы First и Second не отменяли друг друга можно им передать SupervisorJob
        val supervisorJob = SupervisorJob() // Job c parent null
        launch(supervisorJob) { // Job c parent supervisorJob
            launch(CoroutineName("First")) { // Job c parent StandaloneCoroutine
                delay(1000)
                throw Error("Some error")
            }
            launch(CoroutineName("Second")) { // Job c parent StandaloneCoroutine
                delay(2000)
                println("Will not be printed")
            }
        }
        launch {
            delay(3000)
            println("Will be printed")
        }
        delay(5000)
        println("End runBlocking")
    }

    fun fun17(): Unit = runBlocking {
        // supervisorScope может бросить exception если его отменить и в каком порядке происходит отмена
        try {
            supervisorScope {
                launch {
                    launch {
                        delay(1000)
                        println("First launch")
                    }.invokeOnCompletion { println("First launch end with $it") }
                    launch {
                        delay(2000)
                        println("Second launch")
                    }.invokeOnCompletion { println("Second launch end with $it") }
                }
                launch(CoroutineName("Main launch")) {
                    delay(3000)
                    println("Main launch")
                }.invokeOnCompletion { println("Main launch end after First and Second  with $it") }
                yield()
                cancel()
            }
        } catch (exception: Exception) { // JobCancellationException
            println("Exception from scope $exception")
        }
        println("End runBlocking")
    }

    fun fun18() = runBlocking {
        val scope = CoroutineScope(Job())
        try {
            val first = scope.launch { delay(5000) }
            first.invokeOnCompletion {
                println("invokeOnCompletion first $it " + it?.suppressed.contentToString())
            }

            val second = scope.launch(handler) {
                launch { throw Exception("Failed coroutine") }
                launch { delay(5000) }
                    .invokeOnCompletion {
                        println("invokeOnCompletion long launch  $it " + it?.suppressed.contentToString())
                    }
                println("Before yield")
                yield()
                println("Not will be printed")
            }
            println("Before joinAll")
            joinAll(second, first)
        } catch (exception: Throwable) {
            // Not show
        }
        launch { println("Will be printed") }.join()
        println("End runBlocking")
    }

    fun fun19() = runBlocking {
        coroutineScope {
            // Если убрать Job(), то runBlocking бросит сразу exception
            val launch = launch(Job()) { // Если убрать Job(), то runBlocking бросит сразу exception
                launch(handler) { // Этот handle не будет использоваться, нужно передать выше
                    throw RuntimeException()
                }
            }
            joinAll(launch)
        }
        launch { println("Will be printed") }.join()
        println("End runBlocking")
    }

    fun fun20() = runBlocking {
        val job = SupervisorJob()
        launch(job) {
            delay(1000)
            throw Error("Some error")
        }
        launch(job) {
            delay(2000)
            println("Second coroutine will be printed")
        }
        job.join() // Зависнет пока кто-то не отменит job
        launch { println("(Not show) Will be printed") }.join()
        println("(Not show) End runBlocking")
    }

    fun fun21() = runBlocking {
        // В какой очереди выполняются корутины
        // При дефолтном значении CoroutineStart.DEFAULT корутина не будет выполнятся пока не дойдет до первой suspend фукнкции
        // При использовании CoroutineStart.UNDISPATCHED корутина будет выполнятся сразу
        // https://stackoverflow.com/a/59152712
        launch { println("1") }
        launch(start = CoroutineStart.DEFAULT) { println("2") }
        launch(start = CoroutineStart.UNDISPATCHED) { println("3") }
        launch { println("4") }.join()
        async { println("5") }
        coroutineScope {
            launch { println("6") }
            println("7")
        }
        launch { println("8") }
        println("9")

        // 3, 1, 2, 4, 7, 5, 6, 9, 8
    }

    @OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
    fun fun22() {
        // Показывает в чем разница между runBlocking и coroutineScope в плане блокирования потока выполнения
        // Что бы был результат нужно раскоментить нужный участок кода
        // https://stackoverflow.com/a/53536713
        val limitedParallelism = Dispatchers.IO.limitedParallelism(1)
        runBlocking(limitedParallelism) {
            val measureTime = measureTime {

                val launch = launch {
                    for (i in 100..110) {
                        println(i)
                        delay(500)
                    }
                }

//                runBlocking { // 6.52s - Если будет один runBlocking
//                    delay(1000)
//                    println("runBlocking")
//                }

//                coroutineScope { // 5.52s - Если будет один coroutineScope
//                    delay(1000)
//                    println("coroutineScope")
//                }

                launch.join()
                launch { println("Will be printed") }.join()
                println("End runBlocking")
            }
            println(measureTime)
        }
    }

    fun fun23() = runBlocking {
        // Пример предложения замены async на withContext
        async(Dispatchers.Main) { "42" }.await()
        launch { println("Will be printed") }.join()
        println("End runBlocking")
    }

    fun fun24() = runBlocking {
        // Пример есть ли разница между launch() {}.join и withContext
        launch(handler + Job()) {// Если в launch передать Job(), то он не будет распространять ошибку
            throw RuntimeException()
        }.join()

        try {
            withContext(handler + Job()) {
                launch {
                    val coroutineExceptionHandler = coroutineContext[CoroutineExceptionHandler]
                    println(coroutineExceptionHandler) // Не null, handle сюда приходит
                    throw IllegalAccessError()
                }.join()
            }
        } catch (exception: Exception) {
            // Not show
            // Exception будет распространен хотя есть Job()
        }

        launch { println("(Not show) Will be printed") }.join()
        println("(Not show) End runBlocking")
    }
}