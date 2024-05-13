package application.control;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.ResourceBundle;

import com.drew.imaging.ImageProcessingException;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXRadioButton;
import com.jfoenix.controls.JFXTextField;

import application.control.GoogleMapFlightLineController.FlightLineCallBack;
import base.controller.ConfirmDialogController.CallBack;
import beans.ImageBean;
import beans.MyFxmlBean;
import beans.ProjectBean;
import consts.ConstSize;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseDragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import utils.FileChooserUtil;
import utils.FileUtil;
import utils.GpsUtil;
import utils.ImageUtil;
import utils.ImagesMapToFileUtil;
import utils.ResUtil;
import utils.SaveProjectsUtil;
import utils.StrUtil;
import utils.SysUtil;
import utils.ToastUtil;
import utils.UIUtil;
import utils.ProgressTask.ProgressTask;
import views.MyToolTip;

/**
 * 点击项目进入，图片列表界面controller
 * 
 * @author DP
 *
 */
public class ImageListController implements Initializable {

	public static final int MAX_SHOW_DELETENUM = 100000;//暂时放很大
	@FXML
	HBox hbox_location;
	@FXML
	JFXRadioButton radioButton_file;
	@FXML
	JFXRadioButton radioButton_img;
	@FXML
	Label labelLocation;
	@FXML
	VBox vbox_rightButtons;
	@FXML
	BorderPane root1;
	@FXML
	JFXTextField textField_projectName;

	private ObservableList<ImageBean> listData = FXCollections.observableArrayList();
	@FXML
	TableView<ImageBean> tableView;
	private ToggleGroup group;

	private ProjectBean project;
	private ProgressTask task;
	private TableColumn<ImageBean, String> longtitudeCol;
	private TableColumn<ImageBean, String> latitudeCol;
	private TableColumn<ImageBean, String> heightCol;
	private TableColumn<ImageBean, String> isDeletedCol;
	@FXML
	ImageView imageview;

	private ArrayList<HashMap<String, String>> analysingGps;

	ImageView imageViewPlace = new ImageView(new Image("/resources/wushuju.png"));

	private Callback callBack;
	private GoogleMapFlightLineController flightController;
	@FXML
	Label bottomLabel_all;
	@FXML
	Label bottomLabel_selected;
	@FXML
	Label bottomLabel_deleted;
	private ObservableList<ImageBean> selectedItems;
	@FXML
	JFXButton btn_delete;

	private HashMap<String, Boolean> labelMap = new HashMap<String, Boolean>();// 存放图片删除情况
	private Stage flightStage; // 飞行路径界面的stage

