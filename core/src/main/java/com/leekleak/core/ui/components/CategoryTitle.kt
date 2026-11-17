package com.leekleak.core.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leekleak.core.R
import com.leekleak.core.ui.navigation.Navigator

fun LazyListScope.categoryTitle(text: @Composable (() -> String)){
    item { CategoryTitleText(text()) }
}

@Composable
fun CategoryTitleText(text: String, navigator: Navigator? = null) {
    Row (modifier = Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically){
        navigator?.let {
            IconButton(onClick = { it.goBack() }) {
                Icon(
                    painter = painterResource(R.drawable.arrow_back),
                    contentDescription = stringResource(R.string.go_back),
                )
            }
        }
        Text(
            modifier = Modifier.padding(horizontal = 8.dp),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            text = text
        )
    }
}

fun LazyListScope.categoryTitleSmall(text: @Composable (() -> String)) = item { CategoryTitleSmallText(text()) }

@Composable
fun CategoryTitleSmallText(text: String) {
    Text(
        modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp),
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = colorScheme.tertiary
    )
}
