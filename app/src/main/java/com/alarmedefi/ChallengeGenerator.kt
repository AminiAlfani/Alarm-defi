package com.alarmedefi

import kotlin.random.Random

data class Challenge(
    val type: String,
    val question: String,
    val answers: List<String>
)

object ChallengeGenerator {

    private val riddles = listOf(
        Pair("Qu'est-ce qui monte et ne redescend jamais ?", listOf("l'age", "l'âge", "age", "âge")),
        Pair("Plus j'en ai, moins tu vois. Qu'est-ce que c'est ?", listOf("l'obscurité", "obscurité", "le noir", "noir", "la nuit", "nuit")),
        Pair("Qu'est-ce qui a des dents mais ne mord pas ?", listOf("un peigne", "peigne", "un engrenage", "engrenage")),
        Pair("Qu'est-ce qui t'appartient mais que les autres utilisent plus que toi ?", listOf("ton nom", "nom", "mon nom", "le nom", "prénom", "prenom")),
        Pair("Je parle sans bouche et j'entends sans oreilles. Qui suis-je ?", listOf("l'écho", "écho", "echo", "l'echo")),
        Pair("Qu'est-ce qui a une tête et un pied mais ni bras ni jambes ?", listOf("un lit", "lit")),
        Pair("Combien font la moitié de deux plus deux ?", listOf("3", "trois")),
        Pair("Quel mois de l'année a 28 jours ?", listOf("tous", "tous les mois", "les douze", "12")),
        Pair("Je suis plein de trous mais je retiens l'eau. Qui suis-je ?", listOf("une éponge", "éponge", "l'éponge", "eponge")),
        Pair("Plus on me sèche, plus je deviens humide. Qui suis-je ?", listOf("une serviette", "serviette", "la serviette")),
        Pair("Qu'est-ce qui a des aiguilles mais ne pique pas ?", listOf("une horloge", "horloge", "une montre", "montre")),
        Pair("Qu'est-ce qui court mais n'a pas de jambes ?", listOf("l'eau", "eau", "une rivière", "rivière", "un fleuve", "fleuve"))
    )

    private val phpCopy = listOf(
        "echo 'Salut';",
        "§x = 5;",
        "if (§a > 3) { echo §a; }",
        "foreach (§list as §item) {}",
        "§nom = trim(§_POST['nom']);",
        "return count(§tab);",
        "§sum = §a + §b;",
        "echo strlen(§texte);",
        "§actif = true;",
        "§prix = 19.99;",
        "§users = ['Alice', 'Bob'];",
        "if (!empty(§email)) { return true; }",
        "§clean = htmlspecialchars(trim(§val));",
        "for (§i = 0; §i < 5; §i++) { §total += §i; }",
        "function carre(§n) { return §n * §n; }",
        "§keys = array_keys(§data);",
        "§merged = array_merge(§a, §b);",
        "§found = in_array('admin', §roles);",
        "§parts = explode(',', §csv);",
        "§line = implode('-', §parts);",
        "§code = strtoupper(trim(§input));",
        "while (§count > 0) { §count--; }",
        "§cfg = ['host' => 'localhost', 'port' => 3306];",
        "switch (§role) { case 'admin': break; }"
    )

    private val phpBugs = listOf(
        Pair("echo 'Salut'", listOf("echo 'Salut';", "echo \"Salut\";")),
        Pair("x = 5;", listOf("§x = 5;", "§x=5;")),
        Pair("if (§a > 3 { echo §a; }", listOf("if (§a > 3) { echo §a; }", "if (§a > 3) {echo §a;}")),
        Pair("§total = §a + ;", listOf("§total = §a + §b;", "§total=§a+§b;")),
        Pair("for (§i = 0; §i < 3; §i++ { }", listOf("for (§i = 0; §i < 3; §i++) { }", "for (§i=0; §i<3; §i++) { }")),
        Pair("§tab = array(1, 2, 3;", listOf("§tab = array(1, 2, 3);", "§tab=array(1,2,3);")),
        Pair("function add(§a, §b { return §a + §b; }", listOf("function add(§a, §b) { return §a + §b; }", "function add(§a,§b){return §a+§b;}")),
        Pair("foreach (§items as §item) { echo §item", listOf("foreach (§items as §item) { echo §item; }", "foreach (§items as §item) {echo §item;}")),
        Pair("§msg = 'Bonjour;", listOf("§msg = 'Bonjour';", "§msg=\"Bonjour\";")),
        Pair("if (§ok = true) { return 1; }", listOf("if (§ok == true) { return 1; }", "if (§ok === true) { return 1; }")),
        Pair("return count(§liste", listOf("return count(§liste);", "return count(§liste);")),
        Pair("§val = trim(§_GET['id']", listOf("§val = trim(§_GET['id']);", "§val=trim(§_GET['id']);")),
        Pair("while (§n > 0 { §n--; }", listOf("while (§n > 0) { §n--; }", "while (§n>0){§n--;}"))
    )

