package utils;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.GpsDirectory;

import beans.ImageBean;

public class ImageUtil {

	/**
	 * 读取照片里面的信息
	 * 
	 * @throws IOException
	 * @throws ImageProcessingException
	 */
	public static void printImageTags(String imgPath, ImageBean imageBean)
			throws ImageProcessingException, IOException {
		File img = new File(imgPath);
		if (img == null || !img.exists() || img.isDirectory()) {
			return;
		}
		Metadata metadata;
		metadata = ImageMetadataReader.readMetadata(img);
		Collection<GpsDirectory> gps = metadata.getDirectoriesOfType(GpsDirectory.class);
		if (gps == null || gps.size() <= 0) {
			return;
		}
		for (GpsDirectory item : gps) {
//			System.out.println(item);
			Collection<Tag> tags = item.getTags();
			for (Tag itemTag : tags) {
				if(itemTag==null||"".equals(itemTag.getTagName())) {
					System.out.println("ERROR0");
					continue;
				}
				//System.out.println(itemTag.getTagType()+"--->"+itemTag.getTagName() + "---------->" + itemTag.getDescription());

				if ("GPS Latitude Ref".equals(itemTag.getTagName())) {
					imageBean.setLatitudeRef(itemTag.getDescription());
				}
				if ("GPS Longitude Ref".equals(itemTag.getTagName())) {
					imageBean.setLongitudeRef(itemTag.getDescription());
				}
				if ("GPS Latitude".equals(itemTag.getTagName())) {
					imageBean.setLatitude(pointToLatlong(itemTag.getDescription()));
				}else {
					//System.out.println("1"+itemTag.getTagName());
				}
				if ("GPS Longitude".equals(itemTag.getTagName())) {
					imageBean.setLongitude(pointToLatlong(itemTag.getDescription()));
				}else {
					//System.out.println("2"+itemTag.getTagName());
				}
				if ("GPS Altitude".equals(itemTag.getTagName())) {
					String height = itemTag.getDescription();
					if (height != null && !"".equals(height)) {
						if (height.contains("metres")) {
							height = height.replace("metres", "");
						}
					}
					imageBean.setHeight(height);
				}else {
					//System.out.println("3"+itemTag.getTagName());
				}
			}
		}
	}

	/**
	 * 经纬度格式 转换为 度分秒格式 ,如果需要的话可以调用该方法进行转换 41 *
	 * 
	 * @param point 坐标点 @return
	 */
	public static double pointToLatlong(String point) {
		Double duStr;
		try {
			String strDu = point.substring(0, point.indexOf("°")).trim();
			if(!StrUtil.isEmpty(strDu)&&strDu.contains(",")) {
				strDu = strDu.replace(",", ".");
			}
			Double du = Double.parseDouble(strDu);
			String strFen = point.substring(point.indexOf("°") + 1, point.indexOf("'")).trim();
			if(!StrUtil.isEmpty(strFen)&&strFen.contains(",")) {
				strFen = strFen.replace(",", ".");
			}
			Double fen = Double.parseDouble(strFen);
			String strMiao = point.substring(point.indexOf("'") + 1, point.indexOf("\"")).trim();
			if(!StrUtil.isEmpty(strMiao)&&strMiao.contains(",")) {
				strMiao = strMiao.replace(",", ".");
			}
			Double miao = Double.parseDouble(strMiao);
			duStr = du + fen / 60 + miao / 60 / 60;
		}catch(Exception e) {
			duStr = 0d;
			e.printStackTrace();
		}	
		return duStr;
	}

}
