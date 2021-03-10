package com.asm.code

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        hello1()
        hello2()
        hello3()
        hello4()
        hello4()
    }

    fun hello1() {

    }

    fun hello2() {
        hello4()
    }

    fun hello3() {
        case1()
        case2()
        case3()

    }

    fun hello4() {
        case1()
        case2()
        case3()
    }

    fun case1() {

    }

    fun case2() {

    }

    fun case3() {

    }


}