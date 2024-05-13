package application.control;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;

import base.controller.ConfirmDialogController.CallBack;
import beans.AMapGeocodingBean;
import beans.ImageBean;
import consts.ConstSize;
import javafx.animation.Animation.Status;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.PathTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.StrokeLineJoin;
import javafx.stage.Stage;
import javafx.util.Duration;
import net.kurobako.gesturefx.GesturePane;
import utils.AnimatorUtil;
import utils.GoogleMapUtil;
import utils.ResUtil;
import utils.SysUtil;
import utils.ToastUtil;
import utils.UIUtil;
import utils.ProgressTask.ProgressTask;
import views.MyToolTip;

public class GoogleMapFlightLineController implements Initializable {
	private static final String TIP = ResUtil.gs("flight_tips");
	private static final double FrameW = ConstSize.Flight_Width;
	private static final double FrameH = ConstSize.Flight_Height;
	private static final double FlightPaneW = FrameW - 40;
	private static final double FlightPaneH = FrameH - 40 - 50;
	private static final double FlightDataOffset = 20;

	private HashMap<ImageBean, Label> labelMap = new HashMap<ImageBean, Label>();

	private final Image camera = new Image(getClass().getResourceAsStream("/resources/camera-fill-normal.png"), 20, 20,
			false, true);
	private final Image cameraHover = new Image(getClass().getResourceAsStream("/resources/camera-hover4.png"), 20, 20,
			false, true);
	private final Image cameraFocus = new Image(getClass().getResourceAsStream("/resources/camera-fill-focus.png"), 20,
			20, false, true);
	private final Image cameraDeleted = new Image(getClass().getResourceAsStream("/resources/camera-fill-deleted.png"),
			20, 20, false, true);
	private final Image imageTarget = new Image(getClass().getResourceAsStream("/resources/flight_uav.png"), 25, 25,
			false, true);
	private final Image imageSwitchOn = new Image(getClass().getResourceAsStream("/resources/eye_open.png"), 20, 20,
			false, true);
	private final Image imageSwitchOff = new Image(getClass().getResourceAsStream("/resources/eye_close.png"), 20, 20,
			false, true);
	private DropShadow dropShadow;

	@FXML
	BorderPane root;
	@FXML
	ImageView imageView;
	@FXML
	AnchorPane pane;
	@FXML
	GesturePane gesturePane;
	@FXML
	Label textArea_tip;
	@FXML
	Pane pane_Canvas;
	@FXML
	Pane gesturePaneFlight;
	@FXML
	ImageView btn_switch;

	// 谷歌地图图片
	private Image imageMap;

	// 经纬度数据分析得到的
	private double distX;
	private double distY;
	private double xMin;
	private double yMin;

	private double radioData;
	private double scale;
	private int radioType;

	private boolean isShowTipsPane = true;

