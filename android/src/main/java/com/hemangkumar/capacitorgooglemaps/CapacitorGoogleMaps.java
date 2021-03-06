package com.hemangkumar.capacitorgooglemaps;


import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.getcapacitor.JSObject;
import com.getcapacitor.NativePlugin;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.google.android.libraries.maps.CameraUpdateFactory;
import com.google.android.libraries.maps.GoogleMap;
import com.google.android.libraries.maps.GoogleMapOptions;
import com.google.android.libraries.maps.MapView;
import com.google.android.libraries.maps.OnMapReadyCallback;
import com.google.android.libraries.maps.UiSettings;
import com.google.android.libraries.maps.model.CameraPosition;
import com.google.android.libraries.maps.model.LatLng;
import com.google.android.libraries.maps.model.MapStyleOptions;
import com.google.android.libraries.maps.model.Marker;
import com.google.android.libraries.maps.model.MarkerOptions;
import com.google.android.libraries.maps.model.PointOfInterest;

import java.io.IOException;
import java.util.List;


@NativePlugin()
public class CapacitorGoogleMaps extends Plugin implements OnMapReadyCallback {

    private MapView mapView;
    GoogleMap googleMap;
    Integer mapViewParentId;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean locationPermissionGranted;
    Integer DEFAULT_WIDTH = 500;
    Integer DEFAULT_HEIGHT = 500;
    Float DEFAULT_ZOOM = 12.0f;

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        notifyListeners("onMapReady", null);
    }

    @Override
    protected void handleRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.handleRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    locationPermissionGranted = true;
                }
            }
        }

        if (locationPermissionGranted) {
            notifyListeners("onLocationPermissionGranted", null);
        }
    }

    @PluginMethod()
    public void initialize(PluginCall call) {
        /*
         *  TODO: Check location permissions and API key
         */
        call.success();
    }

    @PluginMethod()
    public void requestLocationPermission(PluginCall call) {
        ActivityCompat.requestPermissions(getBridge().getActivity(),
                new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);

        JSObject result = new JSObject();
        result.put("locationPermissionRequested", true);

        call.resolve(result);
    }

    @PluginMethod()
    public void hasLocationPermission(PluginCall call) {
        Context ctx = getBridge().getContext();

        if (ContextCompat.checkSelfPermission(ctx,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
        } else {
            locationPermissionGranted = false;
        }

        JSObject result = new JSObject();
        result.put("locationPermissionGranted", locationPermissionGranted);

        call.resolve(result);
    }

    @PluginMethod()
    public void create(PluginCall call) {
        final Integer width = call.getInt("width", DEFAULT_WIDTH);
        final Integer height = call.getInt("height", DEFAULT_HEIGHT);
        final Integer x = call.getInt("x", 0);
        final Integer y = call.getInt("y", 0);

        final Float zoom = call.getFloat("zoom", DEFAULT_ZOOM);
        final Double latitude = call.getDouble("latitude");
        final Double longitude = call.getDouble("longitude");

        final boolean liteMode = call.getBoolean("enabled", false);

        final CapacitorGoogleMaps ctx = this;
        getBridge().getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                LatLng latLng = new LatLng(latitude, longitude);
                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(latLng)
                        .zoom(zoom)
                        .build();

                GoogleMapOptions googleMapOptions = new GoogleMapOptions();
                googleMapOptions.camera(cameraPosition);
                googleMapOptions.liteMode(liteMode);

                FrameLayout mapViewParent = new FrameLayout(getBridge().getContext());
                mapViewParentId = View.generateViewId();
                mapViewParent.setId(mapViewParentId);

                mapView = new MapView(getBridge().getContext(), googleMapOptions);

                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(getScaledPixels(width), getScaledPixels(height));
                lp.topMargin = getScaledPixels(y);
                lp.leftMargin = getScaledPixels(x);

                mapView.setLayoutParams(lp);

                mapViewParent.addView(mapView);

                ((ViewGroup) getBridge().getWebView().getParent()).addView(mapViewParent);

                mapView.onCreate(null);
                mapView.getMapAsync(ctx);
            }
        });
        call.success();
    }

    @PluginMethod()
    public void addMarker(PluginCall call) {
        final Double latitude = call.getDouble("latitude", 0d);
        final Double longitude = call.getDouble("longitude", 0d);
        final Float opacity = call.getFloat("opacity", 1.0f);
        final String title = call.getString("title", "");
        final String snippet = call.getString("snippet", "");
        final Boolean isFlat = call.getBoolean("isFlat", true);

        getBridge().getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                LatLng latLng = new LatLng(latitude, longitude);
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(latLng);
                markerOptions.alpha(opacity);
                markerOptions.title(title);
                markerOptions.snippet(snippet);
                markerOptions.flat(isFlat);
                googleMap.addMarker(markerOptions);
            }
        });

        call.resolve();
    }

    @PluginMethod()
    public void setMapType(PluginCall call) {

        String specifiedMapType = call.getString("type", "normal");
        Integer mapType;

        switch (specifiedMapType) {
            case "hybrid":
                mapType = GoogleMap.MAP_TYPE_HYBRID;
                break;
            case "satellite":
                mapType = GoogleMap.MAP_TYPE_SATELLITE;
                break;
            case "terrain":
                mapType = GoogleMap.MAP_TYPE_TERRAIN;
                break;
            case "none":
                mapType = GoogleMap.MAP_TYPE_NONE;
                break;
            default:
                mapType = GoogleMap.MAP_TYPE_NORMAL;
        }

        final Integer selectedMapType = mapType;
        getBridge().getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                googleMap.setMapType(selectedMapType);
            }

        });

        call.success();
    }

    @PluginMethod()
    public void setIndoorEnabled(PluginCall call) {
        final Boolean indoorEnabled = call.getBoolean("enabled", false);
        getBridge().executeOnMainThread(new Runnable() {
            @Override
            public void run() {
                googleMap.setIndoorEnabled(indoorEnabled);
            }
        });

        call.success();
    }

    @PluginMethod()
    public void setTrafficEnabled(PluginCall call) {
        final Boolean trafficEnabled = call.getBoolean("enabled", false);
        getBridge().executeOnMainThread(new Runnable() {
            @Override
            public void run() {
                googleMap.setTrafficEnabled(trafficEnabled);
            }
        });

        call.success();
    }

    @PluginMethod()
    public void padding(PluginCall call) {
        final Integer top = call.getInt("top", 0);
        final Integer left = call.getInt("left", 0);
        final Integer bottom = call.getInt("bottom", 0);
        final Integer right = call.getInt("right", 0);

        getBridge().executeOnMainThread(new Runnable() {
            @Override
            public void run() {
                googleMap.setPadding(left, top, right, bottom);
            }
        });

        call.success();
    }

    @PluginMethod()
    public void clear(PluginCall call) {
        getBridge().executeOnMainThread(new Runnable() {
            @Override
            public void run() {
                googleMap.clear();
            }
        });

        call.success();
    }

    @PluginMethod()
    public void close(PluginCall call) {
        getBridge().executeOnMainThread(new Runnable() {
            @Override
            public void run() {
                View viewToRemove = ((ViewGroup) getBridge().getWebView().getParent()).findViewById(mapViewParentId);
                ((ViewGroup) getBridge().getWebView().getParent()).removeViewInLayout(viewToRemove);
            }
        });
    }

    @PluginMethod()
    public void reverseGeocodeCoordinate(final PluginCall call) {
        final Double latitude = call.getDouble("latitude", 0.0);
        final Double longitude = call.getDouble("longitude", 0.0);

        getBridge().executeOnMainThread(new Runnable() {
            @Override
            public void run() {
                /*
                 * TODO: Check if can be done without adding Places SDK
                 *
                 */

                Geocoder geocoder = new Geocoder(getContext());
                try {
                    List<Address> addressList = geocoder.getFromLocation(latitude, longitude, 5);

                    JSObject results = new JSObject();

                    int index = 0;
                    for (Address address : addressList) {
                        JSObject addressObject = new JSObject();
                        addressObject.put("administrativeArea", address.getAdminArea());
                        addressObject.put("lines", address.getAddressLine(0));
                        addressObject.put("country", address.getCountryName());
                        addressObject.put("locality", address.getLocality());
                        addressObject.put("subLocality", address.getSubLocality());
                        addressObject.put("thoroughFare", address.getThoroughfare());

                        results.put(String.valueOf(index++), addressObject);
                    }
                    call.success(results);
                } catch (IOException e) {
                    call.error("Error in Geocode!");
                }
            }
        });
    }

    @PluginMethod()
    public void settings(final PluginCall call) {

        final boolean allowScrollGesturesDuringRotateOrZoom = call.getBoolean("allowScrollGesturesDuringRotateOrZoom", true);

        final boolean compassButton = call.getBoolean("compassButton", false);
        final boolean zoomButton = call.getBoolean("zoomButton", true);
        final boolean myLocationButton = call.getBoolean("myLocationButton", false);

        boolean consumesGesturesInView = call.getBoolean("consumesGesturesInView", true);
        final boolean indoorPicker = call.getBoolean("indoorPicker", false);

        final boolean rotateGestures = call.getBoolean("rotateGestures", true);
        final boolean scrollGestures = call.getBoolean("scrollGestures", true);
        final boolean tiltGestures = call.getBoolean("tiltGestures", true);
        final boolean zoomGestures = call.getBoolean("zoomGestures", true);

        getBridge().executeOnMainThread(new Runnable() {
            @Override
            public void run() {
                UiSettings googleMapUISettings = googleMap.getUiSettings();
                googleMapUISettings.setScrollGesturesEnabledDuringRotateOrZoom(allowScrollGesturesDuringRotateOrZoom);
                googleMapUISettings.setCompassEnabled(compassButton);
                googleMapUISettings.setIndoorLevelPickerEnabled(indoorPicker);
                googleMapUISettings.setMyLocationButtonEnabled(myLocationButton);
                googleMapUISettings.setRotateGesturesEnabled(rotateGestures);
                googleMapUISettings.setScrollGesturesEnabled(scrollGestures);
                googleMapUISettings.setTiltGesturesEnabled(tiltGestures);
                googleMapUISettings.setZoomGesturesEnabled(zoomGestures);
                googleMapUISettings.setZoomControlsEnabled(zoomButton);
                googleMapUISettings.setMyLocationButtonEnabled(true);
            }
        });

        call.resolve();
    }

    @PluginMethod()
    public void setCamera(PluginCall call) {

        final float viewingAngle = call.getFloat("viewingAngle", googleMap.getCameraPosition().tilt);
        final float bearing = call.getFloat("bearing", googleMap.getCameraPosition().bearing);
        final Float zoom = call.getFloat("zoom", googleMap.getCameraPosition().zoom);
        final Double latitude = call.getDouble("latitude", googleMap.getCameraPosition().target.latitude);
        final Double longitude = call.getDouble("longitude", googleMap.getCameraPosition().target.longitude);

        final Boolean animate = call.getBoolean("animate", false);
        Double animationDuration = call.getDouble("animationDuration", 1000.0);

        getBridge().executeOnMainThread(new Runnable() {
            @Override
            public void run() {
                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(new LatLng(latitude, longitude))
                        .zoom(zoom)
                        .tilt(viewingAngle)
                        .bearing(bearing)
                        .build();

                if (animate) {
                    /*
                     * TODO: Implement animationDuration
                     * */
                    googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                } else {
                    googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                }
            }
        });

        call.resolve();
    }

    @PluginMethod()
    public void setMapStyle(PluginCall call) {
        /*
            https://mapstyle.withgoogle.com/
        */
        final String mapStyle = call.getString("jsonString", "");

        getBridge().executeOnMainThread(new Runnable() {
            @Override
            public void run() {
                MapStyleOptions mapStyleOptions = new MapStyleOptions(mapStyle);
                googleMap.setMapStyle(mapStyleOptions);
            }
        });
    }

    @PluginMethod()
    public void setOnMyLocationButtonClickListener(PluginCall call) {
        final CapacitorGoogleMaps ctx = this;

        getBridge().executeOnMainThread(new Runnable() {
            @Override
            public void run() {
                googleMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
                    @Override
                    public boolean onMyLocationButtonClick() {
                        ctx.onMyLocationButtonClick();
                        return false;
                    }
                });
            }
        });

        call.resolve();
    }

    @PluginMethod()
    public void setOnMyLocationClickListener(PluginCall call) {
        final CapacitorGoogleMaps ctx = this;
        getBridge().executeOnMainThread(new Runnable() {
            @Override
            public void run() {
                googleMap.setOnMyLocationClickListener(new GoogleMap.OnMyLocationClickListener() {
                    @Override
                    public void onMyLocationClick(@NonNull Location location) {
                        ctx.onMyLocationClick(location);
                    }
                });
            }
        });

        call.resolve();
    }

    @PluginMethod()
    public void setOnMarkerClickListener(PluginCall call) {

        final CapacitorGoogleMaps ctx = this;

        getBridge().executeOnMainThread(new Runnable() {
            @Override
            public void run() {
                googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                    @Override
                    public boolean onMarkerClick(Marker marker) {
                        ctx.onMarkerClick(marker);
                        return false;
                    }
                });
            }
        });

        call.resolve();
    }

    @PluginMethod()
    public void setOnPoiClickListener(PluginCall call) {

        final CapacitorGoogleMaps ctx = this;

        getBridge().executeOnMainThread(new Runnable() {
            @Override
            public void run() {
                googleMap.setOnPoiClickListener(new GoogleMap.OnPoiClickListener() {
                    @Override
                    public void onPoiClick(PointOfInterest pointOfInterest) {
                        ctx.onPoiClick(pointOfInterest);
                    }
                });
            }
        });

        call.resolve();
    }

    @PluginMethod()
    public void setOnMapClickListener(PluginCall call) {

        final CapacitorGoogleMaps ctx = this;

        getBridge().executeOnMainThread(new Runnable() {
            @Override
            public void run() {
                googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                    @Override
                    public void onMapClick(LatLng latLng) {
                        ctx.onMapClick(latLng);
                    }
                });
            }
        });

        call.resolve();
    }

    @PluginMethod()
    public void enableCurrentLocation(final PluginCall call) {

        final boolean enableLocation = call.getBoolean("enabled", false);

        getBridge().executeOnMainThread(new Runnable() {
            @Override
            public void run() {
                if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    call.error("Permission for location not granted");
                } else {
                    googleMap.setMyLocationEnabled(enableLocation);
                    call.resolve();
                }
            }
        });
    }

    public void onPoiClick(PointOfInterest pointOfInterest) {
        JSObject result = new JSObject();
        JSObject location = new JSObject();

        location.put("latitude", pointOfInterest.latLng.latitude);
        location.put("longitude", pointOfInterest.latLng.longitude);

        result.put("name", pointOfInterest.name);
        result.put("placeID", pointOfInterest.placeId);
        result.put("location", location);

        notifyListeners("didTapPOIWithPlaceID", result);
    }

    public void onMapClick(LatLng latLng) {
        JSObject location = new JSObject();

        location.put("latitude", latLng.latitude);
        location.put("longitude", latLng.longitude);

        notifyListeners("didTapAt", location);
    }

    public void onMarkerClick(Marker marker) {
        JSObject result = new JSObject();
        JSObject location = new JSObject();

        location.put("latitude", marker.getPosition().latitude);
        location.put("longitude", marker.getPosition().longitude);

        result.put("name", marker.getTitle());
        result.put("snippet", marker.getSnippet());
        result.put("location", location);

        notifyListeners("didTap", result);
    }

    public void onMyLocationButtonClick() {
        /*
         *  TODO: Add handler
         */
    }

    public void onMyLocationClick(Location location) {
        JSObject result = new JSObject();

        result.put("latitude", location.getLatitude());
        result.put("longitude", location.getLongitude());

        notifyListeners("onMyLocationClick", result);
    }

    public int getScaledPixels(float pixels) {
        // Get the screen's density scale
        final float scale = getBridge().getActivity().getResources().getDisplayMetrics().density;
        // Convert the dps to pixels, based on density scale
        return (int) (pixels * scale + 0.5f);
    }

}
