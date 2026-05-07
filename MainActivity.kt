package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.random.Random

//holds info about each cell on the grid
data class Cell(
    val text: String = "",
    val isOp: Boolean = false,
    val isFixed: Boolean = false,
    val isBlank: Boolean = false,
    val isEmpty: Boolean = true,
    val userVal: String = "",
    val eqIds: List<Int> = emptyList(),
    val color: Color = Color.Transparent
)

//stores each equation - the 5 cells are: number, operator, number, equals, result
data class Eq(
    val id: Int,
    val cells: List<Pair<Int, Int>>,
    val a: Int,
    val op: Char,
    val b: Int,
    val res: Int,
    val blankIdx: Int
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { App() }
    }
}

//generates a random equation
fun genEq(): Array<Any>? {
    val ops = charArrayOf('+', '-', 'x', '/')
    val op = ops[Random.nextInt(4)]

    // try a few times to get valid numbers
    for (i in 0..30) {
        var a: Int; var b: Int; var r: Int
        when (op) {
            '+' -> { a = Random.nextInt(1, 50); b = Random.nextInt(1, 50); r = a + b }
            '-' -> { a = Random.nextInt(2, 99); b = Random.nextInt(1, a); r = a - b }
            'x' -> { a = Random.nextInt(2, 13); b = Random.nextInt(2, 13); r = a * b }
            '/' -> { b = Random.nextInt(2, 13); r = Random.nextInt(1, 13); a = b * r } // make sure it divides evenly
            else -> continue
        }
        if (r > 0 && a > 0 && b > 0) return arrayOf(a, op, b, r)
    }
    return null
}

//converts the operator char to a nice symbol for display
fun opStr(op: Char) = when (op) {
    '+' -> "+"; '-' -> "-"; 'x' -> "×"; '/' -> "÷"; else -> "?"
}

//main function that builds the whole puzzle grid and places equations on it
fun makePuzzle(
    reqCount: Int? = null,
    advanced: Boolean = false
): Triple<Array<Array<Cell>>, List<Eq>, Pair<Int, Int>> {

    //random grid size between 11 and 20 for both dimensions
    val rows = Random.nextInt(11, 21)
    val cols = Random.nextInt(11, 21)
    val grid = Array(rows) { Array(cols) { Cell() } }
    val used = Array(rows) { BooleanArray(cols) } // keeps track of which cells are taken
    val eqs = mutableListOf<Eq>()
    var eid = 0

    //if user didnt specify how many equations, pick a random amount
    val target = reqCount ?: Random.nextInt(6, minOf(15, (rows + cols) / 2))
    var placed = 0
    var att = 0

    while (placed < target && att < target * 20) {
        att++
        val horiz = Random.nextBoolean() //randomly decide horizontal or vertical
        val e = genEq() ?: continue
        val a = e[0] as Int; val op = e[1] as Char; val b = e[2] as Int; val r = e[3] as Int

        //need at least 5 cells in a row/column for an equation
        if (horiz && cols < 5 || !horiz && rows < 5) continue

        val sR = if (horiz) Random.nextInt(rows) else Random.nextInt(rows - 4)
        val sC = if (horiz) Random.nextInt(cols - 4) else Random.nextInt(cols)
        val pos = (0..4).map {
            if (horiz) Pair(sR, sC + it) else Pair(sR + it, sC)
        }

        //check none of these cells are already used
        if (pos.any { used[it.first][it.second] }) continue

        //also check the cells right before and after so equations dont touch each other
        if (horiz) {
            if (sC > 0 && used[sR][sC - 1]) continue
            if (sC + 5 < cols && used[sR][sC + 5]) continue
        } else {
            if (sR > 0 && used[sR - 1][sC]) continue
            if (sR + 5 < rows && used[sR + 5][sC]) continue
        }

        //pick which number in the equation will be blank (the one the user has to guess)
        //for advanced mode dont make any blank here, do it later
        val blankPos = if (advanced) -1 else listOf(0, 2, 4)[Random.nextInt(3)]
        val vals = listOf(a.toString(), opStr(op), b.toString(), "=", r.toString())

        //fill in the grid cells for this equation
        for (i in 0..4) {
            val (cr, cc) = pos[i]
            used[cr][cc] = true
            val existIds = grid[cr][cc].eqIds

            if (i == 1 || i == 3) {
                // operator or equals sign
                grid[cr][cc] = Cell(
                    vals[i], isOp = true, isFixed = true,
                    isEmpty = false, eqIds = existIds + eid
                )
            } else {
                // number cell - might be blank
                val blank = blankPos == i
                grid[cr][cc] = Cell(
                    if (blank) "" else vals[i],
                    isFixed = !blank, isBlank = blank,
                    isEmpty = false, eqIds = existIds + eid
                )
            }
        }
        eqs.add(Eq(eid, pos, a, op, b, r, blankPos))
        eid++
        placed++
    }

    // for advanced mode, try to make as many cells blank as possible
    if (advanced) maximiseBlanks(grid, eqs)

    return Triple(grid, eqs, Pair(rows, cols))
}

