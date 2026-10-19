package it.matato.dietreminder.ui.screens.account

import it.matato.dietreminder.common.model.PasswordRules

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.matato.dietreminder.R
import it.matato.dietreminder.ui.icons.AppIcons as Icons

@Composable
fun PasswordCriteria(password: String, confirmation: String, currentPassword: String? = null) {
    val checks=listOf(R.string.password_minimum to (password.length>=12),R.string.password_maximum to (password.isNotEmpty()&&password.length<=128),R.string.password_uppercase to PasswordRules.hasUppercase(password),R.string.password_lowercase to PasswordRules.hasLowercase(password),R.string.password_special to PasswordRules.hasSpecial(password),R.string.password_confirmation_matches to (password.isNotEmpty()&&password==confirmation)) + if(currentPassword==null) emptyList() else listOf(R.string.password_different to (password.isNotEmpty()&&password!=currentPassword))
    Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
        checks.forEach { (text,met) -> Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            val color=if(met) if(MaterialTheme.colorScheme.surface.luminance() < 0.5f) Color(0xFF91E7A6) else Color(0xFF246A42) else MaterialTheme.colorScheme.onSurfaceVariant
            Icon(Icons.Rounded.CheckCircle,null,Modifier.size(16.dp),tint=color)
            Text(stringResource(text),style=MaterialTheme.typography.bodySmall,color=color)
        } }
    }
}
