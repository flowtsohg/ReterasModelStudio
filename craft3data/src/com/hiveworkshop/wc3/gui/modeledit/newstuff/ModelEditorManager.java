package com.hiveworkshop.wc3.gui.modeledit.newstuff;

import java.util.Arrays;
import java.util.Collection;

import com.hiveworkshop.wc3.gui.ProgramPreferences;
import com.hiveworkshop.wc3.gui.animedit.NodeAnimationModelEditor;
import com.hiveworkshop.wc3.gui.animedit.NodeAnimationSelectionManager;
import com.hiveworkshop.wc3.gui.modeledit.actions.newsys.ModelStructureChangeListener;
import com.hiveworkshop.wc3.gui.modeledit.newstuff.listener.ModelEditorChangeListener;
import com.hiveworkshop.wc3.gui.modeledit.selection.SelectionItemTypes;
import com.hiveworkshop.wc3.gui.modeledit.selection.SelectionListener;
import com.hiveworkshop.wc3.gui.modeledit.selection.SelectionMode;
import com.hiveworkshop.wc3.gui.modeledit.selection.SelectionView;
import com.hiveworkshop.wc3.gui.modeledit.toolbar.ToolbarButtonGroup;
import com.hiveworkshop.wc3.mdl.render3d.RenderModel;
import com.hiveworkshop.wc3.mdl.Vertex;
import com.hiveworkshop.wc3.mdl.v2.ModelView;

public final class ModelEditorManager {
	private final ModelView model;
	private final ProgramPreferences programPreferences;
	private ModelEditor modelEditor;
	private final ViewportSelectionHandlerImpl viewportSelectionHandler;
	private final ModelEditorChangeListener modelEditorChangeListener;
	private SelectionView selectionView;
	private final SelectionListener selectionListener;
	private final RenderModel renderModel;
	private NodeAnimationSelectionManager nodeAnimationSelectionManager;
	private final ModelStructureChangeListener structureChangeListener;
	public static boolean MOVE_LINKED;

	public ModelEditorManager(final ModelView model, final ProgramPreferences programPreferences,
			final ToolbarButtonGroup<SelectionMode> modeButtonGroup,
			final ModelEditorChangeListener modelEditorChangeListener, final SelectionListener selectionListener,
			final RenderModel renderModel, final ModelStructureChangeListener structureChangeListener) {
		this.model = model;
		this.modelEditorChangeListener = modelEditorChangeListener;
		this.programPreferences = programPreferences;
		this.selectionListener = selectionListener;
		this.renderModel = renderModel;
		this.structureChangeListener = structureChangeListener;
		viewportSelectionHandler = new ViewportSelectionHandlerImpl(modeButtonGroup, null);
		setSelectionItemType(SelectionItemTypes.VERTEX);
	}

