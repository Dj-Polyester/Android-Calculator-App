package com.example.calc

import android.os.Bundle
import android.util.Log
import android.widget.AdapterView
import android.widget.HorizontalScrollView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.calc.databinding.ActivityMainBinding
import java.util.*
import kotlin.math.pow
import kotlin.math.roundToInt


const val NUM_COLUMNS = 4

enum class TokenType {
    OPERATOR,
    OPERAND,
    LEFT_PARENTHESIS,
    RIGHT_PARENTHESIS,
    START,
}

class MainActivity : AppCompatActivity() {
    private val largestNumber = 10.0.pow(10)-1
    private var overflow:Boolean = false
    private val resPrecisionScale:Int = 4

    private var operatorsStack:Stack<String> = Stack<String>()
    private var outputQueue:Queue<String> = LinkedList<String>()

    private var entriesRaw:MutableList<String> = mutableListOf()
    private var tokenQueue:Queue<String> = LinkedList<String>()

    private var postfixStack:Stack<Double> = Stack<Double>()

    private var binding: ActivityMainBinding? = null
    private val calcLabels: List<String> = listOf(
        "C","<-",".","/",
        "1","2","3","*",
        "4","5","6","+",
        "7","8","9","-",
        "(","0",")","=",
    )
    private val operands: String by lazy {
        ((0..9).toList()+".").joinToString("")
    }
    fun scrollR(){
        binding!!.scrollView.postDelayed(
            Runnable { binding!!.scrollView.fullScroll(HorizontalScrollView.FOCUS_RIGHT) },
            10L
        )
    }
    private val operators: String ="+-/*"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (binding == null) {
            binding = ActivityMainBinding.inflate(this.layoutInflater)
        }
        setContentView(binding!!.root)

