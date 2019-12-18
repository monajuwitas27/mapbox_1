package com.monajuwitas.mapbox_sample

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncherOptions
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute
import kotlinx.android.synthetic.main.activity_main.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var permissionsManager: PermissionsManager
    private lateinit var mapboxMap: MapboxMap
    private val markers = ArrayList<Marker>()
    private lateinit var currentRoute: DirectionsRoute
    private var navigationMapRoute: NavigationMapRoute? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, getString(R.string.map_box_access_token))
        setContentView(R.layout.activity_main)
        initMapView(savedInstanceState)
        initPermissions()
        button_start_navigation.setOnClickListener {
            val navigationLauncherOptions = NavigationLauncherOptions.builder()
                .directionsRoute(currentRoute)
                .shouldSimulateRoute(true)
                .build()
            NavigationLauncher.startNavigation(this, navigationLauncherOptions)
        }
    }

    override fun onStart() {
        super.onStart()
        map_view.onStart()
    }

    override fun onResume() {
        super.onResume()
        map_view.onResume()
    }

    override fun onPause() {
        super.onPause()
        map_view.onPause()
    }

    override fun onStop() {
        super.onStop()
        map_view.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        map_view.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        map_view.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        map_view.onSaveInstanceState(outState!!)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_item_change_style -> {
                val items = arrayOf(
                    "Mapbox Street",
                    "Outdoor",
                    "Light",
                    "Dark",
                    "Satellite",
                    "Satellite Street",
                    "Traffic Day",
                    "Traffic Night"
                )
                val alertDialogChangeStyleMaps = AlertDialog.Builder(this)
                    .setItems(items) { dialog, item ->
                        when (item) {
                            0 -> {
                                mapboxMap.setStyle(Style.MAPBOX_STREETS)
                                dialog.dismiss()
                            }
                            1 -> {
                                mapboxMap.setStyle(Style.OUTDOORS)
                                dialog.dismiss()
                            }
                            2 -> {
                                mapboxMap.setStyle(Style.LIGHT)
                                dialog.dismiss()
                            }
                            3 -> {
                                mapboxMap.setStyle(Style.DARK)
                                dialog.dismiss()
                            }
                            4 -> {
                                mapboxMap.setStyle(Style.SATELLITE)
                                dialog.dismiss()
                            }
                            5 -> {
                                mapboxMap.setStyle(Style.SATELLITE_STREETS)
                                dialog.dismiss()
                            }
                            6 -> {
                                mapboxMap.setStyle(Style.TRAFFIC_DAY)
                                dialog.dismiss()
                            }
                            7 -> {
                                mapboxMap.setStyle(Style.TRAFFIC_NIGHT)
                                dialog.dismiss()
                            }
                        }
                    }
                    .setTitle(getString(R.string.change_style_maps))
                    .create()
                alertDialogChangeStyleMaps.show()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun initMapView(savedInstanceState: Bundle?) {
        map_view.onCreate(savedInstanceState)
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        this.mapboxMap.setStyle(Style.MAPBOX_STREETS) {
            showingDeviceLocation(mapboxMap)
        }
        this.mapboxMap.addOnMapClickListener {
            if (markers.size == 2) {
                mapboxMap.removeMarker(markers[1])
                markers.removeAt(1)
            }
            markers.add(
                mapboxMap.addMarker(
                    MarkerOptions().position(it)
                )
            )
            if (markers.size == 2) {
                val originPoint = Point.fromLngLat(markers[0].position.longitude, markers[0].position.latitude)
                val destinationPoint = Point.fromLngLat(markers[1].position.longitude, markers[1].position.latitude)
                NavigationRoute.builder(this)
                    .accessToken(Mapbox.getAccessToken()!!)
                    .origin(originPoint)
                    .destination(destinationPoint)
                    .voiceUnits(DirectionsCriteria.IMPERIAL)
                    .build()
                    .getRoute(object : Callback<DirectionsResponse> {
                        override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
                            Toast.makeText(this@MainActivity, "Error occured: ${t.message}", Toast.LENGTH_LONG)
                                .show()
                        }

                        override fun onResponse(call: Call<DirectionsResponse>, response: Response<DirectionsResponse>) {
                            if (response.body() == null) {
                                Toast.makeText(this@MainActivity, "No routes found, make sure you set the right user and access token.", Toast.LENGTH_LONG)
                                    .show()
                                button_start_navigation.visibility = View.GONE
                                return
                            } else if (response.body()!!.routes().size < 1) {
                                Toast.makeText(this@MainActivity, "No routes found", Toast.LENGTH_LONG)
                                    .show()
                                button_start_navigation.visibility = View.GONE
                                return
                            }
                            currentRoute = response.body()!!.routes()[0]
                            if (navigationMapRoute != null) {
                                navigationMapRoute?.removeRoute()
                            } else {
                                navigationMapRoute = NavigationMapRoute(null, map_view, mapboxMap, R.style.NavigationMapRoute)
                            }
                            navigationMapRoute?.addRoute(currentRoute)
                            button_start_navigation.visibility = View.VISIBLE
                        }
                    })
            } else {
                button_start_navigation.visibility = View.GONE
            }
            true
        }
        this.mapboxMap.setOnMarkerClickListener {
            for (marker in markers) {
                if (marker.position == it.position) {
                    markers.remove(marker)
                    mapboxMap.removeMarker(marker)
                    break
                }
            }
            true
        }
    }

    @SuppressLint("MissingPermission")
    private fun showingDeviceLocation(mapboxMap: MapboxMap) {
        val locationComponent = mapboxMap.locationComponent
        locationComponent.activateLocationComponent(this, mapboxMap.style!!)
        locationComponent.isLocationComponentEnabled = true
        locationComponent.cameraMode = CameraMode.TRACKING
        locationComponent.renderMode = RenderMode.COMPASS
    }

    private fun initPermissions() {
        val permissionListener = object : PermissionsListener {
            override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
                /* Nothing to do in here */
            }

            override fun onPermissionResult(granted: Boolean) {
                if (granted) {
                    syncMapbox()
                } else {
                    val alertDialogInfo = AlertDialog.Builder(this@MainActivity)
                        .setTitle(getString(R.string.info))
                        .setCancelable(false)
                        .setMessage(getString(R.string.permissions_denied))
                        .setPositiveButton(getString(R.string.dismiss)) { _, _ ->
                            finish()
                        }
                        .create()
                    alertDialogInfo.show()
                }
            }
        }
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            syncMapbox()
        } else {
            permissionsManager = PermissionsManager(permissionListener)
            permissionsManager.requestLocationPermissions(this)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun syncMapbox() {
        map_view.getMapAsync(this)
    }
}
