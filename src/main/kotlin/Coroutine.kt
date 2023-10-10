@file:Suppress("DeferredResultUnused")
@file:OptIn(ExperimentalCoroutinesApi::class)

import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import kotlin.time.measureTime

//fun main() = CoroutineMain().fun1()
//fun main() = CoroutineMain().fun2()
//fun main() = CoroutineMain().fun3()
fun main() = CoroutineMain().fun4()
//fun main() = CoroutineMain().fun5()
//fun main() = CoroutineMain().fun6()

internal class CoroutineMain {
    private fun println(value: Any?) =
        kotlin.io.println(Thread.currentThread().toString() + " " + value)

    fun CoroutineContext.name(): String = this[CoroutineName]?.name ?: "null"

    fun fun1() = runBlocking {
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

    fun fun2() = runBlocking {
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

    fun fun3() = runBlocking(CoroutineName("runBlocking")) {
        // https://youtu.be/w0kfnydnFWI?si=-WVlIdztk1snxEM2&t=1506

        // Так не получится предотвратить отмену второй корутины,
        // Т.к. у launch1 и launch2 в родителях стоит launch, а нет Job(), который является JobImpl,
        // а launch который является StandaloneCoroutine
        val coroutineScope = CoroutineScope(SupervisorJob())
        coroutineScope.launch(Job() + CoroutineName("rootLaunch")) {
            // [CoroutineName(rootLaunch), StandaloneCoroutine{Active}@252aaf0e, Dispatchers.Default]
            println(coroutineContext.toString())
            // JobImpl null
            println(coroutineContext.job.parent?.toString() + coroutineContext.job.parent?.name())
            launch(CoroutineName("launch1")) {
                // [CoroutineName(launch1), StandaloneCoroutine{Active}@27ddd392, Dispatchers.Default]
                println(coroutineContext.toString())
                val parentJob = coroutineContext.job.parent
                // StandaloneCoroutine null
                println(parentJob?.toString() + parentJob?.name())
                delay(1000)
                error("crash")
            }
            launch(CoroutineName("launch2")) {
                // [CoroutineName(launch1), StandaloneCoroutine{Active}@27ddd392, Dispatchers.Default]
                println(coroutineContext.toString())
                val parentJob = coroutineContext.job.parent
                // StandaloneCoroutine null
                println(parentJob?.toString() + parentJob?.name())
                delay(2000)
                println("Not show")
            }
        }.join()


        // Вот тут не будет отмены, т.к. тут напрямую родилтель указан
        val job = SupervisorJob()
        println(job) // SupervisorJobImpl
        launch(job + CoroutineName("launch1")) {
            // [CoroutineName(launch1), StandaloneCoroutine{Active}@27ddd392, BlockingEventLoop@19e1023e]
            println(coroutineContext.toString())
            val parentJob = coroutineContext.job.parent
            // SupervisorJobImpl null
            println(parentJob?.toString() + parentJob?.name())
            delay(1000)
            throw Error("Some error")
        }
        launch(CoroutineName("launch2") + job) {
            // [CoroutineName(launch2), StandaloneCoroutine{Active}@27ddd392, BlockingEventLoop@19e1023e]
            println(coroutineContext.toString())
            val parentJob = coroutineContext.job.parent
            // SupervisorJobImpl null
            println(parentJob?.toString() + parentJob?.name())
            delay(2000)
            println("Will show")
        }
        job.join() // Зависнет пока кто-то не отменит job
        println("(Not show)")
    }

    fun fun4(): Unit = runBlocking {
        // В каком порядке происходит отмена
        try {
            supervisorScope {
                launch {
                    launch { delay(11000) }.invokeOnCompletion { println("First launch end with $it") }
                    launch { delay(12000) }.invokeOnCompletion { println("Second launch end with $it") }
                }
                launch { delay(13000) }.invokeOnCompletion { println("Main launch end with $it") }
                yield()
                cancel()
            }
        } catch (throwable: Throwable) {
            println("Exception from scope $throwable")
        }
        println("End runBlocking")
        // First launch end with kotlinx.coroutines.JobCancellationException
        // Second launch end with kotlinx.coroutines.JobCancellationException
        // Main launch end with kotlinx.coroutines.JobCancellationException
        // Exception from scope kotlinx.coroutines.JobCancellationException
        // End runBlocking
    }

    fun fun5() = runBlocking {
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

    fun fun6() {
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
            }
            println(measureTime)
        }
    }

    fun fun7() = runBlocking {
//         async(Dispatchers.Main) { "42" }.await()
//         Строку выше предлагается заменить на такую
//         withContext(Dispatchers.Main) { "42" }
    }
}