        val adapter: CalcGridAdapter = CalcGridAdapter(this@MainActivity, calcLabels)
        binding!!.calcButtons.numColumns = NUM_COLUMNS
        binding!!.calcButtons.adapter = adapter
        binding!!.entryField.text = "0"
        binding!!.calcButtons.onItemClickListener = AdapterView.OnItemClickListener {
            adapterView, view, i, l ->
            processBtnClick(calcLabels[i])
            scrollR()
        }
    }
    private fun processBtnClick(lastClickedBtnTxt:String){
        when(lastClickedBtnTxt){
            "<-" -> {
                if (entriesRaw.isNotEmpty()){
                    entriesRaw.removeLast()
                    binding!!.entryField.text = entriesRaw.joinToString("")
                }
                if (entriesRaw.isEmpty()){
                    binding!!.entryField.text = "0"
                }
            }
            "C" -> {
                entriesRaw.clear()
                binding!!.entryField.text = "0"
            }
            else -> {
                var entryFieldText:String? = null

                val lastCharIsEqual: Boolean = lastClickedBtnTxt == "="
                if (!lastCharIsEqual){
                    if (entriesRaw.size == 1 && entriesRaw.first() == "0"){
                        entriesRaw.clear()
                    }
                    entriesRaw.add(lastClickedBtnTxt)
                }
                else if (entriesRaw.isEmpty()){
                    entriesRaw.add("0")
                }
                var errMessages:MutableList<String> = expValid(lastCharIsEqual,lastCharIsEqual)

                if (errMessages.isEmpty()){
                    Log.d("ActivityMain","Tokens: $tokenQueue")
                    if (lastCharIsEqual){
                        //shunting yard
                        shuntingYard()
                        val result:Double = postfixEval()
                        entryFieldText =
                        if (result > largestNumber) "Error"
                        else if (result < -largestNumber) "-Error"
                        else {
                                val denom:Double = 10.0.pow(resPrecisionScale)
                                val resRefined:Number = if (result.roundToInt().toDouble() == result) result.toInt() else ((result * denom).roundToInt() / denom)
                                resRefined.toString()
                        }
                    }
                    else {
                        entryFieldText = entriesRaw.joinToString("")
                    }
                }
                else{//Erroneous expression
                    if (!lastCharIsEqual){
                        entriesRaw.removeLast()
                    }
                    Toast.makeText(this@MainActivity,errMessages.joinToString("\n"),Toast.LENGTH_LONG).show()
                    entryFieldText = entriesRaw.joinToString("")
                }
                binding!!.entryField.text = entryFieldText!!
            }
        }
    }

    private fun expValid(
        checkParentheses:Boolean = false,
        checkLast:Boolean = false,
    ):MutableList<String>{
        tokenQueue.clear()
        var operand:String = ""
        var numOfPtsInAnOperand:Int = 0
        var numOfOpsInARow:Int = 0
        var lastToken:TokenType = TokenType.START
        var parentheses:Int = 0

        var errMessages:MutableList<String> = mutableListOf()
        if (entriesRaw.isEmpty())
            return errMessages
        for (c:String in entriesRaw){
            if (errMessages.isNotEmpty()) break
            when (c) {
                in operands -> {
                    if (c == "."){
                        ++numOfPtsInAnOperand
                        if (numOfPtsInAnOperand > 1){
                            errMessages.add("One operand can only have a single precision point")
                        }
                    }
                    if (lastToken == TokenType.RIGHT_PARENTHESIS){
                        errMessages.add("Cannot have an operand after right parenthesis")
                    }
                    operand += c
                    lastToken = TokenType.OPERAND
                }
                else -> {
                    if (lastToken == TokenType.OPERAND){
                        tokenQueue.add(operand)
                        operand = ""
                        numOfPtsInAnOperand = 0
                    }
                    when(c){
                        in operators -> {
                            if (lastToken == TokenType.OPERATOR){
                                ++numOfOpsInARow
                                if (numOfOpsInARow>2){
                                    errMessages.add("Cannot have more than 2 operators in a row")
                                }
                            }
                            else {
                                numOfOpsInARow = 1
                            }
                            if (
                                lastToken == TokenType.OPERATOR ||
                                lastToken == TokenType.START ||
                                lastToken == TokenType.LEFT_PARENTHESIS
                            ){
                                if (c in "*/"){
                                    errMessages.add("Cannot have * or / after an operator, left parenthesis, or at the beginning")
                                }
                                else if (c in "+-"){
                                    operand = c
                                }
                            }
                            else{
                                tokenQueue.add(c)
                            }
                            lastToken = TokenType.OPERATOR
                        }
                        "(" -> {
                            if (
                                lastToken == TokenType.OPERAND ||
                                lastToken == TokenType.RIGHT_PARENTHESIS
                            ){
                                errMessages.add("Cannot have a left parenthesis after an operand, or right parenthesis")
                            }
                            ++parentheses
                            lastToken = TokenType.LEFT_PARENTHESIS
                            tokenQueue.add(c)
                        }
                        ")" -> {
                            if (
                                lastToken == TokenType.OPERATOR ||
                                lastToken == TokenType.LEFT_PARENTHESIS ||
                                lastToken == TokenType.START
                            ){
                                errMessages.add("Cannot have a right parenthesis after an operator, left parenthesis, or at the beginning")
                            }
                            --parentheses
                            lastToken = TokenType.RIGHT_PARENTHESIS
                            tokenQueue.add(c)
                        }
                        else -> {
                            Log.e("ActivityMain","c doesn't belong to any type specified by 'TokenType'")
                        }
                    }
                }
            }
        }
        if (lastToken == TokenType.OPERAND){
            tokenQueue.add(operand)
        }
        if (checkParentheses && parentheses != 0){
            errMessages.add("Number of parentheses don't match")
        }
        if (checkLast && entriesRaw.last() in "($operators"){
            errMessages.add("Last element cannot be one of (, *, /, + and -")
        }
        return errMessages
    }
    private fun precedence(op:String):Byte{
        return when(op){
            "*" -> 1
            "/" -> 1
            "+" -> 0
            "-" -> 0
            else -> Log.e("ActivityMain","The operator $op is undefined.").toByte()
        }
    }
    private fun shuntingYard(){
        while (tokenQueue.isNotEmpty()){
            when(val token:String = tokenQueue.remove()){
                in operators -> {
                    var top:String? = if (operatorsStack.isEmpty()) null else operatorsStack.peek()
                    while (operatorsStack.isNotEmpty()) {
                        if (top!! == "(" || precedence(top!!) < precedence(token)) break
                        else {
                            outputQueue.add(operatorsStack.pop())
                        }
                    }
                    operatorsStack.push(token)
                }
                "(" -> {
                    operatorsStack.push(token)
                }
                ")" -> {
                    while (operatorsStack.isNotEmpty()) {
                        if (operatorsStack.peek() == "(") {
                            break
                        }
                        else {
                            outputQueue.add(operatorsStack.pop())
                        }
                    }
                    if (operatorsStack.isNotEmpty()){
                        operatorsStack.pop()
                    }
                    else{
                        Log.e("ActivityMain","Parentheses don't match")
                    }
                }
                else -> {
                    val num:Double? = token.toDoubleOrNull()
                    if (num == null){
                        Log.e("ActivityMain","The token $token cannot be converted to a double")
                    }
                    else{//operand
                        outputQueue.add(token)
                    }
                }
            }
        }
        while (operatorsStack.isNotEmpty()){
            outputQueue.add(operatorsStack.pop())
        }
    }
    private fun postfixEval():Double{
        overflow = false
        while (outputQueue.isNotEmpty()){
            when(val token:String = outputQueue.remove()){
                in operators -> {
                    val snd:Double = postfixStack.pop()
                    val fst:Double = postfixStack.pop()
                    val result:Double = eval(token,fst,snd)
                    //overflow = checkOverflow(token,fst,snd,result)
                    postfixStack.push(result)
                }
                else -> {
                    val num:Double? = token.toDoubleOrNull()
                    if (num == null){
                        Log.e("ActivityMain","The token $token cannot be converted to a double")
                    }
                    else{//operand
                        postfixStack.push(num!!)
                    }
                }
            }
        }
        if (postfixStack.size != 1){
            Log.e("ActivityMain","postfixStack.size: ${postfixStack.size} is not 1")
        }
        return postfixStack.pop()
    }
    private fun checkOverflow(op:String,fst:Double, snd:Double, res:Double):Boolean{
        return when(op){
            "+"->res-snd == fst
            "-"->res+snd == fst
            "/"->res*snd == fst
            "*"->res/snd == fst
            else -> {
                Log.e("ActivityMain","Invalid operator $op") == 0
            }
        }
    }
    private fun eval(op:String,fst:Double, snd:Double):Double{
        return when(op){
            "+"->fst+snd
            "-"->fst-snd
            "/"->fst/snd
            "*"->fst*snd
            else -> {
                Log.e("ActivityMain","Invalid operator $op").toDouble()
            }
        }
    }
}