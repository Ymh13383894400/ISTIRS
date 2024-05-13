package utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import com.google.gson.Gson;

import beans.AMapGeocodingBean;
import beans.GoogleMapGeocodingBean;
import javafx.scene.image.Image;

public class GoogleMapUtil {
	private static final String GOOGLE_MAP_KEY = "AIzaSyBPUI8p3FaOVNI_MipsHG7s2ESJ8S5-cws";
	private static final String A_MAP_KEY = "045549c4d5b0b38d1ed3d52e69646705";

	// 谷歌卫星地图api
	private static final String BASE_STATIC_URL = "http://google.cn/maps/api/staticmap?maptype=satellite&language=zh-CN&sensor=false"
			+ "&key=" + GOOGLE_MAP_KEY;

	// 谷歌地理信息api.现在此api已被谷歌禁用
	private static final String BASE_GEOCODING_URL = "https://google.cn/maps/api/geocode/json?&result_type=political&key="
			+ GOOGLE_MAP_KEY + "&language=zh-CN";

	// 高德地理信息api
	private static final String AMAP_BASE_GEOCODING_URL = "https://restapi.amap.com/v3/geocode/regeo?key=" + A_MAP_KEY;

	/**
	 * 获取高德地图地理信息
	 * 
	 * @param lat
	 * @param lng
	 * @return
	 */
	public static AMapGeocodingBean getAMapGeocoding(double lat, double lng) {
		//解决非中文win10下gps读入秒数据是，而不是.。
		AMapGeocodingBean jsonBean = null;
		String urlStr = AMAP_BASE_GEOCODING_URL + "&location=" + lng + "," + lat;
		if (SaveLanguageUtil.getData() == 1) {
			urlStr = urlStr + "&language=en";
		}
		try {
			URL url = new URL(urlStr);
			HttpURLConnection openConnection = (HttpURLConnection) url.openConnection();
			int responseCode = openConnection.getResponseCode();
			if (responseCode != 200) {
				ToastUtil.toast(ResUtil.gs("flight_geo_error"));
			} else {
				InputStreamReader in = new InputStreamReader(openConnection.getInputStream(), "UTF-8");
				BufferedReader br = new BufferedReader(in);
				String line;
				StringBuffer result = new StringBuffer();
				while ((line = br.readLine()) != null) {
					result.append(line);
				}
				String jsonStr = result.toString();
				Gson gson = new Gson();
				jsonBean = gson.fromJson(jsonStr, AMapGeocodingBean.class);
				return jsonBean;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("URL Amap Geocoding:" + urlStr);
		return jsonBean;
	}

	/**
	 * 根据经纬度获取到具体地理信息
	 * 
	 * @deprecated 此api已被谷歌禁用，使用AMap的
	 */
	public static GoogleMapGeocodingBean getGeocoding(double lat, double lng) {
		GoogleMapGeocodingBean jsonBean = null;
		String urlStr = BASE_GEOCODING_URL + "&latlng=" + lat + "," + lng;
		try {
			URL url = new URL(urlStr);
			HttpURLConnection openConnection = (HttpURLConnection) url.openConnection();
			int responseCode = openConnection.getResponseCode();
			if (responseCode != 200) {
				ToastUtil.toast(ResUtil.gs("flight_geo_error"));
			} else {
				InputStreamReader in = new InputStreamReader(openConnection.getInputStream(), "UTF-8");
				BufferedReader br = new BufferedReader(in);
				String line;
				StringBuffer result = new StringBuffer();
				while ((line = br.readLine()) != null) {
					result.append(line);
				}
				String jsonStr = result.toString();
				Gson gson = new Gson();
				jsonBean = gson.fromJson(jsonStr, GoogleMapGeocodingBean.class);
				return jsonBean;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("URL Geocoding:" + urlStr);
		return jsonBean;
	}

	/**
	 * 根据数据得到谷歌地图图片
	 * 
	 * @param analyGoogleData
	 * @return
	 */
	public static Image getMapImage(ArrayList<Double> analyGoogleData) {
		String urlStr = GoogleMapUtil.generateUrl(analyGoogleData);
		System.out.println("URL map:" + urlStr);
		Image imageMap = null;
		try {
			URL url = new URL(urlStr);
			HttpURLConnection openConnection = (HttpURLConnection) url.openConnection();
			int responseCode = openConnection.getResponseCode();
			if (responseCode != 200) {
				ToastUtil.toast(ResUtil.gs("flight_map_error"));
			} else {
				InputStream is = openConnection.getInputStream();
				imageMap = new Image(is);
			}
		} catch (Exception e) {
			e.printStackTrace();
			ToastUtil.toast(ResUtil.gs("flight_map_error_no_net"));
		} 
		System.out.println("URL mapimage:" + urlStr);
		return imageMap;
	}

	/**
	 * 根据数据生成谷歌请求url
	 * 
	 * @param analyData
	 * @return
	 */
	private static String generateUrl(ArrayList<Double> analyData) {
		StringBuffer imageUrl = new StringBuffer(BASE_STATIC_URL);
		imageUrl.append("&size=" + 640 + "x" + 410);
		String marks = generateMarkAndPath(analyData);
		System.out.println(marks);
		return imageUrl + marks;
	}

	private static String generateMarkAndPath(ArrayList<Double> analyData) {
		StringBuffer marks = new StringBuffer();
		StringBuffer paths = new StringBuffer("&path=color:orange|weight:3");

		for (int i = 2; i < analyData.size(); i = i + 2) {
			double[] tra = new double[2];
			GpsCorrect.transform(analyData.get(i + 1), analyData.get(i), tra);
			if (i == 2) {
				marks.append("&markers=color:yellow|label:S|");
				marks.append(tra[0]);
				marks.append(",");
				marks.append(tra[1]);
			} else if ((i + 2) == analyData.size()) {
				marks.append("&markers=color:red|label:E|");
				marks.append(tra[0]);
				marks.append(",");
				marks.append(tra[1]);
			}

			paths.append("|");
			paths.append(tra[0]);
			paths.append(",");
			paths.append(tra[1]);
		}
		return marks.toString() + paths.toString();
	}

}
