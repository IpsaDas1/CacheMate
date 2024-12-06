package com.ipsa.ipscache

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ipsa.ipscache.cache.ImageCache
import com.ipsa.ipscache.ui.theme.IpsCacheTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IpsCacheTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CachedDrawableScreen()
                }
            }
        }
    }
}

@Composable
fun CachedDrawableScreen() {
    val context = LocalContext.current

    CachedDrawableImage(
        context = context,
        resId = R.drawable.image,
        modifier = Modifier
            .size(150.dp)
            .clip(CircleShape)
    )
}


@Composable
fun CachedDrawableImage(context: Context, resId: Int, modifier: Modifier = Modifier) {
    val bitmap = produceState<Bitmap?>(null) {
        value = ImageCache.getInstance(context).loadDrawableImage(context, resId)
    }

    bitmap.value?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = null,
            modifier = modifier
        )
    } ?: Box(modifier = modifier.background(Color.Gray)) // Placeholder
}


@Composable
fun CachedURLScreen() {
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CachedNetworkImage(
            context = context,
            url = "https://picsum.photos/id/237/200/300",
            modifier = Modifier
                .size(150.dp)
                .clip(CircleShape)
        ) {
            // Optional custom placeholder
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .background(Color.LightGray)
            )
        }
    }
}

@Composable
fun CachedNetworkImage(
    context: Context,
    url: String,
    modifier: Modifier = Modifier,
    placeholder: @Composable (() -> Unit)? = null
) {
    // State to hold the loaded bitmap
    val bitmapState = produceState<Bitmap?>(null, url) {
        val imageCache = ImageCache.getInstance(context)
        value = imageCache.loadImage(url) // Load image using Coroutines
    }

    // Display the image or a placeholder
    if (bitmapState.value != null) {
        Image(
            bitmap = bitmapState.value!!.asImageBitmap(),
            contentDescription = null,
            modifier = modifier
        )
    } else {
        placeholder?.invoke() ?: Box(
            modifier = modifier.background(Color.Gray) // Default placeholder
        )
    }
}


