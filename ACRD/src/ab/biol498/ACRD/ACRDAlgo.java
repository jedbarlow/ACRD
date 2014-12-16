package ab.biol498.ACRD;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.clcbio.api.base.algorithm.Algo;
import com.clcbio.api.base.algorithm.AlgoException;
import com.clcbio.api.base.algorithm.CallableExecutor;
import com.clcbio.api.base.algorithm.OutputHandler;
import com.clcbio.api.base.algorithm.parameter.keys.Keys;
import com.clcbio.api.base.session.ApplicationContext;
import com.clcbio.api.base.util.NoRemovalIterator;
import com.clcbio.api.base.util.iterator.MovableIntegerIterator;
import com.clcbio.api.free.datatypes.ClcObject;
import com.clcbio.api.free.datatypes.GeneralClcTabular;
import com.clcbio.api.free.datatypes.Tabular;
import com.clcbio.api.free.datatypes.bioinformatics.sequence.BasicSequenceAccessible;
import com.clcbio.api.free.datatypes.bioinformatics.sequence.BasicSequenceAccessor;
import com.clcbio.api.free.datatypes.bioinformatics.sequence.NucleotideSequence;
import com.clcbio.api.free.datatypes.bioinformatics.sequence.Sequence;
import com.clcbio.api.free.datatypes.bioinformatics.sequence.alignment.AlignmentSequenceIndexer;
import com.clcbio.api.free.datatypes.bioinformatics.sequence.alphabet.Alphabet;
import com.clcbio.api.free.datatypes.bioinformatics.sequence.index.BasicIndexer;
import com.clcbio.api.free.datatypes.bioinformatics.sequence.index.DefaultBasicIndexer;
import com.clcbio.api.free.datatypes.bioinformatics.sequence.index.DefaultSingleIndexer;
import com.clcbio.api.free.datatypes.bioinformatics.sequence.index.IndexSequenceLocator;
import com.clcbio.api.free.datatypes.bioinformatics.sequence.index.MultiIndexer;
import com.clcbio.api.free.datatypes.bioinformatics.sequence.index.SingleIndexer;
import com.clcbio.api.free.datatypes.bioinformatics.sequence.list.NucleotideSequenceList;
import com.clcbio.api.free.datatypes.bioinformatics.sequence.region.Region;
import com.clcbio.api.free.datatypes.bioinformatics.sequencecluster.AssemblySequenceCluster;
import com.clcbio.api.free.datatypes.bioinformatics.sequencecluster.AssemblySequenceClusterSource;
import com.clcbio.api.free.datatypes.bioinformatics.sequencecluster.BasicLocalAlignment;
import com.clcbio.api.free.datatypes.bioinformatics.sequencecluster.ScatteredLocalAlignment;
import com.clcbio.api.free.datatypes.bioinformatics.sequencecluster.SequenceCluster;
import com.clcbio.api.free.datatypes.bioinformatics.sequencecluster.SequenceClusterConsensus;
import com.clcbio.api.free.datatypes.bioinformatics.sequencecluster.index.SequenceClusterIndexer;
import com.clcbio.api.free.datatypes.framework.permission.PermissionController;
import com.clcbio.api.free.datatypes.framework.permission.Permissions;
import com.clcbio.api.free.gui.dialog.ClcMessages;
import com.clcbio.api.free.workbench.WorkbenchManager;
import com.clcbio.api.free.datatypes.bioinformatics.sequence.feature.Feature;

public class ACRDAlgo extends Algo implements PermissionController {
	WorkbenchManager workbookManager;
	NucleotideSequence annotatedSequence;
	List<String> names;
	List<Region> regions;
	List<Integer> errorCounts;

	public ACRDAlgo(WorkbenchManager wm) {
		super(wm);
		workbookManager = wm;
	}

