import kotlinx.coroutines.*
import rx.Observable
import rx.Subscriber
import rx.schedulers.Schedulers
import java.util.concurrent.TimeUnit

fun main() = Test().fun1()

private class Test {

    private fun println(value: Any?) = kotlin.io.println(Thread.currentThread().toString() + " " + value)

    init {
        Thread.currentThread().uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { _, exception ->
            println("!!!!!!!! Android Crash !!!!!!!!")
            println(exception.toString() + " " + exception.suppressed.contentToString())
        }
    }

    fun fun1() {

        Observable.just("adas").
            subscribe({
                throw RuntimeException()
            }, {
                println(it)
            })

        Thread {
            val newThread = Schedulers.newThread()
            Observable.create { emitter: Subscriber<in String> ->
                for (i in 1..10000) {
//                for (i in 1..129) {
                    emitter.onNext(i.toString())
                    emitter.onNext(i.toString())
                    emitter.onNext(i.toString())
                    emitter.onNext(i.toString())
                    emitter.onNext(i.toString())
                    emitter.onNext(i.toString())
                    emitter.onNext(i.toString())
                    emitter.onNext(i.toString())
                    emitter.onNext(i.toString())
                    emitter.onNext(i.toString())
                    emitter.onNext(i.toString())
                }
            }
                .onBackpressureLatest()
                .doOnNext { println(it) }
//                .observeOn(Schedulers.newThread(), 1)
                .observeOn(newThread, 1)
                .subscribe {
                    Thread.sleep(10)
                    println(it)
                }
        }.start()

        Thread.sleep(TimeUnit.SECONDS.toMillis(100))

    }
}