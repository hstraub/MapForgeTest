package at.linuxhacker.mapforgetest;

import java.io.File;

import android.R.bool;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.view.Menu;
import android.widget.SearchView;
import android.widget.Toast;
import android.widget.SearchView.OnQueryTextListener;

import org.mapsforge.android.maps.MapActivity;
import org.mapsforge.android.maps.MapController;
import org.mapsforge.android.maps.MapScaleBar;
import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.overlay.ArrayCircleOverlay;
import org.mapsforge.android.maps.overlay.ArrayItemizedOverlay;
import org.mapsforge.android.maps.overlay.CircleOverlay;
import org.mapsforge.android.maps.overlay.ItemizedOverlay;
import org.mapsforge.android.maps.overlay.OverlayCircle;
import org.mapsforge.android.maps.overlay.OverlayItem;
import org.mapsforge.core.GeoPoint;

// Buffer not enough mit originaler Library
// siehe: https://groups.google.com/forum/?fromgroups=#!topic/mapsforge-dev/cGztCDPjwkI


public class MainActivity extends MapActivity implements LocationListener, OnQueryTextListener {
	
	/*
	 * Is a given point outside the mapview?
	 *   -> https://groups.google.com/forum/?fromgroups=#!topic/mapsforge-dev/8qnu09OI90E
	 *   
	 * class=highway siehe http://wiki.openstreetmap.org/wiki/Highway_tag_usage
	 * class=railway siehe http://wiki.openstreetmap.org/wiki/Railway
	 * and the whole description:
	 *   http://wiki.openstreetmap.org/wiki/DE:Map_Features
	 *   
	 *   Suche:
	 *   http://wiki.openstreetmap.org/wiki/Nominatim
	 *   http://nominatim.openstreetmap.org/search?q=museum,%20e%C3%9Fling,wien&format=xml&addressdetails=1&limit=30
	 */

	private MapView mapView = null;
	private MapController mapController = null;
	private LocationManager locationManager = null;
	private ArrayCircleOverlay circleOverlay = null;
	ArrayItemizedOverlay itemizedOverlay = null;
	ArrayItemizedOverlay osmSearchOverlay = null;
	private OverlayCircle overlayCircle = null;
	private OverlayItem overlayItem = null;
	private Paint circleOverlayFill;
	private Paint circleOverlayOutline;
	private boolean centerAtFirstFix = true;
	private SearchView searchView = null;
	
	
	public class OsmSearchArrayItemOverlay extends ArrayItemizedOverlay {
		private Context context;
		
		OsmSearchArrayItemOverlay( Drawable defaultMarker, Context context ) {
			super( defaultMarker );
			this.context = context;
		}

		@Override
		protected boolean onTap(int index) {
			OverlayItem item = createItem( index );
			if ( item != null ) {
				Builder builder = new AlertDialog.Builder( this.context );
				builder.setIcon( android.R.drawable.ic_menu_info_details );
				builder.setTitle( item.getTitle( ) );
				builder.setMessage( item.getSnippet( ) );
				builder.setPositiveButton( "OK", null );
				builder.show( );
			}
			return true;
		}
		
	}
	
	private class AsyncOSMSearch  extends AsyncTask<OsmSearch, Void, OsmSearch> {

		@Override
		protected OsmSearch doInBackground(OsmSearch... params) {
			OsmSearch osmSearch = params[0];
			
			osmSearch.doSearch( );
			
			return osmSearch;
		}

		@Override
		protected void onPostExecute(OsmSearch result) {
			if ( MainActivity.this.osmSearchOverlay != null ) {
				MainActivity.this.mapView.getOverlays( ).remove( MainActivity.this.osmSearchOverlay );
			}
			MainActivity.this.osmSearchOverlay = result.getOverlay( );
			MainActivity.this.mapView.getOverlays( ).add( MainActivity.this.osmSearchOverlay );
		}
		
	}
	
