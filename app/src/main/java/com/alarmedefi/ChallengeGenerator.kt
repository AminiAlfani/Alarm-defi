package com.alarmedefi

import kotlin.random.Random

data class Challenge(
    val type: String,
    val question: String,
    val answers: List<String>
)

object ChallengeGenerator {

    private val riddles = listOf(
        Triple("Qu'est-ce qui monte et ne redescend jamais ?", listOf("l'age", "l'âge", "age", "âge"), 0),
        Triple("Plus j'en ai, moins tu vois. Qu'est-ce que c'est ?", listOf("l'obscurité", "obscurité", "le noir", "noir", "la nuit", "nuit"), 0),
        Triple("Qu'est-ce qui a des dents mais ne mord pas ?", listOf("un peigne", "peigne", "un engrenage", "engrenage"), 0),
        Triple("Qu'est-ce qui appartient à toi mais que les autres utilisent plus que toi ?", listOf("ton nom", "nom", "mon nom", "le nom", "prenom", "prénom"), 0),
        Triple("Je parle sans bouche et j'entends sans oreilles. Qui suis-je ?", listOf("l'écho", "echo", "écho", "l'echo"), 0),
        Triple("Qu'est-ce qui a une tête et un pied mais ni bras ni jambes ?", listOf("un lit", "lit"), 0),
        Triple("Combien font la moitié de deux plus deux ?", listOf("3", "trois"), 0),
        Triple("Quel mois de l'année a 28 jours ?", listOf("tous", "tous les mois", "les douze", "12"), 0)
    )

    private val phpCopy = listOf(
        "echo 'Salut';",
        "\$x = 5;",
        "if (\$a > 3) { echo \$a; }",
        "foreach (\$list as \$item) {}",
        "\$nom = trim(\$_POST['nom']);",
        "return count(\$tab);",
        "\$sum = \$a + \$b;",
        "echo strlen(\$texte);"
    )

    private val phpBugs = listOf(
        Pair("echo 'Salut'", listOf("echo 'Salut';", "echo "Salut";")),
        Pair("x = 5;", listOf("\$x = 5;", "\$x=5;")),
        Pair("if (\$a > 3 { echo \$a; }", listOf("if (\$a > 3) { echo \$a; }", "if (\$a > 3) {echo \$a;}")),
        Pair("\$total = \$a + ;", listOf("\$total = \$a + \$b;", "\$total=\$a+\$b;")),
        Pair("for (\$i = 0; \$i < 3; \$i++ { }", listOf("for (\$i = 0; \$i < 3; \$i++) { }", "for (\$i=0; \$i<3; \$i++) { }")),
        Pair("\$tab = array(1, 2, 3;", listOf("\$tab = array(1, 2, 3);", "\$tab=array(1,2,3);"))
    )

    fun next(): Challenge = when (Random.nextInt(4)) {
        0 -> math()
        1 -> riddle()
        2 -> copy()
        else -> bug()
    }

    private fun math(): Challenge {
        val kind = Random.nextInt(3)
        return when (kind) {
            0 -> {
                val a = Random.nextInt(12, 60)
                val b = Random.nextInt(12, 60)
                Challenge("Calcul", "$a + $b = ?", listOf((a + b).toString()))
            }
            1 -> {
                val a = Random.nextInt(30, 99)
                val b = Random.nextInt(11, a)
                Challenge("Calcul", "$a - $b = ?", listOf((a - b).toString()))
            }
            else -> {
                val a = Random.nextInt(3, 13)
                val b = Random.nextInt(3, 13)
                Challenge("Calcul", "$a × $b = ?", listOf((a * b).toString()))
            }
        }
    }

    private fun riddle(): Challenge {
        val r = riddles.random()
        return Challenge("Énigme", r.first, r.second)
    }

    private fun copy(): Challenge {
        val code = phpCopy.random()
        return Challenge("Code PHP à recopier", code, listOf(code))
    }

    private fun bug(): Challenge {
        val b = phpBugs.random()
        return Challenge("Bug PHP à corriger", b.first, b.second)
    }

    private fun clean(s: String): String =
        s.trim().replace("\s+".toRegex(), " ")
            .replace('’', ''').replace('‘', ''')
            .replace('“', '"').replace('”', '"')

    fun isCorrect(challenge: Challenge, input: String): Boolean {
        val given = clean(input)
        val strict = challenge.type.startsWith("Code") || challenge.type.startsWith("Bug")
        return challenge.answers.any {
            if (strict) clean(it) == given else clean(it).equals(given, ignoreCase = true)
        }
    }
}
