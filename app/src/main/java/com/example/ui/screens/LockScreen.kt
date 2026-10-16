package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun LockScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var enteredPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color(0x3300E5FF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = CyanPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "ProxXQuepus Locked",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (isError) "Incorrect PIN, try again" else "Enter your PIN to unlock",
                color = if (isError) Color(0xFFFF5252) else TextSecondary,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            // PIN Dots Indicator
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                for (i in 0 until 4) {
                    val isFilled = i < enteredPin.length
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(if (isFilled) CyanPrimary else Color(0x3300E5FF))
                            .border(1.dp, if (isError) Color(0xFFFF5252) else DarkSurfaceBorder, CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Compact Keypad Grid (1-9, 0, Backspace)
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                for (row in 0..2) {
                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        for (col in 1..3) {
                            val digit = row * 3 + col
                            KeypadButton(
                                text = digit.toString(),
                                onClick = {
                                    if (enteredPin.length < 4) {
                                        val newPin = enteredPin + digit
                                        enteredPin = newPin
                                        isError = false
                                        if (newPin.length == 4) {
                                            coroutineScope.launch {
                                                val verified = viewModel.securityManager.verifyPin(newPin)
                                                if (verified) {
                                                    viewModel.unlockApp()
                                                } else {
                                                    isError = true
                                                    enteredPin = ""
                                                }
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                // Row for 0 and Backspace
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    Spacer(modifier = Modifier.size(68.dp))

                    KeypadButton(
                        text = "0",
                        onClick = {
                            if (enteredPin.length < 4) {
                                val newPin = enteredPin + "0"
                                enteredPin = newPin
                                isError = false
                                if (newPin.length == 4) {
                                    coroutineScope.launch {
                                        val verified = viewModel.securityManager.verifyPin(newPin)
                                        if (verified) {
                                            viewModel.unlockApp()
                                        } else {
                                            isError = true
                                            enteredPin = ""
                                        }
                                    }
                                }
                            }
                        }
                    )

                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceVariant)
                            .clickable {
                                if (enteredPin.isNotEmpty()) {
                                    enteredPin = enteredPin.dropLast(1)
                                    isError = false
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Backspace,
                            contentDescription = "Backspace",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(68.dp)
            .clip(CircleShape)
            .background(DarkSurfaceVariant)
            .border(1.dp, DarkSurfaceBorder, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