	@SuppressLint("SdCardPath")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.mapView = new MapView( this );
		this.mapView.setClickable( true );
		this.mapView.setBuiltInZoomControls( true );
		File mapFile = new File( "/sdcard/Maps/Austria/Austria.map" );
		this.mapView.setMapFile( mapFile );
		this.mapController = this.mapView.getController( );
		this.mapView.getMapMover( ).setMoveSpeedFactor( 10f );
		
		MapScaleBar mapScaleBar = this.mapView.getMapScaleBar( );
		mapScaleBar.setShowMapScaleBar( true );
		
		this.mapView.setTextScale( 1.8f );
		
		setupMyLocation( );
		
		setContentView( this.mapView );
	}

	private void setupMyLocation() {
		
		this.locationManager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );
		Criteria criteria = new Criteria( );
		criteria.setAccuracy( Criteria.ACCURACY_FINE );
		String bestProvider = this.locationManager.getBestProvider( criteria, false );
		if ( bestProvider == null ) {
			return;
		}
		
		this.locationManager.requestLocationUpdates( bestProvider, 400, 1, this );
		
		this.circleOverlayFill = new Paint( Paint.ANTI_ALIAS_FLAG );
		this.circleOverlayFill.setStyle( Paint.Style.FILL );
		this.circleOverlayFill.setColor( Color.BLUE );
		this.circleOverlayFill.setAlpha( 48 );
		
		this.circleOverlayOutline = new Paint( Paint.ANTI_ALIAS_FLAG );
		this.circleOverlayOutline.setStyle( Paint.Style.STROKE );
		this.circleOverlayOutline.setColor( Color.BLUE );
		this.circleOverlayOutline.setAlpha( 128 );
		this.circleOverlayOutline.setStrokeWidth( 2 );
		
		this.circleOverlay = new ArrayCircleOverlay( this.circleOverlayFill, this.circleOverlayOutline );
		this.overlayCircle = new OverlayCircle( );
		this.circleOverlay.addCircle( this.overlayCircle );
		this.mapView.getOverlays( ).add( this.circleOverlay );
		
		this.itemizedOverlay = new ArrayItemizedOverlay( null );
		this.overlayItem = new OverlayItem( );
		this.overlayItem.setMarker( ItemizedOverlay.boundCenter( getResources( ).getDrawable( R.drawable.ic_launcher ) ) );
		this.overlayItem.setTitle( "Test" );
		this.overlayItem.setSnippet( "snippet" );
		this.itemizedOverlay.addItem( this.overlayItem );
		this.mapView.getOverlays( ).add( this.itemizedOverlay );
		
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		
		this.searchView = (SearchView) menu.findItem( R.id.action_search ).getActionView( );
		this.searchView.setOnQueryTextListener( this );
		return true;
	}

	@Override
	public void onLocationChanged(Location location) {
		GeoPoint point = new GeoPoint( location.getLatitude( ), location.getLongitude( ) );
		this.overlayCircle.setCircleData( point, location.getAccuracy( ) );
		this.overlayItem.setPoint( point );
		this.circleOverlay.requestRedraw( );
		this.itemizedOverlay.requestRedraw( );
		
		if ( this.centerAtFirstFix ) {
			this.centerAtFirstFix = false;
			this.mapController.setCenter( point );
		}
		
	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean onQueryTextChange(String newText) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onQueryTextSubmit(String query) {
		//Toast.makeText(this, "jetzt haben wir es: " + query, Toast.LENGTH_LONG ).show( );
		//this.searchView.setIconified( true );
		//this.searchView.clearFocus();
		//this.searchView.setIconified( true ); // Really necessary!
		
		Drawable defaultMarker = getResources( ).getDrawable( R.drawable.marker_red );
		ArrayItemizedOverlay searchOverlay = new OsmSearchArrayItemOverlay( defaultMarker, this );
		OsmSearch osmSearch = new OsmSearch( query, searchOverlay );
		
		new AsyncOSMSearch( ).execute( osmSearch );
		
		return false;
	}

}