// ADVANCED LEVEL - tries to remove as many numbers as possible
// while keeping the puzzle solvable
fun maximiseBlanks(grid: Array<Array<Cell>>, eqs: List<Eq>) {
    val numCells = mutableListOf<Pair<Int, Int>>()
    for (eq in eqs) {
        for (i in listOf(0, 2, 4)) {
            val p = eq.cells[i]
            if (p !in numCells) numCells.add(p)
        }
    }
    numCells.shuffle()

    val blanks = mutableSetOf<Pair<Int, Int>>()
    for (c in numCells) {
        blanks.add(c)
        if (canSolve(grid, eqs, blanks)) {
            grid[c.first][c.second] = grid[c.first][c.second].copy(
                isBlank = true, isFixed = false, text = ""
            )
        } else {
            blanks.remove(c)
        }
    }
}

// checks if the puzzle can be solved with the given blank cells
fun canSolve(grid: Array<Array<Cell>>, eqs: List<Eq>, blanks: Set<Pair<Int, Int>>): Boolean {
    val solved = mutableMapOf<Pair<Int, Int>, Int>()
    val unsolved = blanks.toMutableSet()
    var progress = true

    while (progress && unsolved.isNotEmpty()) {
        progress = false
        for (eq in eqs) {
            val unknowns = listOf(0, 2, 4).filter {
                eq.cells[it] in unsolved && eq.cells[it] !in solved
            }
            if (unknowns.size != 1) continue

            val ui = unknowns[0]
            val p = eq.cells[ui]

            val v = IntArray(5)
            for (idx in listOf(0, 2, 4)) {
                val pp = eq.cells[idx]
                v[idx] = solved[pp]
                    ?: if (pp !in blanks) (grid[pp.first][pp.second].text.toIntOrNull() ?: 0)
                    else -999
            }

            val ans = when (ui) {
                0 -> solveFirst(eq.op, v[2], v[4])
                2 -> solveSecond(eq.op, v[0], v[4])
                4 -> calc(eq.op, v[0], v[2])
                else -> null
            }
            if (ans != null) {
                solved[p] = ans
                unsolved.remove(p)
                progress = true
            }
        }
    }
    return unsolved.isEmpty()
}

// does the actual arithmetic
fun calc(op: Char, a: Int, b: Int): Int? = when (op) {
    '+' -> a + b
    '-' -> a - b
    'x' -> a * b
    '/' -> if (b != 0 && a % b == 0) a / b else null
    else -> null
}

// works backwards to find the first number: ? op b = result
fun solveFirst(op: Char, b: Int, r: Int): Int? = when (op) {
    '+' -> r - b
    '-' -> r + b
    'x' -> if (b != 0 && r % b == 0) r / b else null
    '/' -> r * b
    else -> null
}

// works backwards to find the second number: a op ? = result
fun solveSecond(op: Char, a: Int, r: Int): Int? = when (op) {
    '+' -> r - a
    '-' -> a - r
    'x' -> if (a != 0 && r % a == 0) r / a else null
    '/' -> if (r != 0 && a % r == 0) a / r else null
    else -> null
}

// checks if an equation on the grid is correct, wrong, or still incomplete
fun checkEq(eq: Eq, grid: Array<Array<Cell>>): Boolean? {
    val nums = mutableListOf<Int>()
    for (idx in listOf(0, 2, 4)) {
        val (r, c) = eq.cells[idx]
        val cell = grid[r][c]
        val s = if (cell.isBlank) {
            if (cell.userVal.isEmpty()) return null
            cell.userVal
        } else {
            cell.text
        }
        nums.add(s.toIntOrNull() ?: return null)
    }
    return calc(eq.op, nums[0], nums[1]) == nums[2]
}

// =====================
// UI STUFF STARTS HERE
// =====================

@Composable
fun App() {
    var screen by rememberSaveable { mutableStateOf("menu") }
    var advanced by rememberSaveable { mutableStateOf(false) }
    var nextEqCount by rememberSaveable { mutableStateOf<Int?>(null) }

    when (screen) {
        "menu" -> MenuScreen(
            onNew = { advanced = false; nextEqCount = null; screen = "game" },
            onAdv = { advanced = true; nextEqCount = null; screen = "game" }
        )
        "game" -> GameScreen(
            isAdvanced = advanced,
            reqEqs = nextEqCount,
            onBack = { screen = "menu" },
            onPlayAgain = { count -> nextEqCount = count; screen = "game" }
        )
    }
}

