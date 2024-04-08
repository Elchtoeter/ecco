package at.jku.isse.ecco.storage.neo4j.domain;

import at.jku.isse.ecco.artifact.Artifact;
import at.jku.isse.ecco.artifact.ArtifactData;
import at.jku.isse.ecco.artifact.ArtifactReference;
import at.jku.isse.ecco.pog.PartialOrderGraph;
import at.jku.isse.ecco.tree.Node;
import org.eclipse.collections.impl.factory.Maps;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Property;
import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.annotation.Transient;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Memory implementation of the {@link Artifact}.
 */

@NodeEntity
public class NeoArtifact<DataType extends ArtifactData> extends NeoEntity implements Artifact<DataType>, Artifact.Op<DataType> {

	@Property("buffer")
	private byte[] buffer = null;

	@Transient
	private DataType data;

	@Property("atomic")
	private boolean atomic;

	@Property("ordered")
	private boolean ordered;

	// backref
	@Relationship("hasPOGAf")
	private PartialOrderGraph.Op partialOrderGraph;

	@Property("sequenceNumber")
	private int sequenceNumber;

	@Property("useReferencesInEquals")
	private boolean useReferencesInEquals;



	public NeoArtifact() {}

	public NeoArtifact(DataType data) {
		this(data, false);
	}

	public NeoArtifact(DataType data, boolean ordered) {
		checkNotNull(data);
		this.setData(data);
		this.ordered = ordered;
		this.sequenceNumber = PartialOrderGraph.UNASSIGNED_SEQUENCE_NUMBER;
		this.useReferencesInEquals = false;
	}

	@Override
	@SuppressWarnings("unchecked")
	public DataType getData() {
		if (this.data == null) {
			if (this.buffer == null)
				return null;
			else {
				try (ByteArrayInputStream bis = new ByteArrayInputStream(this.buffer)) {
					try (ObjectInput in = new ObjectInputStream(bis)) {
						this.data = (DataType) in.readObject();
					}
				} catch (IOException | ClassNotFoundException e) {
					e.printStackTrace();
				}
			}
		}

		return this.data;
	}

	public void setData(DataType data) {
		this.data = data;

		try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
			try (ObjectOutput out = new ObjectOutputStream(bos)) {
				out.writeObject(this.data);
				this.buffer = bos.toByteArray();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public int hashCode() {
		int result = this.getData().hashCode();
		result = 31 * result + (this.ordered ? 1 : 0);
		return result;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		NeoArtifact<?> that = (NeoArtifact<?>) o;

		if (this.isOrdered() != that.isOrdered()) return false;
		if (this.getSequenceNumber() != PartialOrderGraph.UNASSIGNED_SEQUENCE_NUMBER && that.getSequenceNumber() != PartialOrderGraph.UNASSIGNED_SEQUENCE_NUMBER && this.getSequenceNumber() != that.getSequenceNumber())
			return false;

		if (!this.useReferencesInEquals())
			return getData().equals(that.getData());
		else {
			if (!this.getData().equals(that.getData()))
				return false;
			if (this.getUses().size() != that.getUses().size())
				return false;
			for (ArtifactReference ar : this.getUses()) {
				boolean found = false;
				for (ArtifactReference thatAR : that.getUses()) {
					if (ar.getTarget().equals(thatAR.getTarget())) {
						found = true;
						break;
					}
				}
				if (!found)
					return false;
				// this causes an endless recursion:
				//if (!that.getUses().contains(ar))
				//	return false;
			}
			return true;
		}
	}

	@Override
	public String toString() {
		if (this.getData() == null)
			return "NULL";
		return this.getData().toString();
	}

	@Override
	public boolean useReferencesInEquals() {
		return this.useReferencesInEquals;
	}

	@Override
	public void setUseReferencesInEquals(boolean useReferenesInEquals) {
		this.useReferencesInEquals = useReferenesInEquals;
	}

	@Override
	public boolean equalsIgnoreSequenceNumber(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		NeoArtifact<?> that = (NeoArtifact<?>) o;

		if (this.isOrdered() != that.isOrdered()) return false;

		return getData().equals(that.getData());
	}

	@Override
	public boolean isAtomic() {
		return this.atomic;
	}

	@Override
	public void setAtomic(boolean atomic) {
		this.atomic = atomic;
	}

	@Override
	public boolean isOrdered() {
		return this.ordered;
	}

	@Override
	public void setOrdered(boolean ordered) {
		this.ordered = ordered;
	}

	@Override
	public PartialOrderGraph.Op getPartialOrderGraph() {
		return this.partialOrderGraph;
	}

	@Override
	public void setPartialOrderGraph(PartialOrderGraph.Op partialOrderGraph) {
		this.partialOrderGraph = partialOrderGraph;
	}

	@Override
	public int getSequenceNumber() {
		return this.sequenceNumber;
	}

	@Override
	public void setSequenceNumber(int sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}

	@Override
	public boolean isSequenced() {
		return this.partialOrderGraph != null;
	}


	@Override
	public PartialOrderGraph.Op createSequenceGraph() {
		return new NeoPartialOrderGraph();
	}


	// CONTAINING NODE

	private Node.Op containingNode;

	@Override
	public Node.Op getContainingNode() {
		return this.containingNode;
	}

	@Override
	public void setContainingNode(final Node.Op node) {
		this.containingNode = node;
	}


	// REFERENCES

	private final Collection<ArtifactReference.Op> uses = new ArrayList<>();
	private final Collection<ArtifactReference.Op> usedBy = new ArrayList<>();

	@Override
	public Collection<ArtifactReference.Op> getUses() {
		return Collections.unmodifiableCollection(this.uses);
	}

	@Override
	public Collection<ArtifactReference.Op> getUsedBy() {
		return Collections.unmodifiableCollection(this.usedBy);
	}

	@Override
	public void addUses(final ArtifactReference.Op reference) {
		checkNotNull(reference);

		this.uses.add(reference);
	}

	@Override
	public void addUsedBy(final ArtifactReference.Op reference) {
		checkNotNull(reference);

		this.usedBy.add(reference);
	}

	@Override
	public void addUses(Op target) {
		this.addUses(target, "");
	}

	@Override
	public void addUses(Op target, String type) {
		checkNotNull(target);
		checkNotNull(type);

		if (this.uses(target))
			return;

		ArtifactReference.Op artifactReference = new NeoArtifactReference();
		artifactReference.setSource(this);
		artifactReference.setTarget(target);
		this.addUses(artifactReference);
		target.addUsedBy(artifactReference);
	}


	// PROPERTIES

	private transient Map<String, Object> properties = null;

	@Override
	public Map<String, Object> getProperties() {
		if (this.properties == null)
			this.properties = Maps.mutable.empty();
		return this.properties;
	}

}
