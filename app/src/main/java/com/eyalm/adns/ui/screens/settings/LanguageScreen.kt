package com.eyalm.adns.ui.screens.settings

import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.eyalm.adns.R
import com.eyalm.adns.data.LocaleHelper
import com.eyalm.adns.ui.components.ExpressiveListItem
import com.eyalm.adns.ui.screens.SettingsCategoryScreenTemplate

// TODO: Move to a dynamic approach

@Composable
fun LanguageScreen(onBack: () -> Unit) {
    SettingsCategoryScreenTemplate(
        onBack = onBack,
        title = stringResource(R.string.language),
        description = stringResource(R.string.language_description)
    ) {
        val context = LocalContext.current
        val currentLang = remember { LocaleHelper.getLanguage(context) }
        LazyColumn(
            contentPadding = PaddingValues(4.dp)
        ) {
            item {
                ExpressiveListItem(
                    title = "English",
                    onClick = {
                        LocaleHelper.setLocale(context, "en")
                        (context as? Activity)?.recreate()
                    },
                    isFirst = true,
                    isSelected = currentLang == "en",
                    altLeadingContent = {
                        RadioButton(
                            currentLang == "en",
                            onClick = { },
                        )
                    },
                )
            }

            item {
                ExpressiveListItem(
                    title = "עברית",
                    onClick = {
                        LocaleHelper.setLocale(context, "iw")
                        (context as? Activity)?.recreate()
                    },
                    isLast = true,
                    isSelected = currentLang == "iw",
                    altLeadingContent = {
                        RadioButton(
                            currentLang == "iw",
                            onClick = { },
                        )
                    },
                )
            }
        }

    }
}