	private int deletedNum;
	private List<? extends ImageBean> initSelectedList;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		initTableView();
		initRadioView();
	}

	private void initView() {
		Stage stage = (Stage) root1.getParent().getScene().getWindow();
		stage.addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, new EventHandler<WindowEvent>() {
			@Override
			public void handle(WindowEvent arg0) {
				if (flightStage != null && flightStage.isShowing()) {
					flightStage.close();
				}
				ImagesMapToFileUtil.saveMap(project, labelMap, null);
			}
		});
	}

	/**
	 * 获取到controller后，调用此方法来初始化显示数据。
	 * 
	 * @param project
	 */
	public void setProjectInfo(ProjectBean project) {
		if (project == null) {
			return;
		}
		initView();
		this.project = project;
		initDataView();
		refreshListData(true);

		Node close = root1.getParent().lookup("#close");
		close.addEventFilter(MouseDragEvent.MOUSE_PRESSED, new EventHandler<Event>() {
			@Override
			public void handle(Event event) {
				int locationFrom = project.getLocationFrom();
				if (locationFrom == 1) {
					String locationPath = project.getProjectLocationFile();
					File locationFile = new File(locationPath);
					if (locationPath == null || "".equals(locationPath) || locationFile == null
							|| !locationFile.exists()) {
						ToastUtil.toast(ResUtil.gs("input_error_location_path"));
						event.consume();
						return;
					}
				}
				project.setProjectName(textField_projectName.getText());
				SaveProjectsUtil.changeProjectData(project, null);
				if (callBack != null) {
					callBack.onProjectChange(project);
				}
			}
		});
	}

	/**
	 * 从label文件中读取删除图片map数据
	 */
	private void initLabelMap() {
		String deletedFilePath = ImagesMapToFileUtil.getDeletedFilePath(project);
		labelMap = ImagesMapToFileUtil.fileToMap(deletedFilePath, project);
		deletedNum = 0;
		if (labelMap != null) {
			for (Entry<String, Boolean> entry : labelMap.entrySet()) {
				Boolean mapValue = entry.getValue();
				if (!mapValue) {
					deletedNum++;
				}
			}
		}
	}

	/**
	 * 解析location文件
	 * 
	 * @param proj
	 */
	private void analysingGps(ProjectBean proj) {
		if (proj != null) {
			analysingGps = GpsUtil.analysingGps(proj);
		}
	}

	/**
	 * 刷新图片数据。
	 */
	private void refreshListData(boolean isClear) {
		refreshDeletedNum();
		if (!isClear) {
			tableView.getColumns().get(0).setVisible(false);
			tableView.getColumns().get(0).setVisible(true);
		} else {
			if (flightStage != null && flightStage.isShowing()) {
				flightStage.close();
			}
			task = new ProgressTask(new ProgressTask.MyTask<Integer>() {
				@Override
				protected void succeeded() {
					super.succeeded();
					refreshDeletedNum();
				}

				@Override
				protected Integer call() {
					if (labelMap == null || labelMap.isEmpty()) {
						initLabelMap();
					}
					File file = new File(project.getProjectDir());
					if (file != null && file.exists()) {
						ArrayList<ImageBean> processList = new ArrayList<ImageBean>();
						File[] itemFiles = file.listFiles();
						for (File item : itemFiles) {
							if (!item.isDirectory() && FileUtil.isImage(item)) {
								// 不是文件夹，并且是图片
								ImageBean imageBean = new ImageBean(item.getAbsolutePath(), item.getName());
								if (project.getLocationFrom() == 0) {
									// 从图片中读取经纬度才解析图片数据
									try {
										ImageUtil.printImageTags(item.getAbsolutePath(), imageBean);
									} catch (ImageProcessingException e) {
										e.printStackTrace();
									} catch (IOException e) {
										e.printStackTrace();
									}
								}
								processList.add(imageBean);
							}
						}
						if (project.getLocationFrom() == 1) {
							analysingGps(project);
							processList = setImageDataFromFile(processList);
						}
						try {
							listData.clear();
							deletedNum = 0;
						} catch (Exception e) {
							e.printStackTrace();
						}
						listData.addAll(processList);
					}
					return 1;
				}
			}, (Stage) root1.getScene().getWindow());
			task.start();
		}
	}

	private void initDataView() {
		if (project != null) {
			int from = project.getLocationFrom();
			if (from == 0) {
				// 图片读入经纬度
				group.selectToggle(radioButton_img);
				labelLocation.setText(project.getProjectLocationFile());
				labelLocation.setTooltip(new MyToolTip(project.getProjectLocationFile()));
			} else {
				// 文件读入经纬度
				group.selectToggle(radioButton_file);
				hbox_location.setDisable(false);
				labelLocation.setText(project.getProjectLocationFile());
				labelLocation.setTooltip(new MyToolTip(project.getProjectLocationFile()));
			}

			textField_projectName.setText(project.getProjectName());
		}
		group.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {
			public void changed(ObservableValue<? extends Toggle> ov, Toggle old_toggle, Toggle new_toggle) {
				if (group.getSelectedToggle() != null) {
					int data = (int) group.getSelectedToggle().getUserData();
					if (data == 0) {
						// 选择图片录入
						hbox_location.setDisable(true);
						if (project != null) {
							project.setLocationFrom(0);
						}
					} else {
						// 选择文件录入
						hbox_location.setDisable(false);
						if (project != null) {
							project.setLocationFrom(1);
						}
					}
					refreshListData(true);
				}
			}
		});
	}

	@FXML
	public void onClickSelectLocation() {
		FileChooserUtil.OpenFileChooserUtil(ResUtil.gs("choose_location_file"), labelLocation,
				new FileChooserUtil.Callback() {
					@Override
					public void onResult(boolean isChoose, File file) {
						if (isChoose) {
							String path = file.getAbsolutePath();
							path = path.replaceAll("\\\\", "/");
							labelLocation.setText(path);
							labelLocation.setTooltip(new MyToolTip(file.getAbsolutePath()));
							if (project != null) {
								project.setLocationFrom(1);
								project.setProjectLocationFile(path);
								refreshListData(true);
							}
						}
					}
				});
	}

	@FXML
	public void onClickHelp() {
		UIUtil.openNoticeDialog(getClass(), ConstSize.Notice_Dialog_Frame_Width, ConstSize.Notice_Dialog_Frame_Height,
				ResUtil.gs("tips"), ResUtil.gs("Text_LocationFile_Notice"), (Stage) root1.getScene().getWindow());
	}

	@FXML
	public void onRecallImg() {
		if (selectedItems == null || selectedItems.size() <= 0) {
			return;
		}
		if (selectedItems.size() == 1) {
			UIUtil.openConfirmDialog(getClass(), ConstSize.Confirm_Dialog_Frame_Width,
					ConstSize.Confirm_Dialog_Frame_Height, ResUtil.gs("imageList_recall_image"),
					ResUtil.gs("imageList_recall_image_confirm", selectedItems.get(0).getName()),
					(Stage) root1.getScene().getWindow(), new CallBack() {
						@Override
						public void onCancel() {
						}

						@Override
						public void onConfirm() {
							ImageBean imageBean = selectedItems.get(0);
							if (flightController != null) {
								flightController.onImageRecallFromImageList(imageBean);
							}
							labelMap.replace(imageBean.getName(), true);
							FileUtil.deleteTxt(project.getProjectDir());
							tableView.getSelectionModel().clearSelection();
							refreshListData(false);
						}
					});
		} else {
			UIUtil.openConfirmDialog(getClass(), ConstSize.Confirm_Dialog_Frame_Width,
					ConstSize.Confirm_Dialog_Frame_Height, ResUtil.gs("imageList_recall_image"),
					ResUtil.gs("imageList_recall_lot_image_confirm", selectedItems.size()),
					(Stage) root1.getScene().getWindow(), new CallBack() {

						@Override
						public void onCancel() {
						}

						@Override
						public void onConfirm() {
							ArrayList<ImageBean> list = new ArrayList<ImageBean>();
							list.addAll(selectedItems);
							if (flightController != null) {
								flightController.onImageRecallFromImageList(list);
							}
							for (ImageBean item : list) {
								labelMap.replace(item.getName(), true);
							}
							FileUtil.deleteTxt(project.getProjectDir());
							tableView.getSelectionModel().clearSelection();
							refreshListData(false);
						}
					});
		}
	}

	@FXML
	public void onDeleteImg() {
		if (selectedItems == null || selectedItems.size() <= 0) {
			return;
		}
		if (selectedItems.size() == 1) {
			UIUtil.openConfirmDialog(getClass(), ConstSize.Confirm_Dialog_Frame_Width,
					ConstSize.Confirm_Dialog_Frame_Height, ResUtil.gs("imageList_remove_image"),
					ResUtil.gs("imageList_remove_image_confirm", selectedItems.get(0).getName()),
					(Stage) root1.getScene().getWindow(), new CallBack() {
						@Override
						public void onCancel() {
						}

						@Override
						public void onConfirm() {
							ImageBean imageBean = selectedItems.get(0);
							if (flightController != null) {
								flightController.onImageDeleteFromImageList(imageBean);
							}
//							FileUtil.deleteImage(imageBean.getPath());
							labelMap.replace(imageBean.getName(), false);
							FileUtil.deleteTxt(project.getProjectDir());
//							listData.remove(imageBean);
							tableView.getSelectionModel().clearSelection();
							refreshListData(false);
						}
					});
		} else {
			UIUtil.openConfirmDialog(getClass(), ConstSize.Confirm_Dialog_Frame_Width,
					ConstSize.Confirm_Dialog_Frame_Height, ResUtil.gs("imageList_remove_image"),
					ResUtil.gs("imageList_remove_lot_image_confirm", selectedItems.size()),
					(Stage) root1.getScene().getWindow(), new CallBack() {

						@Override
						public void onCancel() {
						}

						@Override
						public void onConfirm() {
							ArrayList<ImageBean> deleteList = new ArrayList<ImageBean>();
							deleteList.addAll(selectedItems);
							if (flightController != null) {
								flightController.onImageDeleteFromImageList(deleteList);
							}
//							listData.removeAll(deleteList);
							for (ImageBean item : deleteList) {
//								FileUtil.deleteImage(item.getPath());
								labelMap.replace(item.getName(), false);
							}
							FileUtil.deleteTxt(project.getProjectDir());
							tableView.getSelectionModel().clearSelection();
							refreshListData(false);
						}
					});
		}
	}

	@FXML
	public void onSeeImg() {
		ImageBean selectedItem = tableView.getSelectionModel().getSelectedItem();
		if (selectedItem == null) {
			return;
		}
		if (!SysUtil.exeOpenFile(selectedItem.getPath())) {
			ToastUtil.toast(ResUtil.gs("open_image_error"));
		}
	}

	@SuppressWarnings("unchecked")
	private void initTableView() {
		tableView.setOnKeyPressed(new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent event) {
				if (event.getCode() == KeyCode.DELETE) {
					onDeleteImg();
				}
			}
		});

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
		selectedItems = tableView.getSelectionModel().getSelectedItems();
		selectedItems.addListener(new ListChangeListener<ImageBean>() {
			@Override
			public void onChanged(Change<? extends ImageBean> c) {
				while (c.next()) {
					if (c.wasPermutated()) {
					} else if (c.wasUpdated()) {
					} else {
						int addOrMove = -1;
						List<ImageBean> subListAdd = new ArrayList<ImageBean>();
						List<ImageBean> subListRemove = new ArrayList<ImageBean>();
						if (c.getAddedSize() > 0 && c.wasAdded() && selectedItems.size() > 0) {
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

		tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		TableColumn<ImageBean, String> path = new TableColumn<ImageBean, String>(ResUtil.gs("imageList_image_name"));
		longtitudeCol = new TableColumn<ImageBean, String>(ResUtil.gs("imageList_image_long"));
		latitudeCol = new TableColumn<ImageBean, String>(ResUtil.gs("imageList_image_lat"));
		heightCol = new TableColumn<ImageBean, String>(ResUtil.gs("imageList_image_height"));
		isDeletedCol = new TableColumn<ImageBean, String>(ResUtil.gs("imageList_isDeleted"));
		tableView.getColumns().addAll(path, latitudeCol, longtitudeCol, heightCol, isDeletedCol);
		path.setPrefWidth(130);
		longtitudeCol.setPrefWidth(125);
		latitudeCol.setPrefWidth(125);
		heightCol.setPrefWidth(100);
		isDeletedCol.setPrefWidth(80);

		path.setSortable(false);
		longtitudeCol.setSortable(false);
		latitudeCol.setSortable(false);
		heightCol.setSortable(false);
		isDeletedCol.setSortable(false);
		path.setCellValueFactory(new PropertyValueFactory<ImageBean, String>("name"));
		path.setCellFactory(new javafx.util.Callback<TableColumn<ImageBean, String>, TableCell<ImageBean, String>>() {
			@Override
			public TableCell<ImageBean, String> call(TableColumn<ImageBean, String> param) {
				return new ToolTipTableCell<ImageBean>();
			}
		});
		longtitudeCol.setCellValueFactory(
				new javafx.util.Callback<TableColumn.CellDataFeatures<ImageBean, String>, ObservableValue<String>>() {
					@Override
					public ObservableValue<String> call(CellDataFeatures<ImageBean, String> arg0) {
						SimpleStringProperty re = null;
						try {
							re = new SimpleStringProperty();
							String set;
							if ("".equals(arg0.getValue().getLongitudeRef())) {
								set = arg0.getValue().getLongitude() + "";
							} else {
								set = arg0.getValue().getLongitudeRef() + ":" + arg0.getValue().getLongitude();
							}
							re.set(set);
						} catch (Exception e) {
							re.set("");
						}
						return re;
					}
				});
		longtitudeCol.setCellFactory(
				new javafx.util.Callback<TableColumn<ImageBean, String>, TableCell<ImageBean, String>>() {
					@Override
					public TableCell<ImageBean, String> call(TableColumn<ImageBean, String> param) {
						return new ToolTipTableCell<ImageBean>();
					}
				});
		latitudeCol.setCellValueFactory(
				new javafx.util.Callback<TableColumn.CellDataFeatures<ImageBean, String>, ObservableValue<String>>() {
					@Override
					public ObservableValue<String> call(CellDataFeatures<ImageBean, String> arg0) {
						SimpleStringProperty re = null;
						try {
							re = new SimpleStringProperty();
							if ("".equals(arg0.getValue().getLatitudeRef())) {
								re.set(arg0.getValue().getLatitude() + "");
							} else {
								re.set(arg0.getValue().getLatitudeRef() + ":" + arg0.getValue().getLatitude());
							}
						} catch (Exception e) {
							re.set("");
						}
						return re;
					}
				});
		isDeletedCol.setCellFactory(
				new javafx.util.Callback<TableColumn<ImageBean, String>, TableCell<ImageBean, String>>() {
					@Override
					public TableCell<ImageBean, String> call(TableColumn<ImageBean, String> param) {
						return new TableCell<ImageBean, String>() {
							@Override
							protected void updateItem(String item, boolean empty) {
								super.updateItem(item, empty);
								if (!empty && !StrUtil.isEmpty(item)) {
									setText(item);
									if (item.equals(ResUtil.gs("no"))) {
										setTextFill(Color.RED);
									} else {
										setTextFill(Color.BLUE);
									}
								} else {
									setText("");
									setTextFill(Color.BLUE);
								}
							}
						};
					}
				});
		isDeletedCol.setCellValueFactory(
				new javafx.util.Callback<TableColumn.CellDataFeatures<ImageBean, String>, ObservableValue<String>>() {
					@Override
					public ObservableValue<String> call(CellDataFeatures<ImageBean, String> arg0) {
						SimpleStringProperty re = null;
						try {
							re = new SimpleStringProperty();
							String name = arg0.getValue().getName();
							if (!labelMap.containsKey(name)) {
								re.set("");
							} else {
								boolean isExist = labelMap.get(name);
								if (isExist) {
									re.set(ResUtil.gs("yes"));
								} else {
									re.set(ResUtil.gs("no"));
//									deletedNum++;
								}
							}
						} catch (Exception e) {
							re.set("");
						}
						return re;
					}
				});
		latitudeCol.setCellFactory(
				new javafx.util.Callback<TableColumn<ImageBean, String>, TableCell<ImageBean, String>>() {
					@Override
					public TableCell<ImageBean, String> call(TableColumn<ImageBean, String> param) {
						return new ToolTipTableCell<ImageBean>();
					}
				});
		heightCol.setCellValueFactory(new PropertyValueFactory<ImageBean, String>("height"));
		heightCol.setCellFactory(
				new javafx.util.Callback<TableColumn<ImageBean, String>, TableCell<ImageBean, String>>() {
					@Override
					public TableCell<ImageBean, String> call(TableColumn<ImageBean, String> param) {
						return new ToolTipTableCell<ImageBean>();
					}
				});
		tableView.setItems(listData);

		tableView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<ImageBean>() {
			@Override
			public void changed(ObservableValue<? extends ImageBean> observable, ImageBean oldValue,
					ImageBean newValue) {

//				if(oldValue==null&&selectedItems.size()==1) {
//					if (flightController != null) {
//						flightController.onImageClearFocus(newValue);
//					}
//				}

				if (selectedItems.size() >= 1) {
					initImageView(newValue);
				} else if (selectedItems.size() <= 0) {
					initImageView(null);
				}
				bottomLabel_selected.setText(ResUtil.gs("selected_image_num", selectedItems.size() + ""));
				if (selectedItems.size() > 0) {
					vbox_rightButtons.setDisable(false);
				} else {
					vbox_rightButtons.setDisable(true);
				}
			}
		});
		tableView.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				onTestMouse(event);
			}
		});
		tableView.setPlaceholder(imageViewPlace);
	}

	/**
	 * 初始化图片显示
	 * 
	 * @param newValue
	 */
	private void initImageView(ImageBean newValue) {
		if (newValue == null) {
			imageview.setImage(null);
			return;
		}
		String path = "file:" + newValue.getPath();
		Image image = new Image(path);
		imageview.setImage(image);
		imageview.setFitWidth(140);
		imageview.setSmooth(false);
		imageview.setCache(false);
	}

	/**
	 * 列表双击事件
	 * 
	 * @param event
	 */
	protected void onTestMouse(MouseEvent event) {
		if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
			onSeeImg();
		}
	}

	private void initRadioView() {
		group = new ToggleGroup();
		radioButton_img.setToggleGroup(group);
		radioButton_img.setUserData(0);
		radioButton_file.setToggleGroup(group);
		radioButton_file.setUserData(1);
	}

	/**
	 * 解析图片数据
	 * 
	 * @param listData2
	 */
	private ArrayList<ImageBean> setImageDataFromFile(ArrayList<ImageBean> listData2) {
		return listData2 = GpsUtil.setImageDataFromFile(analysingGps, listData2);
	}

	@FXML
	public void onSeeLine() {
		if(listData==null||listData.isEmpty()) {
			System.out.print("NO imagelist data");
			return;
		}
		if (flightStage != null && flightStage.isShowing()) {
			flightStage.close();
			flightStage = null;
			flightController = null;
		}
		
		if (labelMap == null || labelMap.size() <= 0) {
			ToastUtil.toast(ResUtil.gs("data_error"));
			return;
		}
		tableView.getSelectionModel().clearSelection();

		MyFxmlBean openFrame = UIUtil.openFrame(getClass(), "/application/fxml/GoogleMapFlightLine.fxml",
				ConstSize.Flight_Width, ConstSize.Flight_Height,
				project.getProjectName() + " " + ResUtil.gs("imageList_flight"));
		flightStage = openFrame.getStage();
		flightController = openFrame.getFxmlLoader().getController();
		flightController.setData(listData, labelMap, this, deletedNum);
//		flightController.onImageSelectedFromImageList(initSelectedList, 1);
		flightController.setCallback(new FlightLineCallBack() {
			@Override
			public void onDeleteImage(ImageBean image) {
//				listData.remove(image);
//				FileUtil.deleteImage(image.getPath());
				labelMap.replace(image.getName(), false);
				FileUtil.deleteTxt(project.getProjectDir());
				tableView.getSelectionModel().clearSelection();
				refreshListData(false);
			}

			@Override
			public void onFocusChange(String imageName, boolean isEnter) {
			}

			@Override
			public void onRecallImage(ImageBean image) {
				labelMap.replace(image.getName(), true);
				FileUtil.deleteTxt(project.getProjectDir());
				tableView.getSelectionModel().clearSelection();
				refreshListData(false);
			}

			@Override
			public void onRecallImage(ObservableList<ImageBean> selectedList) {
				for (ImageBean bean : selectedList) {
					labelMap.replace(bean.getName(), true);
				}
				FileUtil.deleteTxt(project.getProjectDir());
				tableView.getSelectionModel().clearSelection();
				refreshListData(false);
			}
		});
	}

	public void setCallBack(Callback callBack) {
		this.callBack = callBack;
	}

	public interface Callback {
		void onProjectChange(ProjectBean project);
	}

	class MyRunnable implements Runnable {
		private List<? extends ImageBean> subListAdd;
		private List<? extends ImageBean> subListRemove;

		public MyRunnable(List<? extends ImageBean> subListAdd, List<ImageBean> subListRemove) {
			this.subListAdd = subListAdd;
			initSelectedList = subListAdd; 
			this.subListRemove = subListRemove;
		}

		public void run() {
			bottomLabel_selected.setText(ResUtil.gs("selected_image_num", selectedItems.size() + ""));
			if (selectedItems != null) {
				if (selectedItems.size() >= 1) {
					vbox_rightButtons.setDisable(false);
				} else if (selectedItems.size() <= 0) {
					vbox_rightButtons.setDisable(true);
					initImageView(null);
				}
			}
//			System.out.println("add;"+subListAdd.size()+"  remo"+subListRemove.size() +"selec"+selectedItems.size());
			Stage stage = (Stage) root1.getScene().getWindow();
			if (stage.isFocused()) {
				if (flightController != null) {
					flightController.onImageSelectedFromImageList(subListRemove, 0);
				}
				if (flightController != null) {
					if (selectedItems.size() > 0) {
						flightController.onImageSelectedFromImageList(subListAdd, 1);
					}
				}
			} else {
//				System.out.println("imageList界面不是焦点");
			}
		}
	}

	public void onImageSelectedFromFlightLine(List<? extends ImageBean> subList, int i) {
		if (subList == null || subList.size() <= 0) {
			return;
		}
		if (i == 1) {
			// 添加
			for (ImageBean item : subList) {
				tableView.getSelectionModel().select(item);
			}
			tableView.scrollTo(subList.get(subList.size() - 1));
		} else {
			// 减少
			for (ImageBean item : subList) {
				int indexOf = listData.indexOf(item);
				tableView.getSelectionModel().clearSelection(indexOf);
			}
		}
	}

	/**
	 * 更新已删除的图片数量
	 */
	private void refreshDeletedNum() {
		if (labelMap.size() >= MAX_SHOW_DELETENUM) {// 如果数量过大，则屏蔽此功能，免得卡顿。
			bottomLabel_deleted.setVisible(false);
			if (flightController != null) {
				flightController.updataDeletedNum(0, false);
			}
			return;
		}
		deletedNum = 0;
		bottomLabel_deleted.setVisible(true);
		if (labelMap != null) {
			for (Entry<String, Boolean> entry : labelMap.entrySet()) {
				Boolean mapValue = entry.getValue();
				if (!mapValue) {
					deletedNum++;
				}
			}
			bottomLabel_deleted.setText(ResUtil.gs("image_deleted_num", listData.size()-deletedNum + ""));
		} else {
			bottomLabel_deleted.setText(ResUtil.gs("image_deleted_num", listData.size()-deletedNum + ""));
		}
		if (flightController != null) {
			flightController.updataDeletedNum(deletedNum, true);
		}
	}
}
