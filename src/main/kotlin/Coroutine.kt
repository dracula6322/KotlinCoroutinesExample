@file:OptIn(ExperimentalCoroutinesApi::class)

import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import kotlin.time.measureTime

fun main() = CoroutineMain().fun17()
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
//fun main() = Main().fun17()
//fun main() = Main().fun18()
//fun main() = Main().fun19()
//fun main() = Main().fun20()
//fun main() = Main().fun21()
//fun main() = Main().fun22()
//fun main() = Main().fun23()
//fun main() = Main().fun24()
//fun main() = Main().fun25()

internal class CoroutineMain {
    // CEH обрабатывается только в launch, в async нужен только для проброса его для детей
    // https://kotlinlang.org/docs/exception-handling.html#coroutineexceptionhandler
    private val handler = CoroutineExceptionHandler { _, throwable ->
        println("CoroutineExceptionHandler $throwable " + throwable.suppressed.contentToString())
    }

    fun CoroutineContext.name(): String = this[CoroutineName]?.name ?: "null"

    private fun println(value: Any?) =
        kotlin.io.println(Thread.currentThread().toString() + " " + value)

    fun assert(value: Boolean) {
        if (!value) {
            throw AssertionError()
        }
    }

    init {
        Thread.currentThread().uncaughtExceptionHandler =
            Thread.UncaughtExceptionHandler { _, throwable ->
                println("!!!!!!!! Android Crash !!!!!!!!")
                println(throwable.toString() + " " + throwable.suppressed.contentToString())
            }
    }


    fun fun3() = runBlocking {
        val job = launch {
            try {
                delay(Long.MAX_VALUE)
            } finally {
                println("Child is cancelled")
            }
        }
        yield()
        println("Cancelling child")
        job.cancel()
        println("Join child")
        job.join()
        println("Parent is not cancelled")
    }

    fun fun4() = runBlocking {
        // Код, который отработает когда отменят корутину
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
        yield()
        println("End")
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

    fun fun21() = runBlocking {
        // В какой очереди выполняются корутины
        // При дефолтном значении CoroutineStart.DEFAULT корутина будет выполняться когда дойдет до первой suspend фукнкции
        // При использовании CoroutineStart.UNDISPATCHED корутина выполняется сразу
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
//                runBlocking { // 6.52s - Если раскомментить один runBlocking
//                    delay(1000)
//                    println("runBlocking")
//                }
//                coroutineScope { // 5.52s - Если раскомментить один coroutineScope
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
//         async(Dispatchers.Main) { "42" }.await()
//         Строку выше предлагается заменить на такую
//         withContext(Dispatchers.Main) { "42" }
    }


}