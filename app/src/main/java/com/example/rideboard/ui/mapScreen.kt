package com.example.rideboard.ui

import android.R.attr.maxWidth
import android.view.MotionEvent
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.rideboard.R
import com.example.rideboard.RideViewModel
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import kotlin.math.max


@Composable
fun MapScreen(
    latitude: Double,
    longitude: Double,
    direction: Double, // -pi..pi, 0=est, pi/2=nord
    trackPoints: List<Pair<Double, Double>>,
    plannedRoute: List<GeoPoint>,
    showTrace: Boolean,
    onToggleTrace: () -> Unit,
    isToggleBlocked: Boolean,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
    onNavClick: () -> Unit = {},
    routeRemaining: RideViewModel.RouteRemaining? = null
) {
    val textMeasurer = rememberTextMeasurer()
    val context = LocalContext.current
    val isNavShown = plannedRoute.isNotEmpty()
    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.OpenTopo)  //MAPNIK
            setMultiTouchControls(true)
            controller.setZoom(14.9)

            // mapOrientation reste à sa valeur par défaut (0 = nord en haut)
        }
    }

    LaunchedEffect(isToggleBlocked) {
        mapView.setMultiTouchControls(!isToggleBlocked)
    }
    val positionMarker = remember {
        Marker(mapView).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            icon = ContextCompat.getDrawable(context, R.drawable.ic_position_arrow)
            setFlat(false) // reste orientée vers le haut de l'écran, ne suit PAS la rotation de la carte
            setInfoWindow(null) // pas de popup au clic
        }
    }

    val polyline = remember {
        Polyline(mapView).apply {
            outlinePaint.color = android.graphics.Color.BLUE
            outlinePaint.strokeWidth = 10f
        }
    }

    val routePolyline = remember {
        Polyline(mapView).apply {
            outlinePaint.color = android.graphics.Color.RED
            outlinePaint.strokeWidth = 12f
        }
    }

    DisposableEffect(mapView, positionMarker) {
        mapView.overlays.add(positionMarker)
        onDispose { mapView.overlays.remove(positionMarker) }
    }

    var autoRecenter by remember { mutableStateOf(true) }

    LaunchedEffect(latitude, longitude, direction, autoRecenter) {
        val compassBearing = mathAngleToCompassBearing(direction)

        positionMarker.position = GeoPoint(latitude, longitude)
        positionMarker.rotation = compassBearing // la flèche pointe selon le cap réel
        mapView.invalidate()

        if (autoRecenter) {
            mapView.controller.setCenter(GeoPoint(latitude, longitude))
        }
    }

    DisposableEffect(mapView, routePolyline) {
        mapView.overlays.add(0, routePolyline)   // en dessous du marqueur de position
        onDispose { mapView.overlays.remove(routePolyline) }
    }

    LaunchedEffect(plannedRoute) {
        routePolyline.setPoints(plannedRoute)
        mapView.invalidate()
    }

    LaunchedEffect(trackPoints, showTrace) {
        if (showTrace) {
            val geoPoints = trackPoints.map { GeoPoint(it.first, it.second) }
            polyline.setPoints(geoPoints)
            if (!mapView.overlays.contains(polyline)) {
                mapView.overlays.add(0, polyline) // ajoute au fond
            }
        } else {
            mapView.overlays.remove(polyline)
        }
        mapView.invalidate()
    }

    DisposableEffect(Unit) {
        onDispose { mapView.onDetach() }
    }


    DisposableEffect(mapView, isToggleBlocked) {
        val touchListener = View.OnTouchListener(fun(_: View, event: MotionEvent): Boolean {
            if (isToggleBlocked) return true // bloque tout geste simple (drag)
            if (event.action == MotionEvent.ACTION_MOVE) {
                autoRecenter = false
            }
            return false
        })
        mapView.setOnTouchListener(touchListener)
        onDispose { mapView.setOnTouchListener(null) }
    }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { mapView }
        )

        routeRemaining?.let { r ->
            val text = formatRemaining(r)
            val widthPerValuePx = maxWidth.toFloat() / max(1, text.length)
            val font = computeFontSize(
                textMeasurer = textMeasurer,
                values = listOf(text, "    "),
                availableWidthPx = widthPerValuePx,
                maxFontSize = 30.sp,
                minFontSize = 6.sp
            )
            Text(
                text = text,
                color = Color.White,
                fontSize = font,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 8.dp, bottom = bottomInset + 8.dp)
                    .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .align(Alignment.TopEnd)
                .padding(8.dp)
        ) {
            IconButton(onClick = { if (!isToggleBlocked) mapView.controller.zoomIn() }) {
                Icon(Icons.Default.Add, contentDescription = "Zoom +")
            }
            IconButton(onClick = { if (!isToggleBlocked) mapView.controller.zoomOut() }) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom -")
            }

            // Bouton de trace
            IconButton(
                onClick = onToggleTrace,
                modifier = Modifier.background(if (showTrace) Color.Blue.copy(alpha = 0.3f) else Color.Transparent, CircleShape)
            ) {
                Icon(
                    painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                    contentDescription = "Trace",
                    tint = Color.Black,
                    modifier = Modifier.size(28.dp)
                )
            }

            //bouton pour la navigation sur un itinéraire
            IconButton(
                onClick = { if (!isToggleBlocked) onNavClick() },
                modifier = Modifier
                    .background(if (isNavShown) Color.Red.copy(alpha = 0.3f) else Color.Transparent, CircleShape)
                    .align(Alignment.End)
                //.padding(end = 8.dp, bottom = 8.dp),
                //à utiliser si Button au lieu de IconButton:  contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
            ) { Text("Nav") }
        }

        if (!autoRecenter) {
            IconButton(
                onClick = { if (!isToggleBlocked) autoRecenter = true },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Recentrer")
            }
        }
    }
}

fun formatRemaining(r: RideViewModel.RouteRemaining): String {
    val km = r.meters / 1000.0
    val dist = if (km >= 100) "%.0f km".format(km) else "%.1f km".format(km)
    return "$dist · D+ ${Math.round(r.ascentM)} m"
}

fun mathAngleToCompassBearing(angleRad: Double): Float {
    val angleDeg = Math.toDegrees(angleRad)
    val bearing =  angleDeg - 90.0
    return ((bearing % 360.0 + 360.0) % 360.0).toFloat()
}