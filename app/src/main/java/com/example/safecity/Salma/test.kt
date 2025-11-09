// File: TodoApp.kt
data class Todo(val id: Int, var title: String, var done: Boolean = false)

fun main() {
    val todos = mutableListOf<Todo>()
    var nextId = 1
    while (true) {
        println("\n1) Lister  2) Ajouter  3) Marquer comme fait  4) Supprimer  5) Quitter")
        print("Choix: "); when (readLine()?.trim()) {
            "1" -> {
                if (todos.isEmpty()) println("Aucune tâche.")
                else todos.forEach { println("${it.id}. [${if (it.done) "x" else " "}] ${it.title}") }
            }
            "2" -> {
                print("Titre: "); val t = readLine().orEmpty()
                todos += Todo(nextId++, t)
                println("Ajouté.")
            }
            "3" -> {
                print("ID à marquer: "); val id = readLine()?.toIntOrNull()
                val item = todos.find { it.id == id }
                if (item != null) { item.done = true; println("Marqué.") } else println("Introuvable.")
            }
            "4" -> {
                print("ID à supprimer: "); val id = readLine()?.toIntOrNull()
                if (todos.removeIf { it.id == id }) println("Supprimé.") else println("Introuvable.")
            }
            "5" -> { println("Bye 👋"); return }
            else -> println("Choix invalide.")
        }
    }
}


}