	private ObservableList<ImageBean> listData = FXCollections.observableArrayList();
	private AMapGeocodingBean aMapGeocodingBean;
	private PathTransition pathTransition;
	@FXML
	Label textArea_place;
	@FXML
	GesturePane largePane;
	private ImageBean preSelectImage;
	private boolean preLabelIschange = false;
	private ImageListController imageListController;
//	private HashMap<String, Boolean> deletedLabelMap;
	private ObservableList<ImageBean> selectedList = FXCollections.observableArrayList();
	@FXML
	Label bottomLabel_all;
	@FXML
	Label bottomLabel_selected;
	@FXML
	Label bottomLabel_deleted;
	@FXML
	Label bottomLabel_current;
	@FXML
	VBox vbox_tip;
	private HashMap<String, Boolean> deletedLabelMap;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		dropShadow = new DropShadow();
		dropShadow.setRadius(5.0);
		dropShadow.setOffsetX(1.0);
		dropShadow.setOffsetY(1.0);
		dropShadow.setColor(Color.LIGHTBLUE);
		initGesturePane();
		initTextData();
		root.setOnKeyPressed(new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent event) {
				if (event.getCode() == KeyCode.DELETE) {
					deleteImages();
				}
			}
		});
		selectedList.addListener(new ListChangeListener<ImageBean>() {
			@Override
			public void onChanged(Change<? extends ImageBean> c) {
				while (c.next()) {
					if (c.wasPermutated()) {
					} else if (c.wasUpdated()) {
					} else {
						int addOrMove = -1;
						List<ImageBean> subListAdd = new ArrayList<ImageBean>();
						List<ImageBean> subListRemove = new ArrayList<ImageBean>();
						if (c.getAddedSize() > 0 && c.wasAdded() && selectedList.size() > 0) {
							addOrMove = 1;
							subListAdd.addAll(c.getAddedSubList());
						}
						if (c.getRemovedSize() > 0 && c.wasRemoved()) {
							addOrMove = 0;
							subListRemove.addAll(c.getRemoved());
						}
						if (addOrMove == -1) {
							return;
						}
						Platform.runLater(new MyRunnable(subListAdd, subListRemove));
					}
				}
			}
		});
	}

	public void setData(ObservableList<ImageBean> listDat, HashMap<String, Boolean> deletedLabelMap,
			ImageListController imageListController, int deletedNum) {
		listData = listDat;
		this.deletedLabelMap = deletedLabelMap;
		System.out.println("deleleleele:"+deletedNum);
		bottomLabel_all.setText(ResUtil.gs("image_num", listData.size() + ""));
		updataDeletedNum(deletedNum, deletedLabelMap.size() <= ImageListController.MAX_SHOW_DELETENUM ? true : false);
		listData.addListener(new ListChangeListener<ImageBean>() {
			@Override
			public void onChanged(Change<? extends ImageBean> c) {
				Platform.runLater(new Runnable() {
					@Override
					public void run() {
						bottomLabel_all.setText(ResUtil.gs("image_num", listData.size() + ""));
					}
				});
			}
		});
		this.imageListController = imageListController;
		if (listData == null || listData.size() == 0) {
			System.out.println("飞机界面数据：" + listData.size());
			ToastUtil.toast(ResUtil.gs("load_data_error"));
		}
		ProgressTask task = new ProgressTask(new ProgressTask.MyTask<double[][]>() {
			private double xMax;
			private double yMax;
			private boolean isReverseY;
			private boolean isReverseX;

			/**
			 * 画东西
			 * 
			 * @param gc
			 * @param xList
			 * @param yList
			 * @param size
			 */
			private void drawShapes(GraphicsContext gc, double[] xList, double[] yList, int size) {
				// 画线
//				LinearGradient linearGradient1 = new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
//						new Stop[] { new Stop(0, Color.LIGHTGREEN), new Stop(1, Color.LIGHTSKYBLUE) });
				gc.setStroke(Color.LIGHTGREEN);
				gc.setLineWidth(3);
				gc.setLineJoin(StrokeLineJoin.ROUND);
				gc.strokePolyline(xList, yList, size);

				ArrayList<Double> lineData = new ArrayList<Double>();
				// 画点
				for (int i = 0; i < size; i++) {
					Label item;
					if (listData.size() > i) {
						item = generateLabel(listData.get(i));
					} else {
						item = generateLabel(null);
					}
					item.setLayoutX(xList[i] - item.getLayoutBounds().getMinX() - 7);
					item.setLayoutY(yList[i] - item.getLayoutBounds().getMinY() - 14);
					lineData.add(xList[i]);
					lineData.add(yList[i]);
					pane_Canvas.getChildren().add(item);
				}

				gc.restore();
				// 画动画
				Polyline polyLine = new Polyline();
				polyLine.getPoints().addAll(lineData);
				pathTransition = new PathTransition();
				pathTransition.setDuration(Duration.millis(500 * size));
				pathTransition.setPath(polyLine);
				pathTransition.setCycleCount(Timeline.INDEFINITE);
				pathTransition.setOrientation(PathTransition.OrientationType.ORTHOGONAL_TO_TANGENT);// 方向

				Label target = new Label();
				target.setPrefWidth(25);
				target.setPrefHeight(25);
				target.setGraphic(new ImageView(imageTarget));
				DropShadow dropShadowFlight = new DropShadow();
				dropShadowFlight.setRadius(3.0);
				dropShadowFlight.setOffsetX(3.0);
				dropShadowFlight.setOffsetY(3.0);
				dropShadowFlight.setColor(Color.DARKGRAY);
				target.setEffect(dropShadowFlight);

				pane_Canvas.getChildren().addAll(target);

				pathTransition.setNode(target);
				pathTransition.setInterpolator(Interpolator.LINEAR);
				pathTransition.play();
			}

			/**
			 * 生成点
			 * 
			 * @param imageBean
			 * @return
			 */
			private Label generateLabel(ImageBean imageBean) {
				Label item = new Label();
				String name = imageBean.getName();
				item.setId(name);
				item.setPrefHeight(14);
				item.setPrefWidth(14);
				item.setMaxWidth(14);
				item.setMaxHeight(14);
				Insets insets = new Insets(0);
				item.setPadding(insets);
				if (deletedLabelMap.containsKey(name) && !deletedLabelMap.get(name)) {
					item.setGraphic(new ImageView(cameraDeleted));
				} else {
					item.setGraphic(new ImageView(camera));
				}
//				item.setEffect(dropShadow);
				item.addEventHandler(MouseEvent.MOUSE_ENTERED, new EnteredHandler(item));
				item.addEventHandler(MouseEvent.MOUSE_EXITED, new ExitHandler(item));

				item.addEventHandler(MouseEvent.ANY, new EventHandler<MouseEvent>() {
					private double oldScreenY;
					private double oldScreenX;
					private final double GAP = 5;

					@Override
					public void handle(MouseEvent e) {
						if (e.getEventType() == MouseEvent.MOUSE_PRESSED && e.getButton() == MouseButton.PRIMARY) { // 鼠标按下的事件
							this.oldScreenX = e.getScreenX();
							this.oldScreenY = e.getScreenY();
						} else if (e.getEventType() == MouseEvent.MOUSE_RELEASED
								&& e.getButton() == MouseButton.PRIMARY) { // 鼠标抬起的事件
							double nowX = e.getScreenX();
							double nowY = e.getScreenY();
							if (Math.abs(oldScreenX - nowX) <= GAP && Math.abs(oldScreenY - nowY) <= GAP) {
								if (labelMap.get(imageBean) != null) {
									if (!e.isControlDown() && !e.isShiftDown()) {
										// 没按ctrl并且没按shift，打开图片
										if (!SysUtil.exeOpenFile(imageBean.getPath())) {
											ToastUtil.toast(ResUtil.gs("open_image_error"));
										}
									} else {
										System.out.println("else");
										preLabelIschange = true;
										if (e.isControlDown()) {
											// 按了ctrl,添加或删除
											System.out.println("isControlDown");
											if (selectedList.contains(imageBean)) {
												selectedList.remove(imageBean);
												if (deletedLabelMap.containsKey(imageBean.getName())
														&& !deletedLabelMap.get(imageBean.getName())) {
													labelMap.get(imageBean).setGraphic(new ImageView(cameraDeleted));
												} else {
													labelMap.get(imageBean).setGraphic(new ImageView(camera));
												}
											} else {
												selectedList.add(imageBean);
												labelMap.get(imageBean).setGraphic(new ImageView(cameraFocus));
											}
										} else if (e.isShiftDown()) {
											// 按了shift。如果点击的是选择的，那么与pre中间都变为未选择；
											// 如果点击是未选择,那么与pre中间都变为已选择
											if (preSelectImage == null) {
												if (selectedList.size() > 0) {
													preSelectImage = selectedList.get(selectedList.size() - 1);
												} else {
													preSelectImage = listData.get(0);
												}
											}
											int preIndex = listData.indexOf(preSelectImage);
											if (preIndex < 0) {
												preIndex = 0;
											}
											int toIndex = listData.indexOf(imageBean);
											System.out.println("isShiftDown:  " + preIndex + ":" + toIndex);
											if (selectedList.contains(imageBean)) {
												// 如果点击的是选择的，那么与pre中间都变为未选择；
												if (preIndex <= toIndex) {
													for (int i = preIndex; i <= toIndex; i++) {
														ImageBean item = listData.get(i);
														selectedList.remove(item);
														if (deletedLabelMap.containsKey(item.getName())
																&& !deletedLabelMap.get(item.getName())) {
															labelMap.get(item).setGraphic(new ImageView(cameraDeleted));
														} else {
															labelMap.get(item).setGraphic(new ImageView(camera));
														}
													}
												} else {
													for (int i = toIndex; i <= preIndex; i++) {
														ImageBean item = listData.get(i);
														selectedList.remove(item);
														if (deletedLabelMap.containsKey(item.getName())
																&& !deletedLabelMap.get(item.getName())) {
															labelMap.get(item).setGraphic(new ImageView(cameraDeleted));
														} else {
															labelMap.get(item).setGraphic(new ImageView(camera));
														}
													}
												}
											} else {
												// 点击是未选择,那么与pre中间都变为已选择
												if (preIndex <= toIndex) {
													for (int i = preIndex; i <= toIndex; i++) {
														ImageBean item = listData.get(i);
														if (!selectedList.contains(item)) {
															selectedList.add(item);
															labelMap.get(item).setGraphic(new ImageView(cameraFocus));
														}
													}
												} else {
													for (int i = toIndex; i <= preIndex; i++) {
														ImageBean item = listData.get(i);
														if (!selectedList.contains(item)) {
															selectedList.add(item);
															labelMap.get(item).setGraphic(new ImageView(cameraFocus));
														}
													}
												}
											}
										}
										preSelectImage = imageBean;
									}
								}
							}
							oldScreenX = nowX;
							oldScreenY = nowY;
						} else if (e.getEventType() == MouseEvent.MOUSE_CLICKED
								&& e.getButton() == MouseButton.SECONDARY) {
							if (!selectedList.contains(imageBean)) {
								item.setContextMenu(null);
							} else {
								item.setContextMenu(new MyContextMenu(imageBean));
							}
							e.consume();
						}
					}
				});
				MyToolTip myToolTip = new MyToolTip(generateToolTipInfo(imageBean));
				item.setTooltip(myToolTip);

//				item.setContextMenu(new MyContextMenu(imageBean));

				labelMap.put(imageBean, item);
				return item;
			}

			/**
			 * 初始化tooltip内容
			 * 
			 * @param imageBean
			 * @return
			 */
			private String generateToolTipInfo(ImageBean imageBean) {
				if (imageBean == null) {
					return "";
				}
				StringBuilder sb = new StringBuilder("");
				sb.append(ResUtil.gs("flight_image_name") + imageBean.getName());
				if ("".equals(imageBean.getLongitudeRef())) {
					sb.append("\n" + ResUtil.gs("flight_image_long") + imageBean.getLongitude());
					sb.append("\n" + ResUtil.gs("flight_image_lat") + imageBean.getLatitude());
				} else {
					sb.append("\n" + ResUtil.gs("flight_image_long") + imageBean.getLongitudeRef() + ":"
							+ imageBean.getLongitude());
					sb.append("\n" + ResUtil.gs("flight_image_lat") + imageBean.getLatitudeRef() + ":"
							+ imageBean.getLatitude());
				}

				return sb.toString();
			}

			/**
			 * 解析数据成为google 地图可用的数据
			 * 
			 * @param listData
			 * @return
			 */
			private void initFlightMapView(double[][] analyData) {
				double canvasW = FlightPaneW, canvasH = FlightPaneH;
				if (radioType == 0) {
					// 飞行路线为瘦高，固定高，缩小宽
					canvasW = FlightPaneH / radioData + 2 * FlightDataOffset;
					canvasH = FlightPaneH;
					AnchorPane.setLeftAnchor(gesturePaneFlight, (FlightPaneW - canvasW) / 2);
					AnchorPane.setBottomAnchor(gesturePaneFlight, (double) 0);
				} else {
					// 飞行路线为矮胖，固定宽，缩小高
					canvasW = FlightPaneW;
					canvasH = FlightPaneW * radioData + 2 * FlightDataOffset;
					AnchorPane.setLeftAnchor(gesturePaneFlight, (double) 0);
					AnchorPane.setBottomAnchor(gesturePaneFlight, (FlightPaneH - canvasH) / 2);
				}
				pane_Canvas.setPrefSize(canvasW, canvasH);
				gesturePaneFlight.setPrefSize(canvasW, canvasH);
				gesturePaneFlight.setMaxSize(canvasW, canvasH);
				Canvas canvas = new Canvas(canvasW, canvasH);
				GraphicsContext gc = canvas.getGraphicsContext2D();
				canvas.setLayoutX(0);
				canvas.setLayoutY(0);
				pane_Canvas.getChildren().add(canvas);

				drawShapes(gc, analyData[0], analyData[1],
						analyData[0].length >= analyData[1].length ? analyData[1].length : analyData[0].length);
			}

			/**
			 * 解析出谷歌地图需要的数据
			 * 
			 * @param listData
			 * @return
			 */
			private ArrayList<Double> analyDataToGoogleData(ObservableList<ImageBean> listData) {
				xMax = listData.get(0).getLongitude();
				xMin = listData.get(0).getLongitude();
				yMax = listData.get(0).getLatitude();
				yMin = listData.get(0).getLatitude();
				// 1.3获取
				for (ImageBean item : listData) {
					if (item.getLatitude() == 0 && item.getLongitude() == 0) {
						continue;
					}
					if (xMax < item.getLongitude()) {
						xMax = item.getLongitude();
					}
					if (xMin > item.getLongitude()) {
						xMin = item.getLongitude();
					}
					if (yMax < item.getLatitude()) {
						yMax = item.getLatitude();
					}
					if (yMin > item.getLatitude()) {
						yMin = item.getLatitude();
					}
				}
				double centerX = (xMax + xMin) / 2;
				double centerY = (yMax + yMin) / 2;
				ArrayList<Double> data = new ArrayList<Double>();
				data.add(centerX);
				data.add(centerY);
				for (ImageBean item : listData) {
					if (item.getLatitude() == 0 && item.getLongitude() == 0) {
						continue;
					}
					double dataX = item.getLongitude();
					double dataY = item.getLatitude();
					if ("S".equals(item.getLatitudeRef())) {
						dataX = -dataX;
						isReverseY = false;
					} else if ("N".equals(item.getLatitudeRef())) {
						isReverseY = true;
					} else {
						if (dataX >= 0) {
							isReverseY = true;
						} else {
							isReverseY = false;
						}
					}

					if ("W".equals(item.getLongitudeRef())) {
						dataY = -dataY;
						isReverseX = true;
					} else if ("E".equals(item.getLongitudeRef())) {
						isReverseX = false;
					} else {
						if (dataY >= 0) {
							isReverseX = false;
						} else {
							isReverseX = true;
						}
					}
					data.add(dataX);
					data.add(dataY);
				}
				distX = xMax - xMin;
				distY = yMax - yMin;
				return data;
			}

			/**
			 * 初始化google 地图
			 * 
			 * @param listData
			 * @return
			 */
			private void initGoogleMapData(ObservableList<ImageBean> listData) {
				ArrayList<Double> analyGoogleData = analyDataToGoogleData(listData);
				System.out.println("analyGoogleData" + analyGoogleData.size());
				if (analyGoogleData != null && analyGoogleData.size() >= 2) {
//					geocodingBean = GoogleMapUtil.getGeocoding(analyGoogleData.get(1), analyGoogleData.get(0));
					aMapGeocodingBean = GoogleMapUtil.getAMapGeocoding(analyGoogleData.get(1), analyGoogleData.get(0));
				}
//				imageMap = GoogleMapUtil.getMapImage(analyGoogleData);
			}

			/**
			 * 初始化飞行图 数据
			 * 
			 * @param listData
			 * @return
			 */
			private double[][] initFlightMapData() {
				double[][] afterData = new double[2][];
				try {
					radioData = distY / distX;
					double windowsRadioData = FlightPaneH / FlightPaneW;
					// 1.选择按x还是y
					scale = 0;
					if (radioData >= windowsRadioData) {
						// 图像比控件更瘦高,使用高来比
						scale = (FlightPaneH - FlightDataOffset * 2) / distY;
						radioType = 0;
					} else {
						// 图像比控件更矮胖,使用宽来比
						scale = (FlightPaneW - FlightDataOffset * 2) / distX;
						radioType = 1;
					}
//					if (radioData >= 1) {
//						// 瘦高，固定高
//						scale = (FlightPaneH - FlightDataOffset * 2) / distY;
//						radioType = 0;
//					} else {
//						// 矮胖，固定宽
//						scale = (FlightPaneW - FlightDataOffset * 2) / distX;
//						radioType = 1;
//					}
					System.out.println("radioData" + radioData + "scale" + scale);
					// 2.重置数据大小
					double[] xList = new double[listData.size()];
					double[] yList = new double[listData.size()];
					for (int i = 0; i < listData.size(); i++) {
						ImageBean item = listData.get(i);
						if (item.getLatitude() == 0 && item.getLongitude() == 0) {
							continue;
						}
						double dataX = (item.getLongitude() - xMin) * scale;
						double dataY = (item.getLatitude() - yMin) * scale;
						if (isReverseX) {
							xList[i] = (distX) * scale - dataX + FlightDataOffset;
						} else {
							xList[i] = dataX + FlightDataOffset;
						}
						if (isReverseY) {
							yList[i] = (distY) * scale - dataY + FlightDataOffset;
						} else {
							yList[i] = dataY + FlightDataOffset;
						}
					}
					afterData[0] = xList;
					afterData[1] = yList;
				} catch (Exception e) {
					e.printStackTrace();
				}
				return afterData;
			}

			@Override
			protected double[][] call() {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				double[][] initFlightMapData = null;
				try {
					// 初始化谷歌地图数据
					initGoogleMapData(listData);
					initFlightMapData = initFlightMapData();
				} catch (Exception e) {
					e.printStackTrace();
				}
				return initFlightMapData;
			}

			@Override
			protected void succeeded() {
				super.succeeded();
				try {
					// 设置地图地理位置信息
					if (aMapGeocodingBean != null && "1".equals(aMapGeocodingBean.getStatus())) {
						if (aMapGeocodingBean.getRegeocode() != null
								&& !"".equals(aMapGeocodingBean.getRegeocode().getFormatted_address())) {
							textArea_place.setVisible(true);
							// 设置place背景透明
							textArea_place.setText(
									ResUtil.gs("flight_geo") + aMapGeocodingBean.getRegeocode().getFormatted_address());
						}
					}

					// 设置tips
					if (imageMap != null) {
						imageView.setImage(imageMap);
						textArea_tip.setVisible(true);
						vbox_tip.setVisible(true);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

				try {
					initFlightMapView(get());
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
				AnimatorUtil.fadeShow(pane, 700);
			}

			@Override
			protected void unSucceeded() {
				super.unSucceeded();
//				ToastUtil.toast(ResUtil.gs("load_data_error"));
			}

		}, (Stage) root.getParent().getScene().getWindow());
		task.start();
	}

	/**
	 * 设置放大缩小手势功能
	 */
	private void initGesturePane() {
		gesturePane.setMaxSize(976, 626);
		gesturePane.setScrollBarPolicy(GesturePane.ScrollBarPolicy.NEVER);
		gesturePane.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent e) {
				if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
					Point2D pivotOnTarget = gesturePane.targetPointAt(new Point2D(e.getX(), e.getY()))
							.orElse(gesturePane.targetPointAtViewportCentre());
					gesturePane.animate(Duration.millis(200)).interpolateWith(Interpolator.EASE_BOTH)
							.zoomBy(gesturePane.getCurrentScale(), pivotOnTarget);
				}
			}
		});
		gesturePaneFlight.setMaxSize(FlightPaneW, FlightPaneH);
		largePane.setScrollBarPolicy(GesturePane.ScrollBarPolicy.NEVER);
	}

	private void initTextData() {
		textArea_tip.setText(TIP);
	}

	@FXML
	public void onClickPaneSwitch() {
		if (isShowTipsPane) {
			if(fadeShow!=null) {
				if(fadeShow.getStatus()==Status.RUNNING) {
					fadeShow.stop();
				}
			}
			fadeHide = AnimatorUtil.fadeHide(textArea_tip, 350);
			AnimatorUtil.fadeHide(vbox_tip, 350);
			fadeHide.setOnFinished(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					textArea_tip.setVisible(false);
					vbox_tip.setVisible(false);
				}
			});
			btn_switch.setImage(imageSwitchOn);
		} else {
			fadeShow = AnimatorUtil.fadeShow(textArea_tip, 350);
			fadeShow = AnimatorUtil.fadeShow(vbox_tip, 350);
			textArea_tip.setVisible(true);
			vbox_tip.setVisible(true);
			fadeShow.setOnFinished(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					textArea_tip.setVisible(true);
					vbox_tip.setVisible(true);
				}
			});
			btn_switch.setImage(imageSwitchOff);
		}
		isShowTipsPane = !isShowTipsPane;
	}

	private ImageView preLabelView;

	/**
	 * 鼠标移入监听
	 * 
	 * @author DP
	 */
	class EnteredHandler implements EventHandler<Event> {
		private Label label;

		public EnteredHandler(Label label) {
			this.label = label;
		}

		@Override
		public void handle(Event event) {
			bottomLabel_current.setText(ResUtil.gs("flightLine_current_img", label.getId()));
			preLabelIschange = false;
			preLabelView = (ImageView) label.getGraphic();
			label.setGraphic(new ImageView(cameraHover));
			if (callback != null) {
				callback.onFocusChange(label.getId(), true);
			}
		}
	}

	/**
	 * 鼠标移出监听
	 * 
	 * @author DP
	 */
	class ExitHandler implements EventHandler<Event> {
		private Label label;

		public ExitHandler(Label label) {
			this.label = label;
		}

		@Override
		public void handle(Event event) {
			bottomLabel_current.setText("");
			if (!preLabelIschange) {
				if (preLabelView != null) {
					label.setGraphic(preLabelView);
				} else {
					label.setGraphic(new ImageView(camera));
				}
			}
			if (callback != null) {
				callback.onFocusChange(label.getId(), false);
			}
		}
	}

	/**
	 * 图片列表界面删除数据,调用此方法，来改变飞行路线界面
	 * 
	 * @param selectedItem
	 */
	public void onImageDeleteFromImageList(ImageBean deletedItem) {
		if (labelMap.size() > 0) {
			Label label = labelMap.get(deletedItem);
			if (label != null) {
//				label.setDisable(true);
				label.setGraphic(new ImageView(cameraDeleted));
				selectedList.remove(deletedItem);
//				labelMap.remove(selectedItem);
			}
		}
	}

	public void setCallback(FlightLineCallBack callback) {
		this.callback = callback;
	}

	private FlightLineCallBack callback;
	private FadeTransition fadeHide;
	private FadeTransition fadeShow;

	public interface FlightLineCallBack {
		void onDeleteImage(ImageBean image);

		void onFocusChange(String id, boolean b);

		void onRecallImage(ImageBean bean);

		void onRecallImage(ObservableList<ImageBean> selectedList);
	}

	class MyContextMenu extends ContextMenu {
		ImageBean bean;

		public MyContextMenu(ImageBean bean) {
			this.bean = bean;
			MenuItem delete = new MenuItem(ResUtil.gs("remove"));
			delete.setStyle("-fx-font-size:12;");
			delete.setOnAction(new EventHandler<ActionEvent>() {
				public void handle(ActionEvent event) {
					deleteImages();
				}
			});
			MenuItem recall = new MenuItem(ResUtil.gs("recall"));
			recall.setStyle("-fx-font-size:12;");
			recall.setOnAction(new EventHandler<ActionEvent>() {
				public void handle(ActionEvent event) {
					recallImages();
				}
			});

			getItems().addAll(delete, recall);
		}

	}

	/**
	 * 添加图片进入此项目
	 */
	private void recallImages() {
		if (selectedList == null || selectedList.size() <= 0) {
			return;
		}
		if (selectedList.size() == 1) {
			UIUtil.openConfirmDialog(getClass(), ConstSize.Confirm_Dialog_Frame_Width,
					ConstSize.Confirm_Dialog_Frame_Height, ResUtil.gs("imageList_recall_image"),
					ResUtil.gs("imageList_recall_image_confirm", selectedList.get(0).getName()),
					(Stage) root.getScene().getWindow(), new CallBack() {
						@Override
						public void onCancel() {
						}

						@Override
						public void onConfirm() {
							preSelectImage = null;
							ImageBean bean = selectedList.get(0);
							Label label = labelMap.get(bean);
							if (label != null) {
//								label.setDisable(true);
								if (callback != null) {
									callback.onRecallImage(bean);
								}
//								listData.remove(bean);
//								labelMap.remove(bean);
								label.setGraphic(new ImageView(camera));
								selectedList.remove(bean);
							}
						}
					});
		} else {
			UIUtil.openConfirmDialog(getClass(), ConstSize.Confirm_Dialog_Frame_Width,
					ConstSize.Confirm_Dialog_Frame_Height, ResUtil.gs("imageList_recall_image"),
					ResUtil.gs("imageList_recall_lot_image_confirm", selectedList.size()),
					(Stage) root.getScene().getWindow(), new CallBack() {
						@Override
						public void onCancel() {
						}

						@Override
						public void onConfirm() {
							preSelectImage = null;
							for (ImageBean bean : selectedList) {
								Label label = labelMap.get(bean);
								if (label != null) {
//									label.setDisable(true);
//									listData.remove(bean);
//									labelMap.remove(bean);
									label.setGraphic(new ImageView(camera));
								}
							}
							if (callback != null) {
								callback.onRecallImage(selectedList);
							}
							selectedList.clear();
						}
					});
		}
	}

	/**
	 * 删除图片
	 */
	private void deleteImages() {
		if (selectedList == null || selectedList.size() <= 0) {
			return;
		}
		if (selectedList.size() == 1) {
			UIUtil.openConfirmDialog(getClass(), ConstSize.Confirm_Dialog_Frame_Width,
					ConstSize.Confirm_Dialog_Frame_Height, ResUtil.gs("imageList_remove_image"),
					ResUtil.gs("imageList_remove_image_confirm", selectedList.get(0).getName()),
					(Stage) root.getScene().getWindow(), new CallBack() {
						@Override
						public void onCancel() {
						}

						@Override
						public void onConfirm() {
							preSelectImage = null;
							ImageBean bean = selectedList.get(0);
							Label label = labelMap.get(bean);
							if (label != null) {
//								label.setDisable(true);
								if (callback != null) {
									callback.onDeleteImage(bean);
								}
//								listData.remove(bean);
//								labelMap.remove(bean);
								label.setGraphic(new ImageView(cameraDeleted));
								selectedList.remove(bean);
							}
						}
					});
		} else {
			UIUtil.openConfirmDialog(getClass(), ConstSize.Confirm_Dialog_Frame_Width,
					ConstSize.Confirm_Dialog_Frame_Height, ResUtil.gs("imageList_remove_image"),
					ResUtil.gs("imageList_remove_lot_image_confirm", selectedList.size()),
					(Stage) root.getScene().getWindow(), new CallBack() {
						@Override
						public void onCancel() {
						}

						@Override
						public void onConfirm() {
							preSelectImage = null;
							for (ImageBean bean : selectedList) {
								Label label = labelMap.get(bean);
								if (label != null) {
//									label.setDisable(true);
									if (callback != null) {
										callback.onDeleteImage(bean);
									}
//									listData.remove(bean);
//									labelMap.remove(bean);
									label.setGraphic(new ImageView(cameraDeleted));
								}
							}
							selectedList.clear();
						}
					});
		}
	}

	/**
	 * 多删
	 * 
	 * @param selectedItem
	 */
	public void onImageDeleteFromImageList(ArrayList<ImageBean> selectedItem) {
		if (labelMap.size() > 0) {
			if (selectedItem != null && selectedItem.size() > 0) {
				for (ImageBean item : selectedItem) {
					Label label = labelMap.get(item);
					if (label != null) {
//						label.setDisable(true);
						label.setGraphic(new ImageView(cameraDeleted));
						selectedList.remove(item);
//						labelMap.remove(item);
					}
				}
			}
		}
	}

	/**
	 * 从图片列表界面传来的控制。来重新添加某图片
	 * 
	 * @param imageBean
	 */
	public void onImageRecallFromImageList(ImageBean imageBean) {
		if (labelMap.size() > 0) {
			Label label = labelMap.get(imageBean);
			if (label != null) {
				label.setGraphic(new ImageView(camera));
				selectedList.remove(imageBean);
			}
		}
	}

	/**
	 * 从图片列表界面传来的控制。来重新添加某些图片
	 * 
	 * @param deleteList
	 */
	public void onImageRecallFromImageList(ArrayList<ImageBean> deleteList) {
		if (labelMap.size() > 0) {
			if (deleteList != null && deleteList.size() > 0) {
				for (ImageBean item : deleteList) {
					Label label = labelMap.get(item);
					if (label != null) {
						label.setGraphic(new ImageView(camera));
						selectedList.remove(item);
					}
				}
			}
		}
	}

	/**
	 * 图片列表界面选择了某个image
	 */
	public void onImageSelectedFromImageList(List<? extends ImageBean> list, int addOrRemove) {
		if (labelMap.size() > 0) {
			if (addOrRemove == 1) {
				// 添加
				if (list != null && list.size() > 0) {
					for (ImageBean item : list) {
						Label newLabel = labelMap.get(item);
						if (newLabel != null) {
							newLabel.setGraphic(new ImageView(cameraFocus));
						}
					}
					selectedList.addAll(list);
				}
			} else if (addOrRemove == 0) {
				// 删除
				if (list != null && list.size() > 0) {
					for (ImageBean item : list) {
						Label old = labelMap.get(item);
						if (old != null) {
							if (deletedLabelMap.containsKey(item.getName()) && !deletedLabelMap.get(item.getName())) {
								old.setGraphic(new ImageView(cameraDeleted));
							} else {
								old.setGraphic(new ImageView(camera));
							}
						} else {
							System.out.println("hash map no" + item.getName());
						}
					}
					selectedList.removeAll(list);
				}
			}
		}
	}

	public void onImageSelected(ImageBean oldValue, ImageBean newValue) {
		// 单选时
		Label old = labelMap.get(oldValue);
		if (old != null) {
			if (deletedLabelMap.containsKey(oldValue.getName()) && !deletedLabelMap.get(oldValue.getName())) {
				old.setGraphic(new ImageView(cameraDeleted));
			} else {
				old.setGraphic(new ImageView(camera));
			}
			selectedList.remove(oldValue);
		}
		Label newLabel = labelMap.get(newValue);
		if (newLabel != null) {
			newLabel.setGraphic(new ImageView(cameraFocus));
			selectedList.add(newValue);
		}
	}

	public void onImageClearFocus(ImageBean newValue) {
		for (Map.Entry<ImageBean, Label> entry : labelMap.entrySet()) {
			Label label = entry.getValue();
			if (!label.isDisable()) {
				label.setGraphic(new ImageView(camera));
				selectedList.remove(entry.getKey());
			}
		}
		Label newLabel = labelMap.get(newValue);
		if (newLabel != null) {
			newLabel.setGraphic(new ImageView(cameraFocus));
			selectedList.clear();
			selectedList.add(newValue);
		}
	}

	class MyRunnable implements Runnable {
		private List<? extends ImageBean> subListAdd;
		private List<? extends ImageBean> subListRemove;

		public MyRunnable(List<? extends ImageBean> subListAdd, List<ImageBean> subListRemove) {
			this.subListAdd = subListAdd;
			this.subListRemove = subListRemove;
		}

		public void run() {
			bottomLabel_selected.setText(ResUtil.gs("selected_image_num", selectedList.size() + ""));
			Stage stage = (Stage) root.getScene().getWindow();
			if (stage.isFocused()) {
				if (imageListController != null) {
					imageListController.onImageSelectedFromFlightLine(subListRemove, 0);
				}
				if (imageListController != null) {
					if (selectedList.size() > 0) {
						imageListController.onImageSelectedFromFlightLine(subListAdd, 1);
					}
				}
			} else {
//				System.out.println("飞行界不是焦点");
			}
		}
	}

	/**
	 * 图片列表通知来进行修改已删除的图片数量
	 * 
	 * @param deletedNum
	 * @param isVisible
	 */
	public void updataDeletedNum(int deletedNum, boolean isVisible) {
		bottomLabel_deleted.setVisible(isVisible);
		bottomLabel_deleted.setText(ResUtil.gs("image_deleted_num",listData.size()- deletedNum + ""));
	}

}