	@Override
	public void calculate(OutputHandler handler, CallableExecutor executor)
			throws AlgoException, InterruptedException {

		if (this.getInputObjectsCount() != 2) {
			displayError("Please select one annotated nucleotide sequence, and the corresponding consensus sequence (containing the mapped reads).");
			return;
		}

		names = new ArrayList<String>();
		regions = new ArrayList<Region>();
		errorCounts = new ArrayList<Integer>();

		NucleotideSequence annotatedSequence = null;
		SequenceCluster consensusWithMappedReads = null;

		for (ClcObject o: this.getInputObjectsIteratorProvider()) {
			if (o instanceof NucleotideSequence) {
				annotatedSequence = (NucleotideSequence) o;
			}
		}
		for (ClcObject o: this.getInputObjectsIteratorProvider()) {
			if (o instanceof SequenceCluster) {
				consensusWithMappedReads = (SequenceCluster)o;
			}
		}

		if (annotatedSequence == null) {
			displayError("Please select an annotated version of the consensus sequence.");
			return;
		}
		if (consensusWithMappedReads == null) {
			displayError("Please select an the consensus sequence (containing the mapped reads).");
			return;
		}
		processAnnotations(annotatedSequence);
		processConsensus(consensusWithMappedReads, handler);
	}

	private void processAnnotations(NucleotideSequence annotatedSequence) {
		Iterator<Feature> it = annotatedSequence.getFeatureIterator();
		while (it.hasNext()) {
			Feature f = it.next();
			names.add(f.getName());
			regions.add(f.getRegion());
			errorCounts.add(0);
		}
	}

	private void processConsensus(SequenceCluster sc, OutputHandler handler)
			throws AlgoException, InterruptedException {
		Sequence s = sc.getMainSequence();

		for(ScatteredLocalAlignment match : sc.getMatches()) {
			for (int i = 0; i < match.getChildLocalAlignmentCount(); i++) {
				BasicLocalAlignment alignment = match.getChildLocalAlignment(i);
				MovableIntegerIterator itAlignment = alignment.getMatchSequencePositionIterator();
				MovableIntegerIterator itMain = alignment.getMainSequencePositionIterator();
				Sequence matchSequence = match.getSequence();
				while (itMain.hasNext()) {
					int consensusIndex = itMain.next();
					int alignmentIndex = itAlignment.next();
					if (alignmentIndex >= 0 && consensusIndex >= 0) {
						int consensusSymbol = s.getSymbolIndexAt(consensusIndex);
						int alignedSymbol = matchSequence.getSymbolIndexAt(alignmentIndex);
						if (consensusSymbol != alignedSymbol) {
							incrementTouched(consensusIndex);
						}
					}
				}
			}
		}

		handler.postOutputObjects(Collections.singletonList(renderTable()), this);
	}

	private GeneralClcTabular renderTable() {
		GeneralClcTabular.Builder builder = GeneralClcTabular.createBuilder(
				"ACRD Results",
				"Number of different residues, per annotated region, between consensus and mapped reads",
				new String[] {
						"Annotation",
						"Difference Count",
						"Length",
						"Differences/Length",
				});
		for (int i = 0; i < names.size(); i++) {
			builder.addRow(
					names.get(i),
					errorCounts.get(i),
					regions.get(i).getSize(),
					((double)errorCounts.get(i))/((double)regions.get(i).getSize()));
		}

		return builder.finish();
	}

	private void incrementTouched(int pos) {
		for (int i = 0; i < names.size(); i++) {
			if (regions.get(i).isInRegion(pos, false)) {
				errorCounts.set(i, 1 + errorCounts.get(i));
			}
		}
	}

	private void displayError(String msg) {
		ClcMessages.showInformation(
				workbookManager.getCurrentMainFrame(),
				msg,
				"ACRD Plugin - Error");
	}

	public Permissions getDeniedPermissions(ClcObject clcObject, Object permissionSeeker) {
		return Permissions.union(Permissions.SEQUENCE_CHANGE_RESIDUES, Permissions.SEQUENCE_INSERT_DELETE_RESIDUES);
	}

	public Permissions getReservedPermissions(ClcObject clcObject, Object permissionSeeker) {
		return Permissions.NO_PERMISSIONS;
	}

	public String getReason() {
		return "Searching for Open Reading Frames";
	}

	@Override
	public String getClassKey() {
		return "acrd_algo";
	}

	@Override
	public String getName() {
		return "ACRD";
	}

	@Override
	public double getVersion() {
		return 0.0;
	}

	@Override
	public Permissions getNotifyPermissions(ClcObject clcObject,
			Object permissionSeeker) {
		return Permissions.NO_PERMISSIONS;
	}

	@Override
	public Permissions getWarnPermissions(ClcObject clcObject,
			Object permissionSeeker) {
		return Permissions.NO_PERMISSIONS;
	}

}
