/**
 * This work was created by participants in the DataONE project, and is
 * jointly copyrighted by participating institutions in DataONE. For 
 * more information on DataONE, see our web site at http://dataone.org.
 *
 *   Copyright ${year}
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 * 
 * $Id$
 */

package org.dataone.cn.indexer.convert;

import ch.hsr.geohash.*;

/**
 * Convert latitude, longitude coordinates of a rectangle to the geohash of the
 * center of the rectangle. The rectangle is specified as longmin, latmin,
 * longmax, latmax. If a single point is specified, then it will be latitude,
 * longitude
 */

public class GeohashConverter implements IConverter {

	// Default geohash length
	private int length = 9;

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	/**
	 * @param latlong
	 *            coords of a bounding box (longmin, lotmin, longmax, latmax) or a single point (lat,
	 *            long)
	 * @return geohash string for the center of the bounding box or the
	 *         specifice point
	 */
	public String convert(String latlong) {

		BoundingBox bbox = null;
		String geohash = null;
		double geohashLat = 0;
		double geohashLong = 0;

		// This will be either the center point of the input bounding box, or
		// the
		// lat, long of an input point.
		WGS84Point centerPoint = null;

		// Parse command line for either bounding coords (west,south,east,north)
		// or single point coords (lat, long)
		String[] coords = latlong.split(" ");

		if (coords.length == 2) {
			geohashLat = Double.parseDouble(coords[0]);
			geohashLong = Double.parseDouble(coords[1]);
		} else if (coords.length == 4) {
			// Input string is west, south, east, north
			double northCoord = Double.parseDouble(coords[0]);
			double southCoord = Double.parseDouble(coords[1]);
			double eastCoord = Double.parseDouble(coords[2]);
			double westCoord = Double.parseDouble(coords[3]);

			// In some cases the the lat and long values for the bounding coords
			// can
			// be equal, if it is intended to use four coords to specify a
			// single point,
			// i.e. west = -119.1234 south=34.5678 east = -119.1234
			// north=34.5678
			if (westCoord == eastCoord || southCoord == northCoord) {
				geohashLat = southCoord;
				geohashLong = westCoord;
			} else {
				// Geohash library has a different ordering of bbox coords
				bbox = new BoundingBox(southCoord, northCoord, westCoord,
						eastCoord);
				centerPoint = bbox.getCenterPoint();
				geohashLat = centerPoint.getLatitude();
				geohashLong = centerPoint.getLongitude();
			}
		} else {
			throw new IllegalArgumentException(
					"Invalid argument for latititude, longitude (or bounding box): "
							+ latlong);
		}

		try {
			geohash = GeoHash.withCharacterPrecision(geohashLat, geohashLong,
					length).toBase32();
		} catch (IllegalArgumentException iae) {
			return null;
		}

		return geohash;

	}

	// public String[] getLatLongMidpoint(double lat1, double lon1, double lat2,
	// double lon2) {
	//
	// double dLon = Math.toRadians(lon2 - lon1);
	//
	// // convert to radians
	// lat1 = Math.toRadians(lat1);
	// lat2 = Math.toRadians(lat2);
	// lon1 = Math.toRadians(lon1);
	//
	// double Bx = Math.cos(lat2) * Math.cos(dLon);
	// double By = Math.cos(lat2) * Math.sin(dLon);
	// double lat3 = Math.atan2(
	// Math.sin(lat1) + Math.sin(lat2),
	// Math.sqrt((Math.cos(lat1) + Bx) * (Math.cos(lat1) + Bx) + By
	// * By));
	// double lon3 = lon1 + Math.atan2(By, Math.cos(lat1) + Bx);
	//
	// // print out in degrees
	// // System.out.println(Math.toDegrees(lat3) + " " +
	// // Math.toDegrees(lon3));
	// return new String[] { String.format("%7.4f", Math.toDegrees(lat3)),
	// String.format("%7.4f", Math.toDegrees(lon3)) };
	//
	// }
}