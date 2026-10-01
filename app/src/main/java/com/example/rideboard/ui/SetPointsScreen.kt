package com.example.rideboard.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.rideboard.AppSettings

@Composable
fun SetPointsScreen(
    onDone: () -> Unit
) {
    Button(
        onClick = {
            onDone()
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("OK")
    }
}