// first screen with the 3 buttons
@Composable
fun MenuScreen(
    onNew: () -> Unit,
    onAdv: () -> Unit
) {
    var showAbout by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Cross Math Puzzle",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A237E),
            modifier = Modifier.padding(bottom = 40.dp)
        )

        Button(
            onClick = onNew,
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A237E))
        ) {
            Text("New Game", fontSize = 18.sp, modifier = Modifier.padding(8.dp))
        }

        Button(
            onClick = onAdv,
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A148C))
        ) {
            Text("Advanced Level", fontSize = 18.sp, modifier = Modifier.padding(8.dp))
        }

        Button(
            onClick = { showAbout = true },
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF004D40))
        ) {
            Text("About", fontSize = 18.sp, modifier = Modifier.padding(8.dp))
        }
    }

    if (showAbout) {
        AlertDialog(
            icon = { Icon(Icons.Default.Info, contentDescription = "Info") },
            title = { Text("About", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Student ID: w2047567\nName: Charanjot Sidhu",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        "I confirm that I understand what plagiarism is and have read and " +
                                "understood the section on Assessment Offences in the Essential " +
                                "Information for Students. The work that I have submitted is entirely " +
                                "my own. Any work from other authors is duly referenced and acknowledged.",
                        fontSize = 14.sp
                    )
                }
            },
            onDismissRequest = { showAbout = false },
            confirmButton = {
                TextButton(onClick = { showAbout = false }) { Text("OK") }
            }
        )
    }
}

