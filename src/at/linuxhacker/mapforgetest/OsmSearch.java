package at.linuxhacker.mapforgetest;

import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.URLEncoder;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mapsforge.android.maps.overlay.ArrayItemizedOverlay;
import org.mapsforge.android.maps.overlay.OverlayItem;
import org.mapsforge.core.GeoPoint;


import android.preference.PreferenceActivity.Header;
import at.linuxhacker.mapforgetest.MainActivity.OsmSearchArrayItemOverlay;

public class OsmSearch {
	
	private String searchString = null;
	private ArrayItemizedOverlay overlay = null;
	private String jsonResult = null;
	private static final String searchUrl = "http://nominatim.openstreetmap.org/search?q=%s&format=json&addressdetails=0&limit=30";
	
	public OsmSearch( String searchString, ArrayItemizedOverlay overlay ) {
		this.searchString = searchString;
		this.overlay = overlay;
	}

	public ArrayItemizedOverlay getOverlay( ) {
		return this.overlay;
	}
	
	public void doSearch( ) {
		this.makeHttpRequest( );
		this.fillOverlayWithResult( );
	}

	private void fillOverlayWithResult() {
		try {
			JSONArray jsonArray = new JSONArray(this.jsonResult);
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject jsonObject = jsonArray.getJSONObject(i);
				String lat = jsonObject.getString("lat");
				String lon = jsonObject.getString("lon");
				String displayName = jsonObject.getString("display_name");

				GeoPoint geoPoint = new GeoPoint( Double.parseDouble( lat ), Double.parseDouble( lon ) );
				OverlayItem item = new OverlayItem( geoPoint, displayName, "" );
				this.overlay.addItem( item );
			}
		} catch (Exception e) {
			e.printStackTrace( );
		}
	}

	private void makeHttpRequest() {
		this.jsonResult = null;
		HttpClient client = new DefaultHttpClient( );
		String concretUrl = String.format( OsmSearch.searchUrl, this.searchString );


		try {
			URI uri = new URI (
					"http",
					"nominatim.openstreetmap.org",
					"/search",
					String.format( "q=%s&format=json&addressdetails=0&limit=30", this.searchString ),
					null
					);
			String encodedUrl = uri.toASCIIString( );
			HttpGet httpGet = new HttpGet( encodedUrl );
			HttpResponse response = client.execute(httpGet);
			StatusLine statusLine = response.getStatusLine();
			if (statusLine.getStatusCode() == 200) {
				HttpEntity entity = response.getEntity();
				InputStream content = entity.getContent();
				StringWriter writer = new StringWriter();
				String encoding = EntityUtils.getContentCharSet(entity);
				IOUtils.copy(content, writer, encoding );
				jsonResult = writer.toString();
			}
		} catch (Exception e) {
			e.printStackTrace( );
		}
	}
}
