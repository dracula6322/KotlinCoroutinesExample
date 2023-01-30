fun main() {

    val mass1 = listOf<String>("zxc", "xcv")
    val mass2 = listOf<String>("zxc", "xcv")
    val mass3 = listOf<String>("xcv", "zxc")
    println(mass1 == mass2)
    println(mass2 == mass3)
    println(mass3 == mass1)

}