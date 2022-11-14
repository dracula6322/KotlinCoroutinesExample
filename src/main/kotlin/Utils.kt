import java.util.regex.Pattern

fun main(){
    println("https://www.youtube.com/watch?v=h_UT3eCqz74".split(Pattern.quote(".")).toTypedArray().contentToString())
    println("https://www.youtube.com/watch?v=h_UT3eCqz74".split(".").toTypedArray().contentToString())

    println("www.youtube.com".split(Pattern.quote(".")).toTypedArray().contentToString())
    println("www.youtube.com".split(".").toTypedArray().contentToString())

    println(".".split(Pattern.quote(".")).toTypedArray().contentToString())
    println(".".split(".").toTypedArray().contentToString())

    println("...".split(Pattern.quote(".")).toTypedArray().contentToString())
    println("...".split(".").toTypedArray().contentToString())
}
