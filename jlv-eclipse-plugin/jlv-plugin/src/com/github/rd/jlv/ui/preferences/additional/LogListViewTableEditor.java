package com.github.rd.jlv.ui.preferences.additional;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.OwnerDrawLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;

import com.github.rd.jlv.ImageType;
import com.github.rd.jlv.JlvActivator;
import com.github.rd.jlv.log4j.LogConstants;

public class LogListViewTableEditor extends FieldEditor {

//	private final Logger logger = LoggerFactory.getLogger(getClass());

	private static final String DISPLAY_LABEL = "Display";
	private static final String NAME_LABEL = "Name";
	private static final String WIDTH_LABEL = "Width";
	private static final String[] COLUMN_NAMES = { DISPLAY_LABEL, NAME_LABEL, WIDTH_LABEL };

	private static final int DISPLAY_COLUMN_WIDTH = 70;
	private static final int NAME_COLUMN_WIDTH = 120;
	private static final int WIDTH_COLUMN_WIDTH = 120;
	private static final int[] COLUMN_WIDTHS = { DISPLAY_COLUMN_WIDTH, NAME_COLUMN_WIDTH, WIDTH_COLUMN_WIDTH };

	private static final String[] NAMES = {
			LogConstants.LEVEL_FIELD_NAME,
			LogConstants.CATEGORY_FIELD_NAME,
			LogConstants.MESSAGE_FIELD_NAME,
			LogConstants.LINE_FIELD_NAME,
			LogConstants.DATE_FIELD_NAME,
			LogConstants.THROWABLE_FIELD_NAME,
	};

	private TableViewer tableViewer;

	private Composite buttonBox;
	private Button upButton;
	private Button downButton;

	private SelectionListener selectionListener;

	private LogsTableStructureItem[] tableModel;

	private IPreferenceStore store;
	private LogsTableStructurePreferenceManager preferenceManager;

	public LogListViewTableEditor(String name, Composite parent) {
		init(name, "");
		this.store = JlvActivator.getDefault().getPreferenceStore();
		preferenceManager = new LogsTableStructurePreferenceManager(store, name);
		createControl(parent);
	}

	@Override
	public void adjustForNumColumns(int numColumns) {
		((GridData) tableViewer.getControl().getLayoutData()).horizontalSpan = numColumns;
	}

	@Override
	public int getNumberOfControls() {
		return 2; // Table and Button box are 2 controls
	}

	@Override
	public void doFillIntoGrid(Composite parent, int numColumns) {
		tableViewer = getTableViewerControl(parent);
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		tableViewer.getControl().setLayoutData(gridData);

		buttonBox = getButtonBoxControl(parent);
		gridData = new GridData();
		gridData.verticalAlignment = GridData.BEGINNING;
		buttonBox.setLayoutData(gridData);
	}

	@Override
	public void doLoad() {
		doLoad(preferenceManager.loadStructure());
	}

	@Override
	public void doLoadDefault() {
		doLoad(preferenceManager.loadDefaultStructure());
	}

	@Override
	public void doStore() {
		if (tableViewer != null) {
			preferenceManager.storeTableStructure(tableModel);
		}
	}

	private void doLoad(LogsTableStructureItem[] model) {
		if (tableViewer != null) {
			for (int i = 0; i < model.length; i++) {
				tableModel[i] = model[i];
			}
			tableViewer.refresh();
		}
	}

	private SelectionListener getSelectionListener() {
		if (selectionListener == null) {
			createSelectionListener();
		}
		return selectionListener;
	}

	private TableViewer getTableViewerControl(Composite parent) {
		if (tableViewer == null) {
			tableViewer = new TableViewer(parent, SWT.SINGLE | SWT.BORDER | SWT.FULL_SELECTION
					| SWT.HIDE_SELECTION);
			tableViewer.setUseHashlookup(true);
			final Table table = tableViewer.getTable();
			table.setLinesVisible(true);
			table.setHeaderVisible(true);
			table.addSelectionListener(getSelectionListener());
			table.addDisposeListener(new DisposeListener() {
				@Override
				public void widgetDisposed(DisposeEvent event) {
					tableViewer = null;
				}
			});
			createTableColumns(tableViewer);
			tableModel = createTableModel();
			tableViewer.setContentProvider(new ArrayContentProvider());
			tableViewer.setInput(tableModel);
		} else {
			checkParent(tableViewer.getControl(), parent);
		}
		return tableViewer;
	}

	private LogsTableStructureItem[] createTableModel() {
		List<LogsTableStructureItem> modelList = new ArrayList<>();

		for (String name : NAMES) {
			modelList.add(new LogsTableStructureItem(name, 100, true));
		}
		return modelList.toArray(new LogsTableStructureItem[modelList.size()]);
	}

