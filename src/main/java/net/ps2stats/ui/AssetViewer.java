package net.ps2stats.ui;

import com.google.common.html.HtmlEscapers;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import lombok.SneakyThrows;
import lombok.val;
import me.nallar.jdds.JDDS;
import net.ps2stats.edit.*;
import net.ps2stats.edit.Paths;
import net.ps2stats.map.MapExporter;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.function.*;
import java.util.regex.*;

public class AssetViewer {
	private static final int MAX_RESULTS = 25000;
	private final JFrame frame;
	private final Console console;
	private Map<String, PackFile.Entry> assetsMap;
	private List<String> assetsList;
	private Paths path;
	private Assets assets;
	private JTextField searchField;
	private JList<String> list;
	private JPanel panel;
	private JLabel label;
	private JScrollPane leftScrollPane;
	private JScrollPane rightScrollPane;
	private JSplitPane splitPane;
	private JButton patchAndRunButton;

	{
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
		$$$setupUI$$$();
	}

	private AssetViewer(Console console, JFrame frame) {
		this.console = console;
		this.frame = frame;
		loadPS2Data();
		Collections.sort(assetsList);
		list.addListSelectionListener((e) -> {
			if (!e.getValueIsAdjusting())
				updateSelection();
		});
		list.addMouseListener(new FileListListener(list));
		searchField.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				handleSearch();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				handleSearch();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				handleSearch();
			}
		});
		leftScrollPane.getVerticalScrollBar().setUnitIncrement(20);
		rightScrollPane.getVerticalScrollBar().setUnitIncrement(20);
		handleSearch();
		patchAndRunButton.addActionListener(e -> launchGame());
		val bar = new MenuBar();
		val menu = new Menu("Options");
		val choosePs2Directory = new MenuItem("Choose PS2 Directory");
		choosePs2Directory.addActionListener((e) -> {
			path.guiSelectPS2Dir(true);
			loadPS2Data();
		});
		menu.add(choosePs2Directory);
		bar.add(menu);
		frame.setMenuBar(bar);
	}

	private static String convertStringForLabel(String stringData) {
		if (stringData.length() > 100000) {
			stringData = stringData.substring(0, 100000);
		}
		stringData = HtmlEscapers.htmlEscaper().escape(stringData);
		stringData = stringData.replace("\r\n", "<br>");
		stringData = stringData.replace("\n", "<br>");
		return "<html>" + stringData + "</html>";
	}

	public static void main(String[] args) {
		val console = Console.create();

		JFrame frame = new JFrame("PS2 Asset Viewer");
		frame.setContentPane(new AssetViewer(console, frame).panel);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
	}

	private void loadPS2Data() {
		path = new Paths();
		assets = new Assets(path, false);
		assetsMap = assets.getFiles();
		assetsList = new ArrayList<>(assetsMap.keySet());
		frame.setTitle("PS2 Asset Viwer - " + (path.isLive() ? "Live" : "Test") + " - " + path.getPs2Dir());
	}

	private void launchGame() {
		new Thread(() -> {
			console.panel.grabFocus();
			new Patcher(path).runGameWithPatches();
		}).start();
	}

	private void handleSearch() {
		List<String> strings = searchAssets(searchField.getText());
		list.setListData(strings.toArray(new String[strings.size()]));
	}

	private List<String> searchAssets(String search) {
		if (search.isEmpty())
			return getReplacements();

		search = search.toLowerCase();
		val results = new ArrayList<String>();
		int count = 0;

		boolean plainSearch = false;
		try {
			val regex = Pattern.compile(search, Pattern.CASE_INSENSITIVE);
			for (String asset : assetsList) {
				if (regex.matcher(asset).find()) {
					results.add(asset);
					if (++count >= MAX_RESULTS)
						break;
				}
			}
		} catch (PatternSyntaxException e) {
			plainSearch = true;
		}

		if (plainSearch) {
			System.out.println("Plain search for: " + search);
			for (String asset : assetsList) {
				if (asset.toLowerCase().contains(search)) {
					results.add(asset);
					if (++count >= MAX_RESULTS)
						break;
				}
			}
		}

		return results;
	}

	private List<String> getReplacements() {
		val list = new ArrayList<String>();

		List<String> replacements = path.getReplacements().getReplacementNames();

		list.add("Showing " + replacements.size() + " replacement files.");
		list.add("Enter text in the search box above to search all PS2 files.");
		list.add("Right click -> edit to open the file for editing.");
		list.add(" ");

		list.addAll(replacements);

		return list;
	}

	private void updateSelection() {
		val selected = list.getSelectedValue();
		val entry = assetsMap.get(selected);

		if (selected == null || selected.isEmpty())
			return;

		String type = getFileType(selected);
		label.setText(null);
		label.setIcon(null);

		if (entry == null)
			return;

		switch (type) {
			case "xml":
			case "txt":
			case "cfg":
			case "adr":
			case "apx":
			case "ini":
			case "props":
				// plain text
				label.setText(convertStringForLabel(entry.getStringData()));
				break;
			case "dds":
				// compressed DDS image
				BufferedImage decompressed;
				synchronized (JDDS.class) {
					decompressed = JDDS.readDDS(entry.getData());
				}
				label.setIcon(new ImageIcon(decompressed));
				break;
			default:
				label.setText("Can not preview this file type");
		}
	}

	private String getFileType(String selected) {
		val i = selected.lastIndexOf('.');
		return selected.substring(i + 1, selected.length()).toLowerCase();
	}

	private void createUIComponents() {
		// TODO: place custom component creation code here
	}

	/**
	 * Method generated by IntelliJ IDEA GUI Designer
	 * >>> IMPORTANT!! <<<
	 * DO NOT edit this method OR call it in your code!
	 *
	 * @noinspection ALL
	 */
	private void $$$setupUI$$$() {
		panel = new JPanel();
		panel.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
		searchField = new JTextField();
		searchField.setToolTipText("File Search");
		panel.add(searchField, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(400, -1), null, 0, false));
		patchAndRunButton = new JButton();
		patchAndRunButton.setText("Patch and Run");
		panel.add(patchAndRunButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		splitPane = new JSplitPane();
		splitPane.setContinuousLayout(false);
		splitPane.setEnabled(true);
		splitPane.setResizeWeight(0.2);
		panel.add(splitPane, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(500, 500), null, 0, false));
		rightScrollPane = new JScrollPane();
		splitPane.setLeftComponent(rightScrollPane);
		list = new JList();
		final DefaultListModel defaultListModel1 = new DefaultListModel();
		list.setModel(defaultListModel1);
		rightScrollPane.setViewportView(list);
		leftScrollPane = new JScrollPane();
		splitPane.setRightComponent(leftScrollPane);
		label = new JLabel();
		label.setText("");
		leftScrollPane.setViewportView(label);
	}

	/**
	 * @noinspection ALL
	 */
	public JComponent $$$getRootComponent$$$() {
		return panel;
	}

	@SneakyThrows
	private void extractForEditing(PackFile.Entry asset) {
		String type = getFileType(asset.getName());
		String outputName = asset.getName().replace('.' + type, "");
		Supplier<byte[]> data = asset::getData;

		switch (type) {
			case "dme":
				outputName += '.' + "obj";
				final Supplier<byte[]> finalData = data;
				data = () -> DMEFile.Companion.saveMesh(finalData.get());
				break;
			default:
				outputName += type;
		}
		File replacement = new File(path.getConfigDir(), outputName);

		if (!replacement.exists()) {
			Files.write(replacement.toPath(), data.get());
		}
	}

	private class FileListListener implements MouseListener {
		private final JList<String> list;
		private final JPopupMenu menu;
		private final JMenuItem exportMap;

		FileListListener(JList<String> list) {
			this.list = list;
			this.menu = new JPopupMenu();
			exportMap = menuItem("Export Map", this::exportMap);

			menu.add(menuItem("Edit", (e) -> {
				List<String> selectedFiles = list.getSelectedValuesList();
				List<PackFile.Entry> entryList = new ArrayList<>();

				for (String selectedFile : selectedFiles) {
					PackFile.Entry asset = assetsMap.get(selectedFile);

					if (asset == null) {
						continue;
					}

					entryList.add(asset);
				}

				if (Desktop.isDesktopSupported()) {
					try {
						Desktop.getDesktop().open(path.getConfigDir());
					} catch (IOException e1) {
						throw new RuntimeException(e1);
					}
				}
				assets.forEntries(entryList, AssetViewer.this::extractForEditing);
			}));
		}

		private JMenuItem menuItem(String name, ActionListener l) {
			JMenuItem item = new JMenuItem(name);
			item.addActionListener(l);
			return item;
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			if (!SwingUtilities.isRightMouseButton(e))
				return;

			if (list.getSelectedIndices().length <= 1)
				list.setSelectedIndex(list.locationToIndex(e.getPoint()));

			menu.remove(exportMap);
			if (list.getSelectedValue() != null &&
				list.getSelectedValue().toLowerCase().contains("tile_") &&
				list.getSelectedValue().toLowerCase().contains("lod0")) {
				menu.add(exportMap);
			}

			menu.show(e.getComponent(), e.getX(), e.getY());
		}

		private void exportMap(@SuppressWarnings("unused") ActionEvent actionEvent) {
			String selectedItem = list.getSelectedValue();
			String name = selectedItem.substring(0, selectedItem.toLowerCase().indexOf("_tile"));

			new Thread(() -> {
				MapExporter exporter = new MapExporter(assets, path);
				exporter.saveMap(name, "png", new File(path.getConfigDir(), name + " map.png"));
			}).start();
		}

		@Override
		public void mousePressed(MouseEvent e) {
		}

		@Override
		public void mouseReleased(MouseEvent e) {
		}

		@Override
		public void mouseEntered(MouseEvent e) {
		}

		@Override
		public void mouseExited(MouseEvent e) {
		}
	}
}