    private val jsCopy = listOf(
        "const total = a + b;",
        "let count = 0;",
        "const double = (x) => x * 2;",
        "items.forEach((el) => console.log(el));",
        "const ok = list.length > 0;",
        "return items.filter(Boolean);",
        "const text = str.trim().toLowerCase();",
        "async function load() { return await fetchData(); }"
    )

    private val pyCopy = listOf(
        "total = sum(values)",
        "for item in items: print(item)",
        "clean = text.strip().lower()",
        "def square(n): return n * n",
        "pairs = [x * 2 for x in nums]",
        "if key in data: return data[key]"
    )

    private fun php(s: String): String = s.replace('§', '$')

    fun next(): Challenge = when (Random.nextInt(100)) {
        in 0..33 -> math()
        in 34..69 -> codeChallenge()
        else -> riddle()
    }

    private fun codeChallenge(): Challenge = when (Random.nextInt(4)) {
        0 -> copy()
        1 -> bug()
        2 -> jsChallenge()
        else -> pyChallenge()
    }

    private fun math(): Challenge {
        return when (Random.nextInt(6)) {
            0 -> {
                val a = Random.nextInt(12, 95)
                val b = Random.nextInt(12, 95)
                Challenge("Calcul mental", "$a + $b = ?", listOf((a + b).toString()))
            }
            1 -> {
                val a = Random.nextInt(35, 140)
                val b = Random.nextInt(11, a)
                Challenge("Calcul mental", "$a - $b = ?", listOf((a - b).toString()))
            }
            2 -> {
                val a = Random.nextInt(3, 16)
                val b = Random.nextInt(3, 15)
                Challenge("Calcul mental", "$a × $b = ?", listOf((a * b).toString()))
            }
            3 -> {
                val b = Random.nextInt(2, 13)
                val ans = Random.nextInt(3, 16)
                val a = b * ans
                Challenge("Calcul mental", "$a ÷ $b = ?", listOf(ans.toString()))
            }
            4 -> {
                val a = Random.nextInt(6, 25)
                val b = Random.nextInt(4, 15)
                val c = Random.nextInt(5, 30)
                val ans = (a * b) + c
                Challenge("Calcul mental", "($a × $b) + $c = ?", listOf(ans.toString()))
            }
            else -> {
                val a = Random.nextInt(20, 80)
                val b = Random.nextInt(10, 50)
                val c = Random.nextInt(5, 25)
                val ans = a + b - c
                Challenge("Calcul mental", "$a + $b - $c = ?", listOf(ans.toString()))
            }
        }
    }

    private fun riddle(): Challenge {
        val r = riddles.random()
        return Challenge("Énigme", r.first, r.second)
    }

    private fun copy(): Challenge {
        val code = php(phpCopy.random())
        return Challenge("Code PHP à recopier", code, listOf(code))
    }

    private fun bug(): Challenge {
        val b = phpBugs.random()
        return Challenge("Bug PHP à corriger", php(b.first), b.second.map { php(it) })
    }

    private fun jsChallenge(): Challenge {
        val code = jsCopy.random()
        return Challenge("Code JS à recopier", code, listOf(code))
    }

    private fun pyChallenge(): Challenge {
        val code = pyCopy.random()
        return Challenge("Code Python à recopier", code, listOf(code))
    }

    private fun clean(s: String): String =
        s.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.joinToString(" ")
            .replace("’", "'").replace("‘", "'")
            .replace("“", "\"").replace("”", "\"")

    fun isCorrect(challenge: Challenge, input: String): Boolean {
        val given = clean(input)
        val strict = challenge.type.startsWith("Code") || challenge.type.startsWith("Bug")
        return challenge.answers.any {
            if (strict) clean(it) == given else clean(it).equals(given, ignoreCase = true)
        }
    }
}