	private void createTableColumns(TableViewer tableViewer) {
		for (int i = 0; i < COLUMN_NAMES.length; i++) {
			TableViewerColumn viewerColumn = new TableViewerColumn(tableViewer, SWT.LEAD);
			viewerColumn.getColumn().setText(COLUMN_NAMES[i]);
			viewerColumn.getColumn().setWidth(COLUMN_WIDTHS[i]);

			switch (COLUMN_NAMES[i]) {
			case DISPLAY_LABEL:
				viewerColumn.setLabelProvider(new DisplayColumnLabelProvider());
				viewerColumn.setEditingSupport(new DisplayCellEditor(tableViewer));
				break;
			case NAME_LABEL:
				viewerColumn.setLabelProvider(new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						LogsTableStructureItem column = (LogsTableStructureItem) element;
						return column.getName();
					}
				});
				break;
			case WIDTH_LABEL:
				viewerColumn.setLabelProvider(new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						LogsTableStructureItem column = (LogsTableStructureItem) element;
						return Integer.toString(column.getWidth());
					}
				});
				viewerColumn.setEditingSupport(new WidthCellEditor(tableViewer));
				break;
			default:
				throw new IllegalArgumentException("No column with such name: " + COLUMN_NAMES[i]
						+ ". Only [Name, Width, Display] are allowed.");
			}
		}
	}

	private Composite getButtonBoxControl(Composite parent) {
		if (buttonBox == null) {
			buttonBox = new Composite(parent, SWT.NULL);
			GridLayout layout = new GridLayout();
			layout.marginWidth = 0;
			layout.marginHeight = 0;
			buttonBox.setLayout(layout);
			createButtons(buttonBox);
			buttonBox.addDisposeListener(new DisposeListener() {
				@Override
				public void widgetDisposed(DisposeEvent event) {
					upButton = null;
					downButton = null;
					buttonBox = null;
				}
			});
		} else {
			checkParent(buttonBox, parent);
		}
		selectionChanged();
		return buttonBox;
	}

	private void createButtons(Composite box) {
		upButton = createPushButton(box, "Up");
		downButton = createPushButton(box, "Down");
	}

	private Button createPushButton(Composite parent, String name) {
		Button button = new Button(parent, SWT.PUSH);
		button.setText(name);
		button.addSelectionListener(getSelectionListener());
		GridData gridData = new GridData(SWT.NONE);
		gridData.widthHint = 95;
		button.setLayoutData(gridData);
		return button;
	}

	private void createSelectionListener() {
		selectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				Widget widget = event.widget;

				if (widget == upButton) {
					upPressed();
				} else if (widget == downButton) {
					downPressed();
				} else if (widget == tableViewer.getControl()) {
					selectionChanged();
				}
			}
		};
	}

	private void selectionChanged() {
		int index = tableViewer.getTable().getSelectionIndex();
		int size = tableViewer.getTable().getItemCount();

		upButton.setEnabled(size > 1 && index > 0);
		downButton.setEnabled(size > 1 && index >= 0 && index < size - 1);
	}

	private void upPressed() {
		swap(true);
	}

	private void downPressed() {
		swap(false);
	}

	private void swap(boolean up) {
		int index = tableViewer.getTable().getSelectionIndex();
		int target = up ? index - 1 : index + 1;

		if (index >= 0) {
			LogsTableStructureItem currentModel = tableModel[index];
			tableModel[index] = tableModel[target];
			tableModel[target] = currentModel;
			tableViewer.refresh();
			tableViewer.getTable().setSelection(target);
		}
		selectionChanged();
	}

	private static class DisplayColumnLabelProvider extends OwnerDrawLabelProvider {

		@Override
		protected void measure(Event event, Object element) {
			// no code
		}

		@Override
		protected void paint(Event event, Object element) {
			TableItem item = (TableItem) event.item;
			LogsTableStructureItem column = (LogsTableStructureItem) item.getData();
			Image image = (column.isDisplay()) ? JlvActivator.getImage(ImageType.CHECKBOX_CHECKED)
					: JlvActivator.getImage(ImageType.CHECKBOX_UNCHECKED);
			Rectangle bounds = item.getBounds(event.index);
			Rectangle imageBounds = image.getBounds();
			int xOffset = bounds.width / 2 - imageBounds.width / 2;
			int yOffset = bounds.height / 2 - imageBounds.height / 2;
			int x = xOffset > 0 ? bounds.x + xOffset : bounds.x;
			int y = yOffset > 0 ? bounds.y + yOffset : bounds.y;
			event.gc.drawImage(image, x, y);
		}
	}

	private static class WidthCellEditor extends EditingSupport {

		private TableViewer viewer;

		public WidthCellEditor(TableViewer viewer) {
			super(viewer);
			this.viewer = viewer;
		}

		@Override
		protected CellEditor getCellEditor(Object element) {
			CellEditor cellEditor = new TextCellEditor(viewer.getTable());

			((Text) cellEditor.getControl()).addVerifyListener(new VerifyListener() {
				@Override
				public void verifyText(final VerifyEvent e) {
					e.doit = e.text.matches("[\\d]*");
				}
			});
			return cellEditor;
		}

		@Override
		protected boolean canEdit(Object element) {
			return true;
		}

		@Override
		protected Object getValue(Object element) {
			LogsTableStructureItem model = (LogsTableStructureItem) element;
			return Integer.toString(model.getWidth());
		}

		@Override
		protected void setValue(Object element, Object value) {
			LogsTableStructureItem model = (LogsTableStructureItem) element;
			int width = Integer.valueOf((String) value);
			model.setWidth(width);
			viewer.update(element, null);
		}
	}

	private static class DisplayCellEditor extends EditingSupport {

		private final TableViewer viewer;

		public DisplayCellEditor(TableViewer viewer) {
			super(viewer);
			this.viewer = viewer;
		}

		@Override
		protected CellEditor getCellEditor(final Object element) {
			return new CheckboxCellEditor(viewer.getTable(), SWT.CHECK | SWT.CENTER);
		}

		@Override
		protected boolean canEdit(final Object element) {
			return true;
		}

		@Override
		protected Object getValue(final Object element) {
			LogsTableStructureItem model = (LogsTableStructureItem) element;
			return model.isDisplay();
		}

		@Override
		protected void setValue(final Object element, final Object value) {
			LogsTableStructureItem model = (LogsTableStructureItem) element;
			model.setDisplay((Boolean) value);
			viewer.update(element, null);
		}
	}
}