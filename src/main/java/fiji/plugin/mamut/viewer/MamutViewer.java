package fiji.plugin.mamut.viewer;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.Collection;
import java.util.Map;

import javax.swing.JFrame;

import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.ui.TransformEventHandler3D;

import viewer.SpimViewer;
import viewer.TextOverlayAnimator;
import viewer.render.SourceAndConverter;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.visualization.TrackMateModelView;

public class MamutViewer extends SpimViewer implements TrackMateModelView {

	protected static final long DEFAULT_TEXT_DISPLAY_DURATION = 3000;
	private static final String INFO_TEXT = "A viewer based on Tobias Pietsch SPIM Viewer";
	private MamutOverlay overlay;
	private TextOverlayAnimator animatedOverlay = null;
	private final Logger logger;
	private final TrackMateModel model;
	private TranslationAnimator currentAnimator = null;

	/*
	 * CONSTRUCTOR
	 */


	public MamutViewer(int width, int height, Collection<SourceAndConverter<?>> sources, int numTimePoints, TrackMateModel model) {
		super(width, height, sources, numTimePoints);
		this.model = model;
		this.logger = new MamutViewerLogger(); 
	}

	/*
	 * METHODS
	 */

	/**
	 * Returns the {@link Logger} object that will echo any message to this {@link MamutViewer}
	 * window.
	 * @return this {@link MamutViewer} logger.
	 */
	public Logger getLogger() {
		return logger;
	}


	@Override
	public void drawOverlays(Graphics g) {
		super.drawOverlays(g);

		if (null != overlay) {
			overlay.setViewerState(state);
			overlay.paint((Graphics2D) g);
		}

		if ( animatedOverlay  != null ) {
			animatedOverlay.paint( ( Graphics2D ) g, System.currentTimeMillis() );
			if ( animatedOverlay.isComplete() )
				animatedOverlay = null;
			else
				display.repaint();
		}
	}

	/**
	 * Returns the {@link JFrame} component that is the parent to this viewer. 
	 * @return  the parent JFrame.
	 */
	public JFrame getFrame() {
		return frame;
	}

	/**
	 * Returns the time-point currently displayed in this viewer.
	 * @return the time-point currently displayed.
	 */
	public int getCurrentTimepoint() {
		return state.getCurrentTimepoint();
	}

	@Override
	public String getInfoText() {
		return INFO_TEXT;
	}

	@Override
	public void render() {
		this.overlay = new MamutOverlay(model);
	}

	@Override
	public void refresh() {
		requestRepaint();
	}

	@Override
	public void clear() {
		this.overlay = null;
	}

	@Override
	public void centerViewOn(Spot spot) {
		
		AffineTransform3D t = new AffineTransform3D();
		state.getViewerTransform(t);
		double[] spotCoords = new double[] {
				spot.getFeature(Spot.POSITION_X),	
				spot.getFeature(Spot.POSITION_Y),	
				spot.getFeature(Spot.POSITION_Z)	
		};

		double dx = frame.getWidth()/2 - ( t.get(0, 0) * spotCoords[0] + t.get(0, 1) * spotCoords[1] + t.get(0, 2) * spotCoords[2]);
		double dy = frame.getHeight()/2 - ( t.get(1, 0) * spotCoords[0] + t.get(1, 1) * spotCoords[1] + t.get(1, 2) * spotCoords[2]);
		double dz = - ( t.get(2, 0) * spotCoords[0] + t.get(2, 1) * spotCoords[1] + t.get(2, 2) * spotCoords[2]);
		
		double[] target = new double[] { dx, dy, dz };
		currentAnimator = new TranslationAnimator( t, target, 300 );
		currentAnimator.setTime( System.currentTimeMillis() );
		transformChanged(t);
	}

	
	@Override
	public void paint() {

		synchronized( this )
		{
			if ( currentAnimator != null )
			{
				final TransformEventHandler3D handler = display.getTransformEventHandler();
				final AffineTransform3D transform = currentAnimator.getCurrent( System.currentTimeMillis() );
				handler.setTransform( transform );
				transformChanged( transform );
				if ( currentAnimator.isComplete() )
					currentAnimator = null;
			}
		}
		
		super.paint();
	}
	
	@Override
	public Map<String, Object> getDisplaySettings() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setDisplaySettings(String key, Object value) {
		// TODO Auto-generated method stub

	}

	@Override
	public Object getDisplaySettings(String key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TrackMateModel getModel() {
		return model;

	}



	private final class MamutViewerLogger extends Logger {

		@Override
		public void setStatus(String status) {
			animatedOverlay = new TextOverlayAnimator(status, DEFAULT_TEXT_DISPLAY_DURATION);
		}

		@Override
		public void setProgress(double val) {
			animatedOverlay = new TextOverlayAnimator(String.format("%3d", Math.round(val)), DEFAULT_TEXT_DISPLAY_DURATION);
		}

		@Override
		public void log(String message, Color color) {
			animatedOverlay = new TextOverlayAnimator(message, DEFAULT_TEXT_DISPLAY_DURATION);
		}

		@Override
		public void error(String message) {
			animatedOverlay = new TextOverlayAnimator(message, DEFAULT_TEXT_DISPLAY_DURATION);
		}

	}


}