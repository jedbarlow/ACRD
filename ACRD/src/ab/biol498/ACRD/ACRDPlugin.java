package ab.biol498.ACRD;

import java.awt.event.ActionEvent;

import com.clcbio.api.base.algorithm.Algo;
import com.clcbio.api.free.actions.framework.ActionGroup;
import com.clcbio.api.free.actions.framework.ActionGroup.GroupType;
import com.clcbio.api.free.actions.framework.ClcAction;
import com.clcbio.api.free.actions.framework.StaticActionGroupDefinitions;
import com.clcbio.api.free.algorithm.AlgoAction;
import com.clcbio.api.free.datatypes.ClcObject;
import com.clcbio.api.free.datatypes.bioinformatics.sequence.NucleotideSequence;
import com.clcbio.api.free.datatypes.bioinformatics.sequencecluster.SequenceCluster;
import com.clcbio.api.free.gui.components.MultiSelectClassRestrictor;
import com.clcbio.api.free.gui.components.MultiSelectRestrictor;
import com.clcbio.api.free.gui.dialog.ClcMessages;
import com.clcbio.api.free.workbench.WorkbenchManager;

public class ACRDPlugin extends AlgoAction {
    private static final long serialVersionUID = 4200858980932875067L;
    public static String PLUGIN_GROUP = "free";

    static {
        System.out.println("*** ACRD Plugin Loaded ***");
    }

    @Override
    public boolean appliesTo(Class<?>[] typeList) {
        return true;
    }

    @Override
    public String getName() {
        return "Annotated Consensus-Reads Difference";
    }

    @Override
    public int getPreferredMenuLocation() {
        return 0;
    }

    @Override
    public double getVersion() {
        return 0.1;
    }

    @Override
    public String getClassKey() {
        return "acrd_plugin";
    }

    @Override
    public boolean isInMenu() {
        return false;
    }

    @Override
    public boolean isInToolBar() {
        return false;
    }

	@Override
	protected void addToActionGroup() {
		ActionGroup ag = StaticActionGroupDefinitions.TOOLBOX_TOP_GROUP.getChildGroup("flossbio_group");
        if (ag == null) {
        	ag = new ActionGroup(
        			manager,
        			"flossbio_group",
        			"Flossbio Tools",
        			ActionGroup.SUBMENUTYPE,
        			StaticActionGroupDefinitions.TOOLBOX_TOP_GROUP);
        	//StaticActionGroupDefinitions.TOOLBOX_TOP_GROUP.addActionGroup();
        }
        ag.addAction(this);
	}

	@Override
	public Algo createAlgo() {
		return new ACRDAlgo(getManager());
	}

	@Override
	public MultiSelectRestrictor createRestrictor(
			WarningReceptor warningReceptor) {
		return new MultiSelectClassRestrictor(new Class<?>[] { NucleotideSequence.class, SequenceCluster.class }, "Select input");
	}
}