// the actual game screen with the grid, timer, score etc
@Composable
fun GameScreen(isAdvanced: Boolean, reqEqs: Int?, onBack: () -> Unit, onPlayAgain: (Int?) -> Unit) {
    // FIX 1: use plain remember (not rememberSaveable) for non-serializable types
    var gridData by remember { mutableStateOf(emptyArray<Array<Cell>>()) }
    var eqs by remember { mutableStateOf(emptyList<Eq>()) }

    var rows by rememberSaveable { mutableStateOf(11) }
    var cols by rememberSaveable { mutableStateOf(11) }
    var score by rememberSaveable { mutableStateOf(0) }
    var gameOver by rememberSaveable { mutableStateOf(false) }
    var complete by rememberSaveable { mutableStateOf(false) }

    var timerOn by rememberSaveable { mutableStateOf(false) }
    var timerRunning by rememberSaveable { mutableStateOf(false) }
    var timeLeft by rememberSaveable { mutableStateOf(60) }

    var showDialog by rememberSaveable { mutableStateOf(false) }
    var selR by rememberSaveable { mutableStateOf(-1) }
    var selC by rememberSaveable { mutableStateOf(-1) }
    var input by rememberSaveable { mutableStateOf("") }

    // end-of-game "play again" prompt
    var showEndDialog by rememberSaveable { mutableStateOf(false) }
    var endEqInput by rememberSaveable { mutableStateOf("") }

    // FIX 2: run makePuzzle on a background thread so it doesn't block the UI
    LaunchedEffect(Unit) {
        val result = withContext(Dispatchers.Default) {
            makePuzzle(reqEqs, isAdvanced)
        }
        gridData = result.first
        eqs = result.second
        rows = result.third.first
        cols = result.third.second
        score = 0
        gameOver = false
        complete = false
        timeLeft = 60
        timerRunning = false
        timerOn = false
    }

    // FIX 3: only key on timerRunning, loop inside the effect instead of
    // restarting the effect every time timeLeft changes (avoids rapid re-triggers)
    LaunchedEffect(timerRunning) {
        while (timerRunning && timeLeft > 0 && !gameOver && !complete) {
            delay(1000L)
            timeLeft--
            if (timeLeft <= 0) {
                gameOver = true
                timerRunning = false
                endEqInput = ""
                showEndDialog = true
            }
        }
    }

    BackHandler { onBack() }

    if (gridData.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(Modifier.fillMaxSize().padding(8.dp)) {

        Row(
            Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (timerOn) "⏱ $timeLeft" else "⏱ --",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (timeLeft <= 10 && timerOn) Color.Red else Color(0xFF1A237E),
                    modifier = Modifier.padding(end = 8.dp)
                )
                Switch(
                    checked = timerOn,
                    onCheckedChange = {
                        if (!gameOver && !complete) {
                            timerOn = it
                            timerRunning = it
                            if (it) timeLeft = 60
                        }
                    }
                )
            }
            Text(
                "Score: $score",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A237E)
            )
        }

        if (gameOver) {
            Text(
                "GAME OVER!", fontSize = 28.sp, fontWeight = FontWeight.Bold,
                color = Color.Red, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )
        }
        if (complete) {
            Text(
                "PUZZLE COMPLETE!", fontSize = 28.sp, fontWeight = FontWeight.Bold,
                color = Color(0xFF2E7D32), textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )
        }

        Box(
            Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState())
                .verticalScroll(rememberScrollState())
        ) {
            Column {
                for (r in 0 until rows) {
                    Row {
                        for (c in 0 until cols) {
                            val cell = gridData[r][c]

                            val bg = when {
                                cell.isEmpty -> Color(0xFFF5F5F5)
                                cell.color != Color.Transparent -> cell.color
                                cell.isBlank -> Color(0xFFFFF9C4)
                                cell.isOp -> Color(0xFFE8EAF6)
                                else -> Color.White
                            }

                            val tc = when {
                                cell.color == Color(0xFF4CAF50) || cell.color == Color(0xFFF44336) -> Color.White
                                cell.isOp -> Color(0xFF1A237E)
                                cell.isBlank && cell.userVal.isNotEmpty() -> Color(0xFF0D47A1)
                                cell.isBlank -> Color(0xFF757575)
                                else -> Color.Black
                            }

                            val display = when {
                                cell.isBlank && cell.userVal.isNotEmpty() -> cell.userVal
                                cell.isBlank -> "?"
                                else -> cell.text
                            }

                            Box(
                                Modifier
                                    .size(44.dp)
                                    .border(
                                        if (cell.isEmpty) 0.dp else 1.dp,
                                        if (cell.isEmpty) Color.Transparent else Color(0xFFBDBDBD)
                                    )
                                    .background(bg)
                                    .then(
                                        if (cell.isBlank && !gameOver && !complete)
                                            Modifier.clickable {
                                                selR = r
                                                selC = c
                                                input = cell.userVal
                                                showDialog = true
                                            }
                                        else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!cell.isEmpty) {
                                    Text(
                                        display,
                                        fontSize = if (display.length > 2) 12.sp else 16.sp,
                                        fontWeight = if (cell.isFixed) FontWeight.Bold else FontWeight.Normal,
                                        color = tc,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Enter a number") },
            text = {
                OutlinedTextField(
                    value = input,
                    onValueChange = {
                        if (it.all { c -> c.isDigit() } || it.isEmpty()) input = it
                    },
                    label = { Text("Number") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (input.isNotEmpty() && selR >= 0 && selC >= 0) {
                        val g = gridData.map { it.clone() }.toTypedArray()
                        g[selR][selC] = g[selR][selC].copy(userVal = input, text = input)

                        for (row in g) {
                            for (i in row.indices) {
                                row[i] = row[i].copy(color = Color.Transparent)
                            }
                        }

                        val cellResults = mutableMapOf<Pair<Int, Int>, MutableList<Boolean>>()
                        var sc = 0
                        for (eq in eqs) {
                            val res = checkEq(eq, g) ?: continue
                            if (res) sc++
                            for (p in eq.cells) {
                                cellResults.getOrPut(p) { mutableListOf() }.add(res)
                            }
                        }

                        for ((p, results) in cellResults) {
                            val clr = if (results.any { !it }) Color(0xFFF44336) else Color(0xFF4CAF50)
                            g[p.first][p.second] = g[p.first][p.second].copy(color = clr)
                        }

                        score = sc
                        gridData = g

                        if (eqs.all { checkEq(it, g) == true }) {
                            complete = true
                            endEqInput = ""
                            showEndDialog = true
                        }
                    }
                    showDialog = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            }
        )
    }
    // end-of-game dialog: shows score and asks how many equations for next game
    if (showEndDialog) {
        AlertDialog(
            onDismissRequest = { showEndDialog = false },
            title = {
                Text(
                    if (complete) "Puzzle Complete! 🎉" else "Game Over!",
                    fontWeight = FontWeight.Bold,
                    color = if (complete) Color(0xFF2E7D32) else Color.Red
                )
            },
            text = {
                Column {
                    Text(
                        "Final score: $score",
                        fontSize = 18.sp,
                        modifier = androidx.compose.ui.Modifier.padding(bottom = 12.dp)
                    )
                    Text(
                        "How many equations for the next game? (leave blank for random)",
                        fontSize = 14.sp,
                        modifier = androidx.compose.ui.Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = endEqInput,
                        onValueChange = {
                            if (it.all { c -> c.isDigit() } || it.isEmpty()) endEqInput = it
                        },
                        label = { Text("Number of equations") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showEndDialog = false
                    onPlayAgain(endEqInput.toIntOrNull())
                }) {
                    Text("Play Again")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showEndDialog = false
                    onBack()
                }) {
                    Text("Main Menu")
                }
            }
        )
    }
}