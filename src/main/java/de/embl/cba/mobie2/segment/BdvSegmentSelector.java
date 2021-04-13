package de.embl.cba.mobie2.segment;

import bdv.util.BdvHandle;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.mobie.n5.source.LabelSource;
import de.embl.cba.mobie2.bdv.BdvMousePositionProvider;
import de.embl.cba.mobie2.display.SegmentationDisplay;
import de.embl.cba.tables.select.SelectionModel;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import net.imglib2.RandomAccess;
import net.imglib2.RealPoint;
import net.imglib2.type.numeric.RealType;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;

import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;

public class BdvSegmentSelector implements Runnable
{
	private BdvHandle bdvHandle;
	private boolean is2D;
	private Supplier< Collection< SegmentationDisplay > > segmentationDisplaySupplier;

	public BdvSegmentSelector( BdvHandle bdvHandle, boolean is2D, Supplier< Collection< SegmentationDisplay > > segmentationDisplaySupplier )
	{
		this.bdvHandle = bdvHandle;
		this.is2D = is2D;
		this.segmentationDisplaySupplier = segmentationDisplaySupplier;
	}


	private synchronized void toggleSelectionAtMousePosition()
	{
		final BdvMousePositionProvider positionProvider = new BdvMousePositionProvider( bdvHandle );
		final int timePoint = positionProvider.getTimePoint();
		final RealPoint position = positionProvider.getPosition();

		final Collection< SegmentationDisplay > segmentationDisplays = segmentationDisplaySupplier.get();

		for ( SegmentationDisplay segmentationDisplay : segmentationDisplays )
		{
			for ( SourceAndConverter< ? > sourceAndConverter : segmentationDisplay.sourceAndConverters )
			{
				if ( SourceAndConverterHelper.isPositionWithinSourceInterval( sourceAndConverter, position, timePoint, is2D ) )
				{
					final Source< ? > source = sourceAndConverter.getSpimSource();
					final double labelIndex = getPixelValue( timePoint, position, source );
					if ( labelIndex == 0 ) return;

					final TableRowImageSegment segment = segmentationDisplay.segmentAdapter.getSegment( labelIndex, timePoint, source.getName() );

					segmentationDisplay.selectionModel.toggle( segment );
					if ( segmentationDisplay.selectionModel.isSelected( segment ) )
					{
						segmentationDisplay.selectionModel.focus( segment );
					}
				}
			}
		}
	}

	private static double getPixelValue( int timePoint, RealPoint position, Source< ? > source )
	{
		final RandomAccess< RealType > randomAccess = ( RandomAccess< RealType > ) source.getSource( timePoint, 0 ).randomAccess();
		final long[] positionInSource = SourceAndConverterHelper.getVoxelPositionInSource( source, position, timePoint, 0 );
		randomAccess.setPosition( positionInSource );
		final double labelIndex = randomAccess.get().getRealDouble();
		return labelIndex;
	}

	@Override
	public void run()
	{
		toggleSelectionAtMousePosition();
	}
}