	public void setSelectionItemType(final SelectionItemTypes selectionMode) {
		final Collection<? extends Vertex> lastSelectedVertices;
		if (selectionView != null) {

			lastSelectedVertices = selectionView.getSelectedVertices();
		} else {
			lastSelectedVertices = null;
		}
		switch (selectionMode) {
		case FACE: {
			final FaceSelectionManager selectionManager = new FaceSelectionManager();
			final PivotPointSelectionManager pivotSelectionManager = new PivotPointSelectionManager();
			final ModelEditorNotifier modelEditorNotifier = new ModelEditorNotifier();
			final FaceModelEditor faceModelEditor = new FaceModelEditor(model, programPreferences, selectionManager,
					structureChangeListener);
			modelEditorNotifier.subscribe(faceModelEditor);
			final PivotPointModelEditor pivotPointModelEditor = new PivotPointModelEditor(model, programPreferences,
					pivotSelectionManager, structureChangeListener);
			modelEditorNotifier.subscribe(pivotPointModelEditor);
			modelEditor = modelEditorNotifier;
			if (lastSelectedVertices != null) {
				modelEditor.selectByVertices(lastSelectedVertices);
			}
			viewportSelectionHandler.setSelectingEventHandler(modelEditor);
			modelEditorChangeListener.modelEditorChanged(modelEditor);
			selectionView = new MultiPartSelectionView(Arrays.asList(selectionManager, pivotSelectionManager));
			selectionListener.onSelectionChanged(selectionView);
			nodeAnimationSelectionManager = null;
			modelEditorNotifier.setCloneContextHelper(new CloneContextHelper(model, structureChangeListener,
					faceModelEditor.getVertexSelectionHelper(), pivotPointModelEditor.getVertexSelectionHelper(),
					pivotPointModelEditor.selectionManager, faceModelEditor.selectionManager));
			break;
		}
		case GROUP: {
			final VertexGroupSelectionManager selectionManager = new VertexGroupSelectionManager();
			final PivotPointSelectionManager pivotSelectionManager = new PivotPointSelectionManager();
			final ModelEditorNotifier modelEditorNotifier = new ModelEditorNotifier();
			final VertexGroupModelEditor vertexGroupModelEditor = new VertexGroupModelEditor(model, programPreferences,
					selectionManager, structureChangeListener);
			modelEditorNotifier.subscribe(vertexGroupModelEditor);
			final PivotPointModelEditor pivotPointModelEditor = new PivotPointModelEditor(model, programPreferences,
					pivotSelectionManager, structureChangeListener);
			modelEditorNotifier.subscribe(pivotPointModelEditor);
			modelEditor = modelEditorNotifier;
			if (lastSelectedVertices != null) {
				modelEditor.selectByVertices(lastSelectedVertices);
			}
			viewportSelectionHandler.setSelectingEventHandler(modelEditor);
			modelEditorChangeListener.modelEditorChanged(modelEditor);
			selectionView = new MultiPartSelectionView(Arrays.asList(selectionManager, pivotSelectionManager));
			selectionListener.onSelectionChanged(selectionView);
			nodeAnimationSelectionManager = null;
			modelEditorNotifier.setCloneContextHelper(new CloneContextHelper(model, structureChangeListener,
					vertexGroupModelEditor.getVertexSelectionHelper(), pivotPointModelEditor.getVertexSelectionHelper(),
					pivotPointModelEditor.selectionManager, vertexGroupModelEditor.selectionManager));
			break;
		}
		case ANIMATE: {
			nodeAnimationSelectionManager = new NodeAnimationSelectionManager(renderModel);
			final NodeAnimationModelEditor nodeAnimationModelEditor = new NodeAnimationModelEditor(model,
					programPreferences, nodeAnimationSelectionManager, renderModel, structureChangeListener);
			modelEditor = nodeAnimationModelEditor;
			if (lastSelectedVertices != null) {
				modelEditor.selectByVertices(lastSelectedVertices);
			}
			viewportSelectionHandler.setSelectingEventHandler(modelEditor);
			modelEditorChangeListener.modelEditorChanged(modelEditor);
			selectionView = nodeAnimationSelectionManager;
			selectionListener.onSelectionChanged(selectionView);
			break;
		}
		case TPOSE: {
			final boolean moveLinked = MOVE_LINKED;// dialog == settings[0];
			final TPoseSelectionManager tposeSelectionManager = new TPoseSelectionManager(model, moveLinked);
			final TPoseModelEditor tPoseModelEditor = new TPoseModelEditor(model, programPreferences,
					tposeSelectionManager, structureChangeListener);
			modelEditor = tPoseModelEditor;
			if (lastSelectedVertices != null) {
				modelEditor.selectByVertices(lastSelectedVertices);
			}
			viewportSelectionHandler.setSelectingEventHandler(modelEditor);
			modelEditorChangeListener.modelEditorChanged(modelEditor);
			selectionView = tposeSelectionManager;
			selectionListener.onSelectionChanged(selectionView);
			break;
		}
		case CLUSTER: {
			final VertexClusterDefinitions vertexClusterDefinitions = new VertexClusterDefinitions(model.getModel());
			final VertexClusterSelectionManager selectionManager = new VertexClusterSelectionManager(
					vertexClusterDefinitions);
			final PivotPointSelectionManager pivotSelectionManager = new PivotPointSelectionManager();
			final ModelEditorNotifier modelEditorNotifier = new ModelEditorNotifier();
			final VertexClusterModelEditor vertexGroupModelEditor = new VertexClusterModelEditor(model,
					programPreferences, selectionManager, structureChangeListener, vertexClusterDefinitions);
			modelEditorNotifier.subscribe(vertexGroupModelEditor);
			final PivotPointModelEditor pivotPointModelEditor = new PivotPointModelEditor(model, programPreferences,
					pivotSelectionManager, structureChangeListener);
			modelEditorNotifier.subscribe(pivotPointModelEditor);
			modelEditor = modelEditorNotifier;
			if (lastSelectedVertices != null) {
				modelEditor.selectByVertices(lastSelectedVertices);
			}
			viewportSelectionHandler.setSelectingEventHandler(modelEditor);
			modelEditorChangeListener.modelEditorChanged(modelEditor);
			selectionView = new MultiPartSelectionView(Arrays.asList(selectionManager, pivotSelectionManager));
			selectionListener.onSelectionChanged(selectionView);
			nodeAnimationSelectionManager = null;
			modelEditorNotifier.setCloneContextHelper(new CloneContextHelper(model, structureChangeListener,
					vertexGroupModelEditor.getVertexSelectionHelper(), pivotPointModelEditor.getVertexSelectionHelper(),
					pivotPointModelEditor.selectionManager, vertexGroupModelEditor.selectionManager));
			break;
		}
		default:
		case VERTEX: {
			final GeosetVertexSelectionManager selectionManager = new GeosetVertexSelectionManager();
			final PivotPointSelectionManager pivotSelectionManager = new PivotPointSelectionManager();
			final ModelEditorNotifier modelEditorNotifier = new ModelEditorNotifier();
			final GeosetVertexModelEditor geosetVertexModelEditor = new GeosetVertexModelEditor(model,
					programPreferences, selectionManager, structureChangeListener);
			modelEditorNotifier.subscribe(geosetVertexModelEditor);
			final PivotPointModelEditor pivotPointModelEditor = new PivotPointModelEditor(model, programPreferences,
					pivotSelectionManager, structureChangeListener);
			modelEditorNotifier.subscribe(pivotPointModelEditor);
			modelEditor = modelEditorNotifier;
			if (lastSelectedVertices != null) {
				modelEditor.selectByVertices(lastSelectedVertices);
			}
			viewportSelectionHandler.setSelectingEventHandler(modelEditor);
			modelEditorChangeListener.modelEditorChanged(modelEditor);
			selectionView = new MultiPartSelectionView(Arrays.asList(selectionManager, pivotSelectionManager));
			selectionListener.onSelectionChanged(selectionView);
			nodeAnimationSelectionManager = null;
			modelEditorNotifier.setCloneContextHelper(new CloneContextHelper(model, structureChangeListener,
					geosetVertexModelEditor.getVertexSelectionHelper(),
					pivotPointModelEditor.getVertexSelectionHelper(), pivotPointModelEditor.selectionManager,
					geosetVertexModelEditor.selectionManager));
			break;
		}
		}
	}

	public ModelEditor getModelEditor() {
		return modelEditor;
	}

	public ViewportSelectionHandlerImpl getViewportSelectionHandler() {
		return viewportSelectionHandler;
	}

	public SelectionView getSelectionView() {
		return selectionView;
	}

	public NodeAnimationSelectionManager getNodeAnimationSelectionManager() {
		return nodeAnimationSelectionManager;
	}
}
