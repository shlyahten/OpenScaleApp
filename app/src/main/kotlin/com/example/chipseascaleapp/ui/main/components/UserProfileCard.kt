package com.example.chipseascaleapp.ui.main.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.chipseascaleapp.model.Gender
import com.example.chipseascaleapp.model.UserProfile
import kotlin.math.roundToInt

@Composable
fun UserProfileCard(
    profile: UserProfile,
    onSaveProfile: (UserProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    var isEditing by remember { mutableStateOf(false) }

    var selectedGender by remember(profile, isEditing) { mutableStateOf(profile.gender) }
    var heightCm by remember(profile, isEditing) { mutableFloatStateOf(profile.heightCm) }
    var ageYears by remember(profile, isEditing) { mutableIntStateOf(profile.ageYears) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Профиль пользователя",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!isEditing) {
                    Text(
                        text = "Изменить",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { isEditing = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (!isEditing) {
                // Summary chips / text
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    ProfileBadge(
                        label = "Пол",
                        value = if (profile.gender == Gender.MALE) "Мужской" else "Женский"
                    )
                    ProfileBadge(
                        label = "Рост",
                        value = "${profile.heightCm.toInt()} см"
                    )
                    ProfileBadge(
                        label = "Возраст",
                        value = "${profile.ageYears} лет"
                    )
                }
            } else {
                // Expanded Edit Form
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Gender selector
                    Text(
                        text = "Пол:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FilterChip(
                            selected = selectedGender == Gender.MALE,
                            onClick = { selectedGender = Gender.MALE },
                            label = { Text("Мужской") }
                        )
                        FilterChip(
                            selected = selectedGender == Gender.FEMALE,
                            onClick = { selectedGender = Gender.FEMALE },
                            label = { Text("Женский") }
                        )
                    }

                    // Height selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Рост: ${heightCm.toInt()} см",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Slider(
                        value = heightCm,
                        onValueChange = { heightCm = it.roundToInt().toFloat() },
                        valueRange = 120f..220f,
                        steps = 99
                    )

                    // Age selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Возраст: $ageYears лет",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Slider(
                        value = ageYears.toFloat(),
                        onValueChange = { ageYears = it.roundToInt() },
                        valueRange = 12f..100f,
                        steps = 87
                    )

                    // Save / Cancel buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(onClick = { isEditing = false }) {
                            Text("Отмена")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onSaveProfile(
                                    UserProfile(
                                        gender = selectedGender,
                                        heightCm = heightCm,
                                        ageYears = ageYears
                                    )
                                )
                                isEditing = false
                            }
                        ) {
                            Text("Сохранить")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileBadge(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
