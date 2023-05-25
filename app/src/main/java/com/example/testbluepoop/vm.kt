package com.example.testbluepoop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class Vm : ViewModel() {
    val variantConnectionFirst = MutableStateFlow("")
    val variantConnectionSecond = MutableStateFlow("")
    val headsetManual = MutableStateFlow